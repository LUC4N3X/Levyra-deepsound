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
    private enum class LexemeKind {
        IDENTIFIER,
        NUMBER,
        STRING,
        SYMBOL
    }

    private data class Lexeme(
        val kind: LexemeKind,
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
        scanSignatureWindows(javascript).forEach { window ->
            val lexemes = tokenize(javascript, window.start, window.end)
            val decode = lexemes.indexOfFirst { lexeme ->
                lexeme.kind == LexemeKind.IDENTIFIER &&
                    lexeme.text == "decodeURIComponent" &&
                    window.anchor in lexeme.start until lexeme.end
            }
            if (decode < 0 || lexemes.getOrNull(decode + 1)?.text != "(") return@forEach
            val decodeClose = matchingParen(lexemes, decode + 1) ?: return@forEach
            if (!mentionsKey(lexemes, decode + 2, decodeClose - 1, "s")) return@forEach
            val sourceEvidence = 30

            enclosingCalls(lexemes, decode, decodeClose).forEach { call ->
                if (isNonTransformTarget(call.target)) return@forEach
                val expression = expressionForRange(lexemes, call, decode, decodeClose) ?: return@forEach
                val outputVariable = assignedVariableForExpression(lexemes, call.targetStart)
                val sinkEvidence = when {
                    outputVariable != null && isWholeAssignedExpression(lexemes, call) ->
                        signatureSinkScore(lexemes, call.close + 1, outputVariable)
                    isNestedInCallable(lexemes, call.targetStart, call.close, "encodeURIComponent") -> 35
                    else -> 0
                }
                addIfConfident(output, expression, 70 + sourceEvidence + sinkEvidence)
            }

            val sourceVariable = assignedVariableForExpression(lexemes, decode)
            if (sourceVariable != null) {
                output += followVariableFlow(
                    lexemes = lexemes,
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
        scanNWindows(javascript).forEach { window ->
            val lexemes = tokenize(javascript, window.start, window.end)
            val keyIndex = lexemes.indexOfFirst { lexeme ->
                lexeme.kind == LexemeKind.STRING &&
                    lexeme.text == "n" &&
                    window.anchor in lexeme.start until lexeme.end
            }
            if (!isGetCallKey(lexemes, keyIndex)) return@forEach
            val getOpen = keyIndex - 1
            val getClose = matchingParen(lexemes, getOpen) ?: return@forEach
            val getCall = callSite(lexemes, getOpen, getClose)
                ?.takeIf { it.target.endsWith(".get") }
                ?: return@forEach
            val getStart = getCall.targetStart

            enclosingCalls(lexemes, getStart, getClose).forEach { call ->
                if (isNonTransformTarget(call.target)) return@forEach
                val expression = expressionForRange(lexemes, call, getStart, getClose) ?: return@forEach
                val outputVariable = assignedVariableForExpression(lexemes, call.targetStart)
                val sinkEvidence = when {
                    outputVariable != null && isWholeAssignedExpression(lexemes, call) ->
                        nSinkScore(lexemes, call.close + 1, outputVariable)
                    isNestedInNSet(lexemes, call.targetStart, call.close) -> 45
                    else -> 0
                }
                addIfConfident(output, expression, 70 + sinkEvidence)
            }

            val sourceVariable = assignedVariableForExpression(lexemes, getStart)
            if (sourceVariable != null) {
                output += followVariableFlow(
                    lexemes = lexemes,
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
        lexemes: List<Lexeme>,
        startIndex: Int,
        sourceVariable: String,
        baseConfidence: Int,
        sinkScore: (List<Lexeme>, Int, String) -> Int
    ): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        val tainted = LinkedHashSet<String>()
        tainted += sourceVariable
        var index = startIndex.coerceAtLeast(0)
        val limit = minOf(lexemes.size, index + MAX_FLOW_TOKENS)

        while (index < limit) {
            if (lexemes[index].text != "=" || !isAssignmentOperator(lexemes, index)) {
                index++
                continue
            }
            val lhs = lexemes.getOrNull(index - 1)
                ?.takeIf { it.kind == LexemeKind.IDENTIFIER }
                ?.text
            if (lhs == null) {
                index++
                continue
            }
            val expressionStart = index + 1
            val expressionEnd = statementEnd(lexemes, expressionStart, limit)
            if (isPureAlias(lexemes, expressionStart, expressionEnd, tainted)) {
                tainted += lhs
                index = expressionEnd + 1
                continue
            }

            val sourceTaint = LinkedHashSet(tainted)
            tainted.remove(lhs)
            var derivesFromTaint = false
            callsBetween(lexemes, expressionStart, expressionEnd).forEach { call ->
                if (!coversExpression(call, expressionStart, expressionEnd)) return@forEach
                val expression = expressionForIdentifiers(lexemes, call, sourceTaint) ?: return@forEach
                val confidence = baseConfidence + sinkScore(lexemes, expressionEnd + 1, lhs)
                addIfConfident(output, expression, confidence)
                derivesFromTaint = true
            }
            if (derivesFromTaint) tainted += lhs
            index = expressionEnd + 1
        }
        return output
    }

    private fun isPureAlias(
        lexemes: List<Lexeme>,
        start: Int,
        endExclusive: Int,
        tainted: Set<String>
    ): Boolean {
        if (endExclusive - start != 1) return false
        val lexeme = lexemes.getOrNull(start) ?: return false
        return lexeme.kind == LexemeKind.IDENTIFIER && lexeme.text in tainted
    }

    private fun coversExpression(call: CallSite, start: Int, endExclusive: Int): Boolean =
        call.targetStart == start && call.close == endExclusive - 1

    private fun expressionForRange(
        lexemes: List<Lexeme>,
        call: CallSite,
        sourceStart: Int,
        sourceEnd: Int
    ): String? {
        var replaced = 0
        val arguments = ArrayList<String>(call.arguments.size)
        call.arguments.forEach { range ->
            if (range.first == sourceStart && range.last == sourceEnd) {
                arguments += "INPUT"
                replaced++
            } else {
                arguments += safeConstantArgument(lexemes, range) ?: return null
            }
        }
        if (replaced != 1) return null
        return "${call.target}(${arguments.joinToString(",")})"
    }

    private fun expressionForIdentifiers(
        lexemes: List<Lexeme>,
        call: CallSite,
        tainted: Set<String>
    ): String? {
        var replaced = 0
        val arguments = ArrayList<String>(call.arguments.size)
        call.arguments.forEach { range ->
            val sole = lexemes.getOrNull(range.first)
                ?.takeIf { range.first == range.last }
            if (sole?.kind == LexemeKind.IDENTIFIER && sole.text in tainted) {
                arguments += "INPUT"
                replaced++
            } else {
                arguments += safeConstantArgument(lexemes, range) ?: return null
            }
        }
        if (replaced != 1) return null
        return "${call.target}(${arguments.joinToString(",")})"
    }

    private fun safeConstantArgument(lexemes: List<Lexeme>, range: IntRange): String? {
        if (range.isEmpty()) return null
        val slice = range.map { lexemes[it] }
        return when {
            slice.size == 1 && isSafeInteger(slice[0]) -> slice[0].text
            slice.size == 2 && slice[0].text == "-" && isSafeInteger(slice[1]) -> "-${slice[1].text}"
            else -> null
        }
    }

    private fun isSafeInteger(lexeme: Lexeme): Boolean =
        lexeme.kind == LexemeKind.NUMBER && lexeme.text.isNotEmpty() && lexeme.text.all(Char::isDigit)

    private fun enclosingCalls(
        lexemes: List<Lexeme>,
        sourceStart: Int,
        sourceEnd: Int
    ): List<CallSite> {
        val output = ArrayList<CallSite>(MAX_ENCLOSING_CALLS)
        var index = sourceStart - 1
        var inspected = 0
        while (index >= 0 && inspected < MAX_PARENT_SCAN_TOKENS && output.size < MAX_ENCLOSING_CALLS) {
            if (lexemes[index].text == "(") {
                val close = matchingParen(lexemes, index)
                if (close != null && close >= sourceEnd) {
                    callSite(lexemes, index, close)?.let(output::add)
                }
            }
            index--
            inspected++
        }
        return output
    }

    private fun callsBetween(lexemes: List<Lexeme>, start: Int, endExclusive: Int): List<CallSite> {
        val output = ArrayList<CallSite>()
        var index = start.coerceAtLeast(0)
        val end = minOf(lexemes.size, endExclusive)
        while (index < end && output.size < MAX_CALLS_PER_FLOW) {
            if (lexemes[index].text == "(") {
                val close = matchingParen(lexemes, index)
                if (close != null && close < end) {
                    callSite(lexemes, index, close)?.let { call ->
                        if (!isNonTransformTarget(call.target)) output += call
                    }
                }
            }
            index++
        }
        return output
    }

    private fun isNonTransformTarget(target: String): Boolean =
        target in NON_TRANSFORM_CALLS ||
            target.endsWith(".get") ||
            target.endsWith(".set")

    private fun callSite(lexemes: List<Lexeme>, open: Int, close: Int): CallSite? {
        val target = callableTarget(lexemes, open) ?: return null
        return CallSite(
            target = target.first,
            targetStart = target.second,
            open = open,
            close = close,
            arguments = splitArguments(lexemes, open + 1, close)
        )
    }

    private fun callableTarget(lexemes: List<Lexeme>, open: Int): Pair<String, Int>? {
        var index = open - 1
        if (index < 0) return null
        var target: String
        var start: Int

        if (lexemes[index].text == "]") {
            if (
                index < 3 ||
                !isSafeInteger(lexemes[index - 1]) ||
                lexemes[index - 2].text != "[" ||
                lexemes[index - 3].kind != LexemeKind.IDENTIFIER
            ) return null
            target = "${lexemes[index - 3].text}[${lexemes[index - 1].text}]"
            start = index - 3
            index -= 4
        } else {
            val last = lexemes[index].takeIf { it.kind == LexemeKind.IDENTIFIER } ?: return null
            target = last.text
            start = index
            index--
        }

        while (
            index >= 1 &&
            lexemes[index].text == "." &&
            lexemes[index - 1].kind == LexemeKind.IDENTIFIER
        ) {
            target = "${lexemes[index - 1].text}.$target"
            start = index - 1
            index -= 2
        }
        if (target.length > MAX_CALLABLE_LENGTH || target.split('.').any { it.isBlank() }) return null
        return target to start
    }

    private fun splitArguments(lexemes: List<Lexeme>, start: Int, close: Int): List<IntRange> {
        if (start >= close) return emptyList()
        val output = ArrayList<IntRange>()
        var argStart = start
        var paren = 0
        var bracket = 0
        var brace = 0
        var index = start
        while (index < close) {
            when (lexemes[index].text) {
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

    private fun assignmentIndexForExpression(lexemes: List<Lexeme>, expressionStart: Int): Int? {
        var index = expressionStart - 1
        var inspected = 0
        while (index >= 1 && inspected < MAX_ASSIGNMENT_SCAN_TOKENS) {
            val symbol = lexemes[index].text
            if (symbol == ";" || symbol == "," || symbol == "{" || symbol == "}") return null
            if (symbol == "=" && isAssignmentOperator(lexemes, index)) return index
            index--
            inspected++
        }
        return null
    }

    private fun assignedVariableForExpression(lexemes: List<Lexeme>, expressionStart: Int): String? {
        val assignment = assignmentIndexForExpression(lexemes, expressionStart) ?: return null
        return lexemes.getOrNull(assignment - 1)
            ?.takeIf { it.kind == LexemeKind.IDENTIFIER }
            ?.text
    }

    private fun isWholeAssignedExpression(lexemes: List<Lexeme>, call: CallSite): Boolean {
        val assignment = assignmentIndexForExpression(lexemes, call.targetStart) ?: return false
        val end = statementEnd(
            lexemes,
            assignment + 1,
            minOf(lexemes.size, assignment + 1 + MAX_FLOW_TOKENS)
        )
        return call.targetStart == assignment + 1 && call.close == end - 1
    }

    private fun isAssignmentOperator(lexemes: List<Lexeme>, index: Int): Boolean {
        return lexemes.getOrNull(index - 1)?.text != "=" &&
            lexemes.getOrNull(index + 1)?.text != "=" &&
            lexemes.getOrNull(index + 1)?.text != ">" &&
            lexemes.getOrNull(index - 1)?.text != "!" &&
            lexemes.getOrNull(index - 1)?.text != ">" &&
            lexemes.getOrNull(index - 1)?.text != "<"
    }

    private fun isAssignmentTo(lexemes: List<Lexeme>, index: Int, variable: String): Boolean {
        if (lexemes.getOrNull(index)?.kind != LexemeKind.IDENTIFIER || lexemes[index].text != variable) return false
        val assignment = index + 1
        return lexemes.getOrNull(assignment)?.text == "=" && isAssignmentOperator(lexemes, assignment)
    }

    private fun singleIdentifier(lexemes: List<Lexeme>, range: IntRange, identifier: String): Boolean {
        if (range.first != range.last) return false
        val lexeme = lexemes.getOrNull(range.first) ?: return false
        return lexeme.kind == LexemeKind.IDENTIFIER && lexeme.text == identifier
    }

    private fun signatureSinkScore(lexemes: List<Lexeme>, start: Int, variable: String): Int {
        var score = 0
        val end = minOf(lexemes.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (isAssignmentTo(lexemes, index, variable)) return score
            if (lexemes[index].text == "encodeURIComponent" && lexemes.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(lexemes, index + 1)
                if (close != null && close < end) {
                    val args = splitArguments(lexemes, index + 2, close)
                    if (args.size == 1 && singleIdentifier(lexemes, args[0], variable)) {
                        score = maxOf(score, 25)
                    }
                }
            }
            if (lexemes[index].text == "set" && lexemes.getOrNull(index - 1)?.text == "." && lexemes.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(lexemes, index + 1)
                if (close != null && close < end) {
                    val args = splitArguments(lexemes, index + 2, close)
                    if (args.size >= 2 && singleIdentifier(lexemes, args[1], variable)) {
                        score = maxOf(score, 40)
                    }
                }
            }
            index++
        }
        return score
    }

    private fun nSinkScore(lexemes: List<Lexeme>, start: Int, variable: String): Int {
        val end = minOf(lexemes.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (isAssignmentTo(lexemes, index, variable)) return 0
            if (lexemes[index].text == "set" && lexemes.getOrNull(index - 1)?.text == "." && lexemes.getOrNull(index + 1)?.text == "(") {
                val close = matchingParen(lexemes, index + 1)
                if (close != null && close < end) {
                    val args = splitArguments(lexemes, index + 2, close)
                    if (
                        args.size >= 2 &&
                        singleString(lexemes, args[0]) == "n" &&
                        singleIdentifier(lexemes, args[1], variable)
                    ) return 45
                }
            }
            index++
        }
        return 0
    }

    private fun isNestedInCallable(
        lexemes: List<Lexeme>,
        sourceStart: Int,
        sourceEnd: Int,
        callable: String
    ): Boolean {
        return enclosingCalls(lexemes, sourceStart, sourceEnd).any { call ->
            call.target == callable &&
                call.arguments.size == 1 &&
                call.arguments[0].first == sourceStart &&
                call.arguments[0].last == sourceEnd
        }
    }

    private fun isNestedInNSet(lexemes: List<Lexeme>, sourceStart: Int, sourceEnd: Int): Boolean {
        return enclosingCalls(lexemes, sourceStart, sourceEnd).any { call ->
            call.target.endsWith(".set") &&
                call.arguments.size >= 2 &&
                singleString(lexemes, call.arguments[0]) == "n" &&
                call.arguments[1].first == sourceStart &&
                call.arguments[1].last == sourceEnd
        }
    }

    private fun isGetCallKey(lexemes: List<Lexeme>, keyIndex: Int): Boolean {
        if (keyIndex < 3) return false
        return lexemes[keyIndex - 1].text == "(" &&
            lexemes[keyIndex - 2].text == "get" &&
            lexemes[keyIndex - 3].text == "."
    }

    private fun mentionsKey(lexemes: List<Lexeme>, start: Int, endInclusive: Int, key: String): Boolean {
        if (start > endInclusive) return false
        for (index in start..minOf(endInclusive, lexemes.lastIndex)) {
            val lexeme = lexemes[index]
            if (
                lexeme.kind == LexemeKind.STRING &&
                lexeme.text == key &&
                lexemes.getOrNull(index - 1)?.text == "[" &&
                lexemes.getOrNull(index + 1)?.text == "]"
            ) return true
            if (
                lexeme.kind == LexemeKind.IDENTIFIER &&
                lexeme.text == key &&
                lexemes.getOrNull(index - 1)?.text == "."
            ) return true
        }
        return false
    }

    private fun singleString(lexemes: List<Lexeme>, range: IntRange): String? {
        if (range.first != range.last) return null
        return lexemes[range.first].takeIf { it.kind == LexemeKind.STRING }?.text
    }

    private fun statementEnd(lexemes: List<Lexeme>, start: Int, limit: Int): Int {
        var paren = 0
        var bracket = 0
        var brace = 0
        var index = start
        while (index < limit) {
            when (lexemes[index].text) {
                "(" -> paren++
                ")" -> if (paren > 0) paren--
                "[" -> bracket++
                "]" -> if (bracket > 0) bracket--
                "{" -> brace++
                "}" -> if (brace > 0) brace--
                ";", "," -> if (paren == 0 && bracket == 0 && brace == 0) return index
            }
            index++
        }
        return limit
    }

    private fun matchingParen(lexemes: List<Lexeme>, open: Int): Int? {
        if (lexemes.getOrNull(open)?.text != "(") return null
        var depth = 0
        for (index in open until lexemes.size) {
            when (lexemes[index].text) {
                "(" -> depth++
                ")" -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun scanSignatureWindows(javascript: String): List<ScanWindow> {
        val output = ArrayList<ScanWindow>()
        val seenAnchors = HashSet<Int>()
        var cursor = 0
        while (cursor < javascript.length && output.size < MAX_ANCHORS_PER_KIND) {
            val anchor = javascript.indexOf("decodeURIComponent", cursor)
            if (anchor < 0) break
            if (isSignatureDecodeAnchor(javascript, anchor) && seenAnchors.add(anchor)) {
                val start = (anchor - SIGNATURE_BEFORE_CHARS).coerceAtLeast(0)
                val end = (anchor + "decodeURIComponent".length + SIGNATURE_AFTER_CHARS)
                    .coerceAtMost(javascript.length)
                output += ScanWindow(start, end, anchor)
            }
            cursor = anchor + "decodeURIComponent".length
        }
        return output
    }

    private fun isSignatureDecodeAnchor(source: String, anchor: Int): Boolean {
        val start = anchor.coerceAtLeast(0)
        val end = (anchor + SIGNATURE_SOURCE_PROBE_CHARS).coerceAtMost(source.length)
        val lexemes = tokenize(source, start, end)
        val decode = lexemes.indexOfFirst { lexeme ->
            lexeme.kind == LexemeKind.IDENTIFIER &&
                lexeme.text == "decodeURIComponent" &&
                anchor in lexeme.start until lexeme.end
        }
        if (decode < 0 || lexemes.getOrNull(decode + 1)?.text != "(") return false
        val close = matchingParen(lexemes, decode + 1) ?: return false
        return mentionsKey(lexemes, decode + 2, close - 1, "s")
    }

    private fun scanNWindows(javascript: String): List<ScanWindow> {
        val output = ArrayList<ScanWindow>()
        val seenAnchors = HashSet<Int>()
        listOf("\"n\"", "'n'").forEach { needle ->
            var cursor = 0
            var foundForNeedle = 0
            while (
                cursor < javascript.length &&
                foundForNeedle < MAX_ANCHORS_PER_NEEDLE &&
                output.size < MAX_ANCHORS_PER_KIND
            ) {
                val anchor = javascript.indexOf(needle, cursor)
                if (anchor < 0) break
                if (isNGetAnchor(javascript, anchor)) {
                    val start = (anchor - N_BEFORE_CHARS).coerceAtLeast(0)
                    val end = (anchor + needle.length + N_AFTER_CHARS).coerceAtMost(javascript.length)
                    if (seenAnchors.add(anchor)) output += ScanWindow(start, end, anchor)
                    foundForNeedle++
                }
                cursor = anchor + needle.length
            }
        }
        return output.sortedBy { it.anchor }
    }

    private fun isNGetAnchor(source: String, anchor: Int): Boolean {
        val start = (anchor - N_SOURCE_PROBE_BEFORE_CHARS).coerceAtLeast(0)
        val end = (anchor + N_SOURCE_PROBE_AFTER_CHARS).coerceAtMost(source.length)
        val lexemes = tokenize(source, start, end)
        val keyIndex = lexemes.indexOfFirst { lexeme ->
            lexeme.kind == LexemeKind.STRING &&
                lexeme.text == "n" &&
                anchor in lexeme.start until lexeme.end
        }
        return isGetCallKey(lexemes, keyIndex)
    }

    private fun tokenize(source: String, start: Int, endExclusive: Int): List<Lexeme> {
        val output = ArrayList<Lexeme>()
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
                    val lexemeStart = index
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
                    output += Lexeme(LexemeKind.STRING, value.toString(), lexemeStart, index)
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
                    val lexemeStart = index
                    index++
                    while (index < endExclusive && isIdentifierPart(source[index])) index++
                    output += Lexeme(LexemeKind.IDENTIFIER, source.substring(lexemeStart, index), lexemeStart, index)
                }
                char.isDigit() -> {
                    val lexemeStart = index
                    index++
                    while (index < endExclusive && (source[index].isDigit() || source[index] == '.')) index++
                    output += Lexeme(LexemeKind.NUMBER, source.substring(lexemeStart, index), lexemeStart, index)
                }
                else -> {
                    output += Lexeme(LexemeKind.SYMBOL, char.toString(), index, index + 1)
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
            .mapIndexed { index, candidate ->
                if (index == 0) candidate else candidate.copy(confidence = SECONDARY_RESERVE_CONFIDENCE)
            }
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

    private const val MIN_CONFIDENCE = 110
    private const val SECONDARY_RESERVE_CONFIDENCE = 0
    private const val MAX_ANCHORS_PER_NEEDLE = 32
    private const val MAX_ANCHORS_PER_KIND = 64
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
    private const val SIGNATURE_SOURCE_PROBE_CHARS = 320
    private const val N_BEFORE_CHARS = 1_800
    private const val N_AFTER_CHARS = 2_600
    private const val N_SOURCE_PROBE_BEFORE_CHARS = 64
    private const val N_SOURCE_PROBE_AFTER_CHARS = 32
}
