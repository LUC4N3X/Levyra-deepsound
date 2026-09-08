package com.luc4n3x.levyra.data

internal data class YoutubeSemanticTransformCandidate(
    val expression: String,
    val confidence: Int
)

internal data class YoutubeSemanticDiscoveryResult(
    val signatures: List<YoutubeSemanticTransformCandidate>,
    val nTransforms: List<YoutubeSemanticTransformCandidate>
)

internal object YoutubePlayerSemanticAnalyzerV2 {
    private enum class TokenKind {
        IDENTIFIER,
        NUMBER,
        STRING,
        SYMBOL
    }

    private data class Token(
        val kind: TokenKind,
        val text: String,
        val start: Int,
        val end: Int
    )

    private data class ScanWindow(
        val start: Int,
        val end: Int,
        val anchor: Int
    )

    private data class CallSite(
        val target: String,
        val targetStart: Int,
        val open: Int,
        val close: Int,
        val arguments: List<IntRange>
    )

    fun discover(javascript: String): YoutubeSemanticDiscoveryResult {
        if (javascript.isBlank()) {
            return YoutubeSemanticDiscoveryResult(emptyList(), emptyList())
        }
        return YoutubeSemanticDiscoveryResult(
            signatures = rank(discoverSignatures(javascript)),
            nTransforms = rank(discoverNTransforms(javascript))
        )
    }

