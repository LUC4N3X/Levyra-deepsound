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

    private data class AnchorIndex(
        val signatureAnchors: List<Int>,
        val nAnchors: List<Int>
    )

    fun discover(javascript: String): YoutubeSemanticDiscoveryResult {
        if (javascript.isBlank()) {
            return YoutubeSemanticDiscoveryResult(emptyList(), emptyList())
        }

        val anchors = scanAnchors(javascript)
        val signatureBudget = if (LEGACY_SIGNATURE_PRESENCE.containsMatchIn(javascript)) {
            0
        } else {
            MAX_SEMANTIC_CANDIDATES_PER_KIND
        }
        val nBudget = if (LEGACY_N_PRESENCE.containsMatchIn(javascript)) {
            0
        } else {
            MAX_SEMANTIC_CANDIDATES_PER_KIND
        }

        return YoutubeSemanticDiscoveryResult(
            signatures = rank(
                discoverSignatures(javascript, anchors.signatureAnchors),
                signatureBudget
            ),
            nTransforms = rank(
                discoverNTransforms(javascript, anchors.nAnchors),
                nBudget
            )
        )
    }

    private fun discoverSignatures(
        javascript: String,
        anchors: List<Int>
    ): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        signatureWindows(javascript, anchors).forEach { window ->
            val lexemes = tokenize(javascript, window.start, window.end)
            val decode = lexemes.indexOfFirst { lexeme ->
                lexeme.kind == LexemeKind.IDENTIFIER &&
                    lexeme.text == "decodeURIComponent" &&
                    window.anchor in lexeme.start until lexeme.end
            }
            if (decode < 0 || lexemes.getOrNull(decode + 1)?.text != "(") return@forEach
            val decodeClose = matchingParen(lexemes, decode + 1) ?: return@forEach
            if (signatureSourceRange(lexemes, decode, decodeClose) == null) return@forEach

            enclosingCalls(lexemes, decode, decodeClose).forEach callLoop@{ call ->
                if (isNonTransformTarget(call.target)) return@callLoop
                val expression = expressionForRange(lexemes, call, decode, decodeClose) ?: return@callLoop
                val outputVariable = assignedVariableForWholeRange(
                    lexemes,
                    call.targetStart,
                    call.close
                )
                val sinkEvidence = when {
                    outputVariable != null ->
                        signatureSinkScore(lexemes, call.close + 1, outputVariable)
                    isNestedInCallable(
                        lexemes,
                        call.targetStart,
                        call.close,
                        "encodeURIComponent"
                    ) -> SIGNATURE_NESTED_SINK_SCORE
                    else -> 0
                }
                addIfConfident(output, expression, SIGNATURE_BASE_CONFIDENCE + sinkEvidence)
            }

            val sourceVariable = assignedVariableForWholeRange(lexemes, decode, decodeClose)
            if (sourceVariable != null) {
                output += followVariableFlow(
                    lexemes = lexemes,
                    startIndex = decodeClose + 1,
                    sourceVariable = sourceVariable,
                    baseConfidence = SIGNATURE_BASE_CONFIDENCE,
                    sinkScore = ::signatureSinkScore
                )
            }
        }
        return output
    }

    private fun discoverNTransforms(
        javascript: String,
        anchors: List<Int>
    ): List<YoutubeSemanticTransformCandidate> {
        val output = ArrayList<YoutubeSemanticTransformCandidate>()
        nWindows(javascript, anchors).forEach { window ->
            val lexemes = tokenize(javascript, window.start, window.end)
            val keyIndex = lexemes.indexOfFirst { lexeme ->
                lexeme.kind == LexemeKind.STRING &&
                    lexeme.text == "n" &&
                    window.anchor in lexeme.start until lexeme.end
            }
            if (keyIndex < 0) return@forEach

            val getOpen = keyIndex - 1
            val getClose = matchingParen(lexemes, getOpen) ?: return@forEach
            val getCall = callSite(lexemes, getOpen, getClose)
                ?.takeIf { call ->
                    call.target.endsWith(".get") &&
                        call.arguments.size == 1 &&
                        singleString(lexemes, call.arguments[0]) == "n"
                }
                ?: return@forEach
            val receiver = getCall.target.removeSuffix(".get")
            if (!isSafeReceiverName(receiver)) return@forEach

            enclosingCalls(lexemes, getCall.targetStart, getClose).forEach callLoop@{ call ->
                if (isNonTransformTarget(call.target)) return@callLoop
                val expression = expressionForRange(
                    lexemes,
                    call,
                    getCall.targetStart,
                    getClose
                ) ?: return@callLoop
                val outputVariable = assignedVariableForWholeRange(
                    lexemes,
                    call.targetStart,
                    call.close
                )
                val sinkEvidence = when {
                    outputVariable != null ->
                        nSinkScore(lexemes, call.close + 1, outputVariable, receiver)
                    isNestedInNSet(
                        lexemes,
                        call.targetStart,
                        call.close,
                        receiver
                    ) -> N_SINK_SCORE
                    else -> 0
                }
                addIfConfident(output, expression, N_BASE_CONFIDENCE + sinkEvidence)
            }

            val sourceVariable = assignedVariableForWholeRange(
                lexemes,
                getCall.targetStart,
                getClose
            )
            if (sourceVariable != null) {
                output += followVariableFlow(
                    lexemes = lexemes,
                    startIndex = getClose + 1,
                    sourceVariable = sourceVariable,
                    baseConfidence = N_BASE_CONFIDENCE,
                    sinkScore = { values, start, variable ->
                        nSinkScore(values, start, variable, receiver)
                    }
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
        val hardLimit = minOf(lexemes.size, index + MAX_FLOW_TOKENS)
        val scopeLimit = minOf(hardLimit, scopeEnd(lexemes, index, hardLimit))

        while (index < scopeLimit) {
            if (startsNestedFunction(lexemes, index)) break
            if (lexemes[index].text != "=" || !isAssignmentOperator(lexemes, index)) {
                index++
                continue
            }

            val lhs = simpleAssignmentLhs(lexemes, index)
            if (lhs == null) {
                index++
                continue
            }

            val expressionStart = index + 1
            val expressionEnd = statementEnd(lexemes, expressionStart, scopeLimit)
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

    private fun signatureSourceRange(
        lexemes: List<Lexeme>,
        decode: Int,
        decodeClose: Int
    ): IntRange? {
        if (lexemes.getOrNull(decode + 1)?.text != "(") return null
        val arguments = splitArguments(lexemes, decode + 2, decodeClose)
        if (arguments.size != 1) return null
        return arguments[0].takeIf { range ->
            isDirectPropertyRead(lexemes, range, "s")
        }
    }

    private fun isDirectPropertyRead(
        lexemes: List<Lexeme>,
        range: IntRange,
        key: String
    ): Boolean {
        if (range.isEmpty()) return false

        if (
            range.last - range.first >= 2 &&
            lexemes[range.last].kind == LexemeKind.IDENTIFIER &&
            lexemes[range.last].text == key &&
            lexemes.getOrNull(range.last - 1)?.text == "."
        ) {
            return isSafeReceiver(lexemes, range.first, range.last - 2)
        }

        if (
            range.last - range.first >= 3 &&
            lexemes[range.last].text == "]" &&
            lexemes.getOrNull(range.last - 1)?.kind == LexemeKind.STRING &&
            lexemes[range.last - 1].text == key &&
            lexemes.getOrNull(range.last - 2)?.text == "["
        ) {
            return isSafeReceiver(lexemes, range.first, range.last - 3)
        }

        return false
    }

    private fun isSafeReceiver(
        lexemes: List<Lexeme>,
        start: Int,
        endInclusive: Int
    ): Boolean {
        if (start > endInclusive) return false
        var index = start
        if (lexemes.getOrNull(index)?.kind != LexemeKind.IDENTIFIER) return false
        index++
        while (index <= endInclusive) {
            if (
                lexemes.getOrNull(index)?.text != "." ||
                lexemes.getOrNull(index + 1)?.kind != LexemeKind.IDENTIFIER
            ) return false
            index += 2
        }
        return index == endInclusive + 1
    }

    private fun isSafeReceiverName(receiver: String): Boolean {
        if (receiver.isBlank() || receiver.length > MAX_CALLABLE_LENGTH) return false
        return receiver.split('.').all { part ->
            part.isNotBlank() &&
                isIdentifierStart(part.first()) &&
                part.drop(1).all(::isIdentifierPart)
        }
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

    private fun safeConstantArgument(
        lexemes: List<Lexeme>,
        range: IntRange
    ): String? {
        if (range.isEmpty()) return null
        val slice = range.map { lexemes[it] }
        return when {
            slice.size == 1 && isSafeInteger(slice[0]) -> slice[0].text
            slice.size == 2 &&
                slice[0].text == "-" &&
                isSafeInteger(slice[1]) -> "-${slice[1].text}"
            else -> null
        }
    }

    private fun isSafeInteger(lexeme: Lexeme): Boolean =
        lexeme.kind == LexemeKind.NUMBER &&
            lexeme.text.isNotEmpty() &&
            lexeme.text.all(Char::isDigit)

    private fun enclosingCalls(
        lexemes: List<Lexeme>,
        sourceStart: Int,
        sourceEnd: Int
    ): List<CallSite> {
        val output = ArrayList<CallSite>(MAX_ENCLOSING_CALLS)
        var index = sourceStart - 1
        var inspected = 0
        while (
            index >= 0 &&
            inspected < MAX_PARENT_SCAN_TOKENS &&
            output.size < MAX_ENCLOSING_CALLS
        ) {
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

    private fun callsBetween(
        lexemes: List<Lexeme>,
        start: Int,
        endExclusive: Int
    ): List<CallSite> {
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

    private fun callSite(
        lexemes: List<Lexeme>,
        open: Int,
        close: Int
    ): CallSite? {
        val target = callableTarget(lexemes, open) ?: return null
        return CallSite(
            target = target.first,
            targetStart = target.second,
            open = open,
            close = close,
            arguments = splitArguments(lexemes, open + 1, close)
        )
    }

    private fun callableTarget(
        lexemes: List<Lexeme>,
        open: Int
    ): Pair<String, Int>? {
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
            val last = lexemes[index]
                .takeIf { it.kind == LexemeKind.IDENTIFIER }
                ?: return null
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

        if (
            target.length > MAX_CALLABLE_LENGTH ||
            target.split('.').any { it.isBlank() }
        ) return null
        return target to start
    }

    private fun splitArguments(
        lexemes: List<Lexeme>,
        start: Int,
        close: Int
    ): List<IntRange> {
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

    private fun assignmentIndexForExpression(
        lexemes: List<Lexeme>,
        expressionStart: Int
    ): Int? {
        var index = expressionStart - 1
        var inspected = 0
        while (index >= 1 && inspected < MAX_ASSIGNMENT_SCAN_TOKENS) {
            val symbol = lexemes[index].text
            if (
                symbol == ";" ||
                symbol == "," ||
                symbol == "{" ||
                symbol == "}"
            ) return null
            if (symbol == "=" && isAssignmentOperator(lexemes, index)) return index
            index--
            inspected++
        }
        return null
    }

    private fun assignedVariableForWholeRange(
        lexemes: List<Lexeme>,
        expressionStart: Int,
        expressionEndInclusive: Int
    ): String? {
        val assignment = assignmentIndexForExpression(lexemes, expressionStart) ?: return null
        val lhs = simpleAssignmentLhs(lexemes, assignment) ?: return null
        val rhsEnd = statementEnd(
            lexemes,
            assignment + 1,
            minOf(lexemes.size, assignment + 1 + MAX_FLOW_TOKENS)
        )
        if (
            expressionStart != assignment + 1 ||
            expressionEndInclusive != rhsEnd - 1
        ) return null
        return lhs
    }

    private fun simpleAssignmentLhs(
        lexemes: List<Lexeme>,
        assignment: Int
    ): String? {
        val lhsIndex = assignment - 1
        val lhs = lexemes.getOrNull(lhsIndex)
            ?.takeIf { it.kind == LexemeKind.IDENTIFIER }
            ?: return null
        if (lexemes.getOrNull(lhsIndex - 1)?.text == ".") return null
        return lhs.text
    }

    private fun isAssignmentOperator(
        lexemes: List<Lexeme>,
        index: Int
    ): Boolean {
        return lexemes.getOrNull(index - 1)?.text != "=" &&
            lexemes.getOrNull(index + 1)?.text != "=" &&
            lexemes.getOrNull(index + 1)?.text != ">" &&
            lexemes.getOrNull(index - 1)?.text != "!" &&
            lexemes.getOrNull(index - 1)?.text != ">" &&
            lexemes.getOrNull(index - 1)?.text != "<"
    }

    private fun isAssignmentTo(
        lexemes: List<Lexeme>,
        index: Int,
        variable: String
    ): Boolean {
        if (
            lexemes.getOrNull(index)?.kind != LexemeKind.IDENTIFIER ||
            lexemes[index].text != variable ||
            lexemes.getOrNull(index - 1)?.text == "."
        ) return false
        val assignment = index + 1
        return lexemes.getOrNull(assignment)?.text == "=" &&
            isAssignmentOperator(lexemes, assignment)
    }

    private fun singleIdentifier(
        lexemes: List<Lexeme>,
        range: IntRange,
        identifier: String
    ): Boolean {
        if (range.first != range.last) return false
        val lexeme = lexemes.getOrNull(range.first) ?: return false
        return lexeme.kind == LexemeKind.IDENTIFIER &&
            lexeme.text == identifier
    }

    private fun signatureSinkScore(
        lexemes: List<Lexeme>,
        start: Int,
        variable: String
    ): Int {
        val end = minOf(lexemes.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (isAssignmentTo(lexemes, index, variable)) return 0
            if (
                lexemes[index].text == "encodeURIComponent" &&
                lexemes.getOrNull(index + 1)?.text == "("
            ) {
                val close = matchingParen(lexemes, index + 1)
                if (close != null && close < end) {
                    val args = splitArguments(lexemes, index + 2, close)
                    if (
                        args.size == 1 &&
                        singleIdentifier(lexemes, args[0], variable)
                    ) return SIGNATURE_SINK_SCORE
                }
            }
            index++
        }
        return 0
    }

    private fun nSinkScore(
        lexemes: List<Lexeme>,
        start: Int,
        variable: String,
        receiver: String
    ): Int {
        val end = minOf(lexemes.size, start + MAX_SINK_SCAN_TOKENS)
        var index = start.coerceAtLeast(0)
        while (index < end) {
            if (isAssignmentTo(lexemes, index, variable)) return 0
            if (lexemes[index].text == "(") {
                val close = matchingParen(lexemes, index)
                if (close != null && close < end) {
                    val call = callSite(lexemes, index, close)
                    if (
                        call?.target == "$receiver.set" &&
                        call.arguments.size >= 2 &&
                        singleString(lexemes, call.arguments[0]) == "n" &&
                        singleIdentifier(lexemes, call.arguments[1], variable)
                    ) return N_SINK_SCORE
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

    private fun isNestedInNSet(
        lexemes: List<Lexeme>,
        sourceStart: Int,
        sourceEnd: Int,
        receiver: String
    ): Boolean {
        return enclosingCalls(lexemes, sourceStart, sourceEnd).any { call ->
            call.target == "$receiver.set" &&
                call.arguments.size >= 2 &&
                singleString(lexemes, call.arguments[0]) == "n" &&
                call.arguments[1].first == sourceStart &&
                call.arguments[1].last == sourceEnd
        }
    }

    private fun singleString(
        lexemes: List<Lexeme>,
        range: IntRange
    ): String? {
        if (range.first != range.last) return null
        return lexemes.getOrNull(range.first)
            ?.takeIf { it.kind == LexemeKind.STRING }
            ?.text
    }

    private fun statementEnd(
        lexemes: List<Lexeme>,
        start: Int,
        limit: Int
    ): Int {
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
                ";", "," -> if (paren == 0 && bracket == 0 && brace == 0) {
                    return index
                }
            }
            index++
        }
        return limit
    }

    private fun scopeEnd(
        lexemes: List<Lexeme>,
        start: Int,
        limit: Int
    ): Int {
        var depth = 0
        var index = start
        while (index < limit) {
            when (lexemes[index].text) {
                "{" -> depth++
                "}" -> {
                    if (depth == 0) return index
                    depth--
                }
            }
            index++
        }
        return limit
    }

    private fun startsNestedFunction(
        lexemes: List<Lexeme>,
        index: Int
    ): Boolean {
        if (lexemes[index].text == "function") return true
        return lexemes[index].text == "=" &&
            lexemes.getOrNull(index + 1)?.text == ">"
    }

    private fun matchingParen(
        lexemes: List<Lexeme>,
        open: Int
    ): Int? {
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

    private fun signatureWindows(
        source: String,
        anchors: List<Int>
    ): List<ScanWindow> {
        return anchors.take(MAX_ANCHORS_PER_KIND).map { anchor ->
            ScanWindow(
                start = boundedWindowStart(source, anchor, SIGNATURE_BEFORE_CHARS),
                end = (anchor + SIGNATURE_AFTER_CHARS).coerceAtMost(source.length),
                anchor = anchor
            )
        }
    }

    private fun nWindows(
        source: String,
        anchors: List<Int>
    ): List<ScanWindow> {
        return anchors.take(MAX_ANCHORS_PER_KIND).map { anchor ->
            ScanWindow(
                start = boundedWindowStart(source, anchor, N_BEFORE_CHARS),
                end = (anchor + N_AFTER_CHARS).coerceAtMost(source.length),
                anchor = anchor
            )
        }
    }

    private fun boundedWindowStart(
        source: String,
        anchor: Int,
        beforeChars: Int
    ): Int {
        val floor = (anchor - beforeChars).coerceAtLeast(0)
        var index = anchor - 1
        while (index >= floor) {
            when (source[index]) {
                ';', '{', '}' -> return index + 1
            }
            index--
        }
        return floor
    }

    private fun scanAnchors(source: String): AnchorIndex {
        val signatures = ArrayList<Int>()
        val nAnchors = ArrayList<Int>()
        var index = 0
        var regexCanStart = true

        while (
            index < source.length &&
            (
                signatures.size < MAX_ANCHORS_PER_KIND ||
                    nAnchors.size < MAX_ANCHORS_PER_KIND
                )
        ) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++

                char == '/' && source.getOrNull(index + 1) == '/' -> {
                    index = skipLineComment(source, index + 2, source.length)
                }

                char == '/' && source.getOrNull(index + 1) == '*' -> {
                    index = skipBlockComment(source, index + 2, source.length)
                }

                char == '/' && regexCanStart -> {
                    index = skipRegexLiteral(source, index, source.length)
                    regexCanStart = false
                }

                char == '\'' || char == '"' -> {
                    val parsed = readString(source, index, source.length)
                    if (
                        parsed.value == "n" &&
                        nAnchors.size < MAX_ANCHORS_PER_KIND &&
                        isNGetAnchorSource(source, index, parsed.end)
                    ) {
                        nAnchors += index
                    }
                    index = parsed.end
                    regexCanStart = false
                }

                char == '`' -> {
                    index = skipTemplateLiteral(source, index, source.length)
                    regexCanStart = false
                }

                isIdentifierStart(char) -> {
                    val start = index
                    index++
                    while (index < source.length && isIdentifierPart(source[index])) index++
                    val identifier = source.substring(start, index)
                    if (
                        identifier == "decodeURIComponent" &&
                        signatures.size < MAX_ANCHORS_PER_KIND &&
                        isSignatureAnchorSource(source, start)
                    ) {
                        signatures += start
                    }
                    regexCanStart = identifier in REGEX_PREFIX_KEYWORDS
                }

                char.isDigit() -> {
                    index++
                    while (
                        index < source.length &&
                        (source[index].isDigit() || source[index] == '.')
                    ) index++
                    regexCanStart = false
                }

                else -> {
                    regexCanStart = symbolAllowsRegexAfter(char)
                    index++
                }
            }
        }

        return AnchorIndex(signatures, nAnchors)
    }

    private fun isSignatureAnchorSource(
        source: String,
        anchor: Int
    ): Boolean {
        val end = (anchor + SIGNATURE_SOURCE_PROBE_CHARS).coerceAtMost(source.length)
        val lexemes = tokenize(source, anchor, end)
        val decode = lexemes.indexOfFirst { lexeme ->
            lexeme.kind == LexemeKind.IDENTIFIER &&
                lexeme.text == "decodeURIComponent" &&
                anchor in lexeme.start until lexeme.end
        }
        if (decode < 0 || lexemes.getOrNull(decode + 1)?.text != "(") return false
        val close = matchingParen(lexemes, decode + 1) ?: return false
        return signatureSourceRange(lexemes, decode, close) != null
    }

    private fun isNGetAnchorSource(
        source: String,
        stringStart: Int,
        stringEnd: Int
    ): Boolean {
        var index = stringStart - 1
        while (index >= 0 && source[index].isWhitespace()) index--
        if (index < 0 || source[index] != '(') return false

        index--
        while (index >= 0 && source[index].isWhitespace()) index--
        val identifierEnd = index + 1
        while (index >= 0 && isIdentifierPart(source[index])) index--
        if (source.substring(index + 1, identifierEnd) != "get") return false

        while (index >= 0 && source[index].isWhitespace()) index--
        if (index < 0 || source[index] != '.') return false

        index = stringEnd
        while (index < source.length && source[index].isWhitespace()) index++
        return source.getOrNull(index) == ')'
    }

    private data class ParsedString(
        val value: String,
        val end: Int
    )

    private fun readString(
        source: String,
        start: Int,
        endExclusive: Int
    ): ParsedString {
        val quote = source[start]
        val value = StringBuilder()
        var index = start + 1
        while (index < endExclusive) {
            val current = source[index]
            if (current == '\\' && index + 1 < endExclusive) {
                value.append(source[index + 1])
                index += 2
                continue
            }
            if (current == quote) {
                return ParsedString(value.toString(), index + 1)
            }
            value.append(current)
            index++
        }
        return ParsedString(value.toString(), endExclusive)
    }

    private fun skipLineComment(
        source: String,
        start: Int,
        endExclusive: Int
    ): Int {
        var index = start
        while (index < endExclusive && source[index] != '\n') index++
        return index
    }

    private fun skipBlockComment(
        source: String,
        start: Int,
        endExclusive: Int
    ): Int {
        var index = start
        while (
            index + 1 < endExclusive &&
            !(source[index] == '*' && source[index + 1] == '/')
        ) index++
        return minOf(endExclusive, index + 2)
    }

    private fun skipTemplateLiteral(
        source: String,
        start: Int,
        endExclusive: Int
    ): Int {
        var index = start + 1
        while (index < endExclusive) {
            if (source[index] == '\\' && index + 1 < endExclusive) {
                index += 2
                continue
            }
            if (source[index] == '`') return index + 1
            index++
        }
        return endExclusive
    }

    private fun skipRegexLiteral(
        source: String,
        start: Int,
        endExclusive: Int
    ): Int {
        var index = start + 1
        var inClass = false
        while (index < endExclusive) {
            val current = source[index]
            if (current == '\\' && index + 1 < endExclusive) {
                index += 2
                continue
            }
            if (current == '[') {
                inClass = true
                index++
                continue
            }
            if (current == ']' && inClass) {
                inClass = false
                index++
                continue
            }
            if (current == '/' && !inClass) {
                index++
                while (
                    index < endExclusive &&
                    source[index].isLetter()
                ) index++
                return index
            }
            if (current == '\n' || current == '\r') return index
            index++
        }
        return endExclusive
    }

    private fun tokenize(
        source: String,
        start: Int,
        endExclusive: Int
    ): List<Lexeme> {
        val output = ArrayList<Lexeme>()
        var index = start
        var regexCanStart = true

        while (
            index < endExclusive &&
            output.size < MAX_WINDOW_TOKENS
        ) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++

                char == '/' && source.getOrNull(index + 1) == '/' -> {
                    index = skipLineComment(source, index + 2, endExclusive)
                }

                char == '/' && source.getOrNull(index + 1) == '*' -> {
                    index = skipBlockComment(source, index + 2, endExclusive)
                }

                char == '/' && regexCanStart -> {
                    val regexStart = index
                    index = skipRegexLiteral(source, index, endExclusive)
                    output += Lexeme(
                        LexemeKind.STRING,
                        "<regex>",
                        regexStart,
                        index
                    )
                    regexCanStart = false
                }

                char == '\'' || char == '"' -> {
                    val parsed = readString(source, index, endExclusive)
                    output += Lexeme(
                        LexemeKind.STRING,
                        parsed.value,
                        index,
                        parsed.end
                    )
                    index = parsed.end
                    regexCanStart = false
                }

                char == '`' -> {
                    val literalStart = index
                    index = skipTemplateLiteral(source, index, endExclusive)
                    output += Lexeme(
                        LexemeKind.STRING,
                        "<template>",
                        literalStart,
                        index
                    )
                    regexCanStart = false
                }

                isIdentifierStart(char) -> {
                    val lexemeStart = index
                    index++
                    while (
                        index < endExclusive &&
                        isIdentifierPart(source[index])
                    ) index++
                    val text = source.substring(lexemeStart, index)
                    output += Lexeme(
                        LexemeKind.IDENTIFIER,
                        text,
                        lexemeStart,
                        index
                    )
                    regexCanStart = text in REGEX_PREFIX_KEYWORDS
                }

                char.isDigit() -> {
                    val lexemeStart = index
                    index++
                    while (
                        index < endExclusive &&
                        (source[index].isDigit() || source[index] == '.')
                    ) index++
                    output += Lexeme(
                        LexemeKind.NUMBER,
                        source.substring(lexemeStart, index),
                        lexemeStart,
                        index
                    )
                    regexCanStart = false
                }

                else -> {
                    output += Lexeme(
                        LexemeKind.SYMBOL,
                        char.toString(),
                        index,
                        index + 1
                    )
                    regexCanStart = symbolAllowsRegexAfter(char)
                    index++
                }
            }
        }
        return output
    }

    private fun symbolAllowsRegexAfter(char: Char): Boolean =
        char !in setOf(')', ']', '}')

    private fun rank(
        values: List<YoutubeSemanticTransformCandidate>,
        budget: Int
    ): List<YoutubeSemanticTransformCandidate> {
        if (budget <= 0) return emptyList()
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
            .take(minOf(MAX_SEMANTIC_CANDIDATES_PER_KIND, budget))
    }

    private fun addIfConfident(
        output: MutableList<YoutubeSemanticTransformCandidate>,
        expression: String,
        confidence: Int
    ) {
        if (confidence >= MIN_CONFIDENCE) {
            output += YoutubeSemanticTransformCandidate(
                expression,
                confidence.coerceAtMost(MAX_DISCOVERY_CONFIDENCE)
            )
        }
    }

    private fun isIdentifierStart(char: Char): Boolean =
        char == '_' ||
            char == '$' ||
            char in 'a'..'z' ||
            char in 'A'..'Z'

    private fun isIdentifierPart(char: Char): Boolean =
        isIdentifierStart(char) || char.isDigit()

    private val NON_TRANSFORM_CALLS = setOf(
        "decodeURIComponent",
        "encodeURIComponent",
        "get",
        "set"
    )

    private val REGEX_PREFIX_KEYWORDS = setOf(
        "return",
        "throw",
        "case",
        "delete",
        "void",
        "typeof",
        "new",
        "in",
        "of",
        "yield",
        "await",
        "else",
        "do"
    )

    private val LEGACY_SIGNATURE_PRESENCE = Regex(
        "(?:&&\\s*\\(\\s*[A-Za-z0-9_$]+\\s*=\\s*[A-Za-z0-9_$]+\\s*\\(\\s*\\d+\\s*,\\s*decodeURIComponent\\s*\\(\\s*[A-Za-z0-9_$]+\\s*\\))" +
            "|(?:\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\([A-Za-z0-9_$]+\\()" +
            "|(?:\\b[A-Za-z0-9_$]+\\s*&&\\s*[A-Za-z0-9_$]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\([A-Za-z0-9_$]+\\()" +
            "|(?:\\bm=[A-Za-z0-9_$]{2,}\\(decodeURIComponent\\(h\\.s\\)\\))" +
            "|(?:\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?[A-Za-z0-9_$]+\\()"
    )

    private val LEGACY_N_PRESENCE = Regex(
        "(?:\\.get\\(\\\"n\\\"\\)\\)&&\\(b=[A-Za-z0-9_$]+(?:\\[\\d+])?\\([A-Za-z0-9_$]\\))" +
            "|(?:\\.get\\(\\\"n\\\"\\)\\)\\s*&&\\s*\\([A-Za-z0-9_$]+\\s*=\\s*[A-Za-z0-9_$]+(?:\\[\\d+])?\\([A-Za-z0-9_$]+\\))" +
            "|(?:[A-Za-z0-9_$]+\\s*=\\s*function\\([A-Za-z0-9_$]\\)\\s*\\{[^}]{0,2000}?enhanced_except_)"
    )

    private const val MIN_CONFIDENCE = 110
    private const val MAX_DISCOVERY_CONFIDENCE = 200
    private const val SIGNATURE_BASE_CONFIDENCE = 100
    private const val N_BASE_CONFIDENCE = 70
    private const val SIGNATURE_SINK_SCORE = 35
    private const val SIGNATURE_NESTED_SINK_SCORE = 35
    private const val N_SINK_SCORE = 45
    private const val MAX_ANCHORS_PER_KIND = 64
    private const val MAX_WINDOW_TOKENS = 1_600
    private const val MAX_FLOW_TOKENS = 260
    private const val MAX_SINK_SCAN_TOKENS = 180
    private const val MAX_PARENT_SCAN_TOKENS = 80
    private const val MAX_ASSIGNMENT_SCAN_TOKENS = 32
    private const val MAX_CALLS_PER_FLOW = 16
    private const val MAX_ENCLOSING_CALLS = 4
    private const val MAX_SEMANTIC_CANDIDATES_PER_KIND = 2
    private const val MAX_CALLABLE_LENGTH = 64
    private const val SIGNATURE_BEFORE_CHARS = 512
    private const val SIGNATURE_AFTER_CHARS = 2_600
    private const val SIGNATURE_SOURCE_PROBE_CHARS = 320
    private const val N_BEFORE_CHARS = 512
    private const val N_AFTER_CHARS = 2_600
}