    private fun discoverSignatures(javascript: String): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        scanWindows(javascript, listOf("decodeURIComponent"), SIGNATURE_BEFORE_CHARS, SIGNATURE_AFTER_CHARS)
            .forEach { window ->
                val tokens = tokenize(javascript, window.start, window.end)
                val decode = tokens.indexOfFirst { token ->
                    token.kind == TokenKind.IDENTIFIER &&
                        token.text == "decodeURIComponent" &&
                        window.anchor in token.start until token.end
                }
                if (decode < 0 || tokens.getOrNull(decode + 1)?.text != "(") return@forEach
                val decodeClose = matchingParen(tokens, decode + 1) ?: return@forEach
                val sourceEvidence = if (mentionsKey(tokens, decode + 2, decodeClose - 1, "s")) 30 else 0

                enclosingCalls(tokens, decode, decodeClose).forEach { call ->
                    val expression = expressionForRange(tokens, call, decode, decodeClose) ?: return@forEach
                    val outputVariable = assignedVariableForExpression(tokens, call.targetStart)
                    val sinkEvidence = when {
                        outputVariable != null -> signatureSinkScore(tokens, call.close + 1, outputVariable)
                        isNestedInCallable(tokens, call.targetStart, call.close, "encodeURIComponent") -> 35
                        else -> 0
                    }
                    addIfConfident(output, expression, 70 + sourceEvidence + sinkEvidence)
                }

                val sourceVariable = assignedVariableForExpression(tokens, decode)
                if (sourceVariable != null) {
                    output += followVariableFlow(
                        tokens = tokens,
                        startIndex = decodeClose + 1,
                        sourceVariable = sourceVariable,
                        baseConfidence = 70 + sourceEvidence,
                        sinkScore = ::signatureSinkScore
                    )
                }
            }
        return output
    }

    private fun discoverNTransforms(javascript: String): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        scanWindows(javascript, listOf("\"n\"", "'n'"), N_BEFORE_CHARS, N_AFTER_CHARS)
            .forEach { window ->
                val tokens = tokenize(javascript, window.start, window.end)
                val keyIndex = tokens.indexOfFirst { token ->
                    token.kind == TokenKind.STRING &&
                        token.text == "n" &&
                        window.anchor in token.start until token.end
                }
                if (!isGetCallKey(tokens, keyIndex)) return@forEach
                val getClose = matchingParen(tokens, keyIndex - 1) ?: return@forEach
                val getStart = keyIndex - 3

                enclosingCalls(tokens, getStart, getClose).forEach { call ->
                    val expression = expressionForRange(tokens, call, getStart, getClose) ?: return@forEach
                    val outputVariable = assignedVariableForExpression(tokens, call.targetStart)
                    val sinkEvidence = when {
                        outputVariable != null -> nSinkScore(tokens, call.close + 1, outputVariable)
                        isNestedInNSet(tokens, call.targetStart, call.close) -> 45
                        else -> 0
                    }
                    addIfConfident(output, expression, 70 + sinkEvidence)
                }

                val sourceVariable = assignedVariableForExpression(tokens, getStart)
                if (sourceVariable != null) {
                    output += followVariableFlow(
                        tokens = tokens,
                        startIndex = getClose + 1,
                        sourceVariable = sourceVariable,
                        baseConfidence = 70,
                        sinkScore = ::nSinkScore
                    )
                }
            }
        return output
    }

    private fun followVariableFlow(
        tokens: List<Token>,
        startIndex: Int,
        sourceVariable: String,
        baseConfidence: Int,
        sinkScore: (List<Token>, Int, String) -> Int
    ): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        val tainted = LinkedHashSet<String>()
        tainted += sourceVariable
        var index = startIndex.coerceAtLeast(0)
        val limit = minOf(tokens.size, index + MAX_FLOW_TOKENS)

        while (index < limit) {
            if (tokens[index].text != "=" || !isAssignmentOperator(tokens, index)) {
                index++
                continue
            }
            val lhs = tokens.getOrNull(index - 1)
                ?.takeIf { it.kind == TokenKind.IDENTIFIER }
                ?.text
            if (lhs == null) {
                index++
                continue
            }
            val statementEnd = statementEnd(tokens, index + 1, limit)
            val rhs = (index + 1 until statementEnd).toList()
            val identifiers = rhs
                .mapNotNull { tokenIndex -> tokens[tokenIndex].takeIf { it.kind == TokenKind.IDENTIFIER }?.text }
            if (identifiers.size == 1 && identifiers.first() in tainted) {
                tainted += lhs
                index = statementEnd + 1
                continue
            }

            callsBetween(tokens, index + 1, statementEnd).forEach { call ->
                val expression = expressionForIdentifiers(tokens, call, tainted) ?: return@forEach
                val confidence = baseConfidence + sinkScore(tokens, statementEnd + 1, lhs)
                addIfConfident(output, expression, confidence)
                tainted += lhs
            }
            index = statementEnd + 1
        }
        return output
    }

    private fun expressionForRange(
        tokens: List<Token>,
        call: CallSite,
        sourceStart: Int,
        sourceEnd: Int
    ): String? {
        var replaced = 0
        val arguments = ArrayList<String>(call.arguments.size)
        call.arguments.forEach { range ->
            if (range.first <= sourceStart && range.last >= sourceEnd) {
                arguments += "INPUT"
                replaced++
            } else {
                arguments += safeConstantArgument(tokens, range) ?: return null
            }
        }
        if (replaced != 1) return null
        return "${call.target}(${arguments.joinToString(",")})"
    }

    private fun expressionForIdentifiers(
        tokens: List<Token>,
        call: CallSite,
        tainted: Set<String>
    ): String? {
        var replaced = 0
        val arguments = ArrayList<String>(call.arguments.size)
        call.arguments.forEach { range ->
            val names = range.mapNotNull { index ->
                tokens[index].takeIf { it.kind == TokenKind.IDENTIFIER }?.text
            }
            if (names.any { it in tainted }) {
                if (names.count { it in tainted } != 1) return null
                if (range.any { index ->
                        val token = tokens[index]
                        token.kind == TokenKind.IDENTIFIER && token.text !in tainted
                    }
                ) return null
                arguments += "INPUT"
                replaced++
            } else {
                arguments += safeConstantArgument(tokens, range) ?: return null
            }
        }
        if (replaced != 1) return null
        return "${call.target}(${arguments.joinToString(",")})"
    }

    private fun safeConstantArgument(tokens: List<Token>, range: IntRange): String? {
        if (range.isEmpty()) return null
        val slice = range.map { tokens[it] }
        return when {
            slice.size == 1 && slice[0].kind == TokenKind.NUMBER -> slice[0].text
            slice.size == 2 && slice[0].text == "-" && slice[1].kind == TokenKind.NUMBER -> "-${slice[1].text}"
            else -> null
        }
    }

    private fun enclosingCalls(
        tokens: List<Token>,
        sourceStart: Int,
        sourceEnd: Int
    ): List<CallSite> {
        val output = ArrayList<CallSite>(MAX_ENCLOSING_CALLS)
        var index = sourceStart - 1
        var inspected = 0
        while (index >= 0 && inspected < MAX_PARENT_SCAN_TOKENS && output.size < MAX_ENCLOSING_CALLS) {
            if (tokens[index].text == "(") {
                val close = matchingParen(tokens, index)
                if (close != null && close >= sourceEnd) {
                    callSite(tokens, index, close)?.let { call ->
                        if (call.target != "decodeURIComponent" && call.target != "encodeURIComponent") {
                            output += call
                        }
                    }
                }
            }
            index--
            inspected++
        }
        return output
    }

    private fun callsBetween(tokens: List<Token>, start: Int, endExclusive: Int): List<CallSite> {
        val output = ArrayList<CallSite>()
        var index = start.coerceAtLeast(0)
        val end = minOf(tokens.size, endExclusive)
        while (index < end && output.size < MAX_CALLS_PER_FLOW) {
            if (tokens[index].text == "(") {
                val close = matchingParen(tokens, index)
                if (close != null && close < end) {
                    callSite(tokens, index, close)?.let { call ->
                        if (call.target !in NON_TRANSFORM_CALLS) output += call
                    }
                }
            }
            index++
        }
        return output
    }

    private fun callSite(tokens: List<Token>, open: Int, close: Int): CallSite? {
        val target = callableTarget(tokens, open) ?: return null
        return CallSite(
            target = target.first,
            targetStart = target.second,
            open = open,
            close = close,
            arguments = splitArguments(tokens, open + 1, close)
        )
    }

    private fun callableTarget(tokens: List<Token>, open: Int): Pair<String, Int>? {
        var index = open - 1
        if (index < 0) return null
        var target: String
        var start: Int

        if (tokens[index].text == "]") {
            if (
                index < 3 ||
                tokens[index - 1].kind != TokenKind.NUMBER ||
                tokens[index - 2].text != "[" ||
                tokens[index - 3].kind != TokenKind.IDENTIFIER
            ) return null
            target = "${tokens[index - 3].text}[${tokens[index - 1].text}]"
            start = index - 3
            index -= 4
        } else {
            val last = tokens[index].takeIf { it.kind == TokenKind.IDENTIFIER } ?: return null
            target = last.text
            start = index
            index--
        }

        while (
            index >= 1 &&
            tokens[index].text == "." &&
            tokens[index - 1].kind == TokenKind.IDENTIFIER
        ) {
            target = "${tokens[index - 1].text}.$target"
            start = index - 1
            index -= 2
        }
        if (target.length > MAX_CALLABLE_LENGTH || target.split('.').any { it.isBlank() }) return null
        return target to start
    }

    private fun splitArguments(tokens: List<Token>, start: Int, close: Int): List<IntRange> {
        if (start >= close) return emptyList()
        val output = ArrayList<IntRange>()
        var argStart = start
        var paren = 0
        var bracket = 0
        var brace = 0
        var index = start
        while (index < close) {
            when (tokens[index].text) {
                "(" -> paren++
                ")" -> paren--
                "[" -> bracket++
                "]" -> bracket--
                "{" -> brace++
                "}" -> brace--
                "," -> if (paren == 0 && bracket == 0 && brace == 0) {
                    if (argStart >= index) return emptyList()
                    output += argStart..(index - 1)
                    argStart = index + 1
                }
            }
            index++
        }
        if (argStart >= close) return emptyList()
        output += argStart..(close - 1)
        return output
    }

    private fun assignedVariableForExpression(tokens: List<Token>, expressionStart: Int): String? {
        var index = expressionStart - 1
        var inspected = 0
        while (index >= 1 && inspected < MAX_ASSIGNMENT_SCAN_TOKENS) {
            val symbol = tokens[index].text
            if (symbol == ";" || symbol == "{" || symbol == "}") return null
            if (symbol == "=" && isAssignmentOperator(tokens, index)) {
                return tokens[index - 1]
                    .takeIf { it.kind == TokenKind.IDENTIFIER }
                    ?.text
            }
            index--
            inspected++
        }
        return null
    }

    private fun isAssignmentOperator(tokens: List<Token>, index: Int): Boolean {
        return tokens.getOrNull(index - 1)?.text != "=" &&
            tokens.getOrNull(index + 1)?.text != "=" &&
            tokens.getOrNull(index - 1)?.text != "!" &&
            tokens.getOrNull(index - 1)?.text != ">" &&
            tokens.getOrNull(index - 1)?.text != "<"
    }

    private fun signatureSinkScore(tokens: List<Token>, start: Int, variable: String): Int {
        var score = 0
        val end = minOf(tokens.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (tokens[index].text == "encodeURIComponent" && tokens.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(tokens, index + 1)
                if (close != null && close < end && containsIdentifier(tokens, index + 2, close, variable)) {
                    score = maxOf(score, 25)
                }
            }
            if (tokens[index].text == "set" && tokens.getOrNull(index - 1)?.text == "." && tokens.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(tokens, index + 1)
                if (close != null && close < end && containsIdentifier(tokens, index + 2, close, variable)) {
                    score = maxOf(score, 40)
                }
            }
            index++
        }
        return score
    }

    private fun nSinkScore(tokens: List<Token>, start: Int, variable: String): Int {
        val end = minOf(tokens.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (tokens[index].text == "set" && tokens.getOrNull(index - 1)?.text == "." && tokens.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(tokens, index + 1)
                if (close != null && close < end) {
                    val args = splitArguments(tokens, index + 2, close)
                    if (
                        args.size >= 2 &&
                        singleString(tokens, args[0]) == "n" &&
                        containsIdentifier(tokens, args[1].first, args[1].last + 1, variable)
                    ) return 45
                }
            }
            index++
        }
        return 0
    }

    private fun isNestedInCallable(
        tokens: List<Token>,
        sourceStart: Int,
        sourceEnd: Int,
        callable: String
    ): Boolean {
        return enclosingCalls(tokens, sourceStart, sourceEnd).any { it.target == callable }
    }

    private fun isNestedInNSet(tokens: List<Token>, sourceStart: Int, sourceEnd: Int): Boolean {
        return enclosingCalls(tokens, sourceStart, sourceEnd).any { call ->
            call.target.endsWith(".set") &&
                call.arguments.firstOrNull()?.let { singleString(tokens, it) == "n" } == true
        }
    }

    private fun isGetCallKey(tokens: List<Token>, keyIndex: Int): Boolean {
        if (keyIndex < 3) return false
        return tokens[keyIndex - 1].text == "(" &&
            tokens[keyIndex - 2].text == "get" &&
            tokens[keyIndex - 3].text == "."
    }

    private fun mentionsKey(tokens: List<Token>, start: Int, endInclusive: Int, key: String): Boolean {
        if (start > endInclusive) return false
        for (index in start..minOf(endInclusive, tokens.lastIndex)) {
            val token = tokens[index]
            if (token.kind == TokenKind.STRING && token.text == key) return true
            if (
                token.kind == TokenKind.IDENTIFIER &&
                token.text == key &&
                tokens.getOrNull(index - 1)?.text == "."
            ) return true
        }
        return false
    }

    private fun singleString(tokens: List<Token>, range: IntRange): String? {
        if (range.first != range.last) return null
        return tokens[range.first].takeIf { it.kind == TokenKind.STRING }?.text
    }

    private fun containsIdentifier(
        tokens: List<Token>,
        start: Int,
        endExclusive: Int,
        identifier: String
    ): Boolean {
        for (index in start.coerceAtLeast(0) until minOf(endExclusive, tokens.size)) {
            if (tokens[index].kind == TokenKind.IDENTIFIER && tokens[index].text == identifier) return true
        }
        return false
    }

    private fun statementEnd(tokens: List<Token>, start: Int, limit: Int): Int {
        var paren = 0
        var bracket = 0
        var brace = 0
        var index = start
        while (index < limit) {
            when (tokens[index].text) {
                "(" -> paren++
                ")" -> if (paren > 0) paren--
                "[" -> bracket++
                "]" -> if (bracket > 0) bracket--
                "{" -> brace++
                "}" -> if (brace > 0) brace--
                ";" -> if (paren == 0 && bracket == 0 && brace == 0) return index
            }
            index++
        }
        return limit
    }

    private fun matchingParen(tokens: List<Token>, open: Int): Int? {
        if (tokens.getOrNull(open)?.text != "(") return null
        var depth = 0
        for (index in open until tokens.size) {
            when (tokens[index].text) {
                "(" -> depth++
                ")" -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun scanWindows(
        javascript: String,
        needles: List<String>,
        before: Int,
        after: Int
    ): List<ScanWindow> {
        val output = ArrayList<ScanWindow>()
        val seen = HashSet<Long>()
        needles.forEach { needle ->
            var cursor = 0
            while (cursor < javascript.length && output.size < MAX_ANCHORS_PER_KIND) {
                val anchor = javascript.indexOf(needle, cursor)
                if (anchor < 0) break
                val start = (anchor - before).coerceAtLeast(0)
                val end = (anchor + needle.length + after).coerceAtMost(javascript.length)
                val identity = (start.toLong() shl 32) xor end.toLong()
                if (seen.add(identity)) output += ScanWindow(start, end, anchor)
                cursor = anchor + needle.length
            }
        }
        return output.sortedBy { it.anchor }
    }

    private fun tokenize(source: String, start: Int, endExclusive: Int): List<Token> {
        val output = ArrayList<Token>()
        var index = start
        while (index < endExclusive && output.size < MAX_WINDOW_TOKENS) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++
                char == '/' && source.getOrNull(index + 1) == '/' -> {
                    index += 2
                    while (index < endExclusive && source[index] != '\n') index++
                }
                char == '/' && source.getOrNull(index + 1) == '*' -> {
                    index += 2
                    while (
                        index + 1 < endExclusive &&
                        !(source[index] == '*' && source[index + 1] == '/')
                    ) index++
                    index = minOf(endExclusive, index + 2)
                }
                char == '\'' || char == '"' -> {
                    val quote = char
                    val tokenStart = index
                    index++
                    val value = StringBuilder()
                    while (index < endExclusive) {
                        val current = source[index]
                        if (current == '\\' && index + 1 < endExclusive) {
                            value.append(source[index + 1])
                            index += 2
                            continue
                        }
                        if (current == quote) {
                            index++
                            break
                        }
                        value.append(current)
                        index++
                    }
                    output += Token(TokenKind.STRING, value.toString(), tokenStart, index)
                }
                char == '`' -> {
                    index++
                    while (index < endExclusive) {
                        if (source[index] == '\\' && index + 1 < endExclusive) {
                            index += 2
                            continue
                        }
                        if (source[index] == '`') {
                            index++
                            break
                        }
                        index++
                    }
                }
                isIdentifierStart(char) -> {
                    val tokenStart = index
                    index++
                    while (index < endExclusive && isIdentifierPart(source[index])) index++
                    output += Token(TokenKind.IDENTIFIER, source.substring(tokenStart, index), tokenStart, index)
                }
                char.isDigit() -> {
                    val tokenStart = index
                    index++
                    while (index < endExclusive && (source[index].isDigit() || source[index] == '.')) index++
                    output += Token(TokenKind.NUMBER, source.substring(tokenStart, index), tokenStart, index)
                }
                else -> {
                    output += Token(TokenKind.SYMBOL, char.toString(), index, index + 1)
                    index++
                }
            }
        }
        return output
    }

    private fun rank(values: List<YoutubeSemanticTransformCandidate>): List<YoutubeSemanticTransformCandidate> {
        val best = LinkedHashMap<String, YoutubeSemanticTransformCandidate>()
        values.forEach { candidate ->
            val current = best[candidate.expression]
            if (current == null || candidate.confidence > current.confidence) {
                best[candidate.expression] = candidate
            }
        }
        return best.values
            .sortedWith(
                compareByDescending<YoutubeSemanticTransformCandidate> { it.confidence }
                    .thenBy { it.expression }
            )
            .take(MAX_CANDIDATES_PER_KIND)
    }

    private fun addIfConfident(
        output: MutableList<YoutubeSemanticTransformCandidate>,
        expression: String,
        confidence: Int
    ) {
        if (confidence >= MIN_CONFIDENCE) {
            output += YoutubeSemanticTransformCandidate(expression, confidence.coerceAtMost(200))
        }
    }

    private fun isIdentifierStart(char: Char): Boolean =
        char == '_' || char == '$' || char in 'a'..'z' || char in 'A'..'Z'

    private fun isIdentifierPart(char: Char): Boolean = isIdentifierStart(char) || char.isDigit()

    private val NON_TRANSFORM_CALLS = setOf(
        "decodeURIComponent",
        "encodeURIComponent",
        "get",
        "set"
    )

    private const val MIN_CONFIDENCE = 100
    private const val MAX_ANCHORS_PER_KIND = 24
    private const val MAX_WINDOW_TOKENS = 900
    private const val MAX_FLOW_TOKENS = 260
    private const val MAX_SINK_SCAN_TOKENS = 180
    private const val MAX_PARENT_SCAN_TOKENS = 80
    private const val MAX_ASSIGNMENT_SCAN_TOKENS = 32
    private const val MAX_CALLS_PER_FLOW = 16
    private const val MAX_ENCLOSING_CALLS = 4
    private const val MAX_CANDIDATES_PER_KIND = 4
    private const val MAX_CALLABLE_LENGTH = 64
    private const val SIGNATURE_BEFORE_CHARS = 1_200
    private const val SIGNATURE_AFTER_CHARS = 2_600
    private const val N_BEFORE_CHARS = 1_800
    private const val N_AFTER_CHARS = 2_600
}
