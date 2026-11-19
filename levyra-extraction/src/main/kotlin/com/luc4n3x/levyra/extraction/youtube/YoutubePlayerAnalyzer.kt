package com.luc4n3x.levyra.extraction.youtube

object YoutubePlayerAnalyzer {
    private data class Rule(
        val regex: Regex,
        val expression: (List<String>) -> String?
    )

    private val signatureRules = listOf(
        Rule(
            Regex("&&\\s*\\(\\s*[A-Za-z0-9_$]+\\s*=\\s*([A-Za-z0-9_$]+)\\s*\\(\\s*(\\d+)\\s*,\\s*decodeURIComponent\\s*\\(\\s*[A-Za-z0-9_$]+\\s*\\)"),
            { groups -> safeName(groups[1])?.let { "$it(${groups[2]},INPUT)" } }
        ),
        Rule(
            Regex("\\b[cs]\\s*&&\\s*[adf]\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\(([A-Za-z0-9_$]+)\\("),
            { groups -> safeName(groups[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\b[A-Za-z0-9_$]+\\s*&&\\s*[A-Za-z0-9_$]+\\.set\\([^,]+\\s*,\\s*encodeURIComponent\\(([A-Za-z0-9_$]+)\\("),
            { groups -> safeName(groups[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\bm=([A-Za-z0-9_$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)"),
            { groups -> safeName(groups[1])?.let { "$it(INPUT)" } }
        ),
        Rule(
            Regex("\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?([A-Za-z0-9_$]+)\\("),
            { groups -> safeName(groups[1])?.let { "$it(INPUT)" } }
        )
    )

    private val nRules = listOf(
        Rule(
            Regex("\\.get\\(\\\"n\\\"\\)\\)&&\\(b=([A-Za-z0-9_$]+)(?:\\[(\\d+)])?\\([A-Za-z0-9_$]\\)"),
            { groups -> functionExpression(groups[1], groups.getOrNull(2), "INPUT") }
        ),
        Rule(
            Regex("\\.get\\(\\\"n\\\"\\)\\)\\s*&&\\s*\\(([A-Za-z0-9_$]+)\\s*=\\s*([A-Za-z0-9_$]+)(?:\\[(\\d+)])?\\(\\1\\)"),
            { groups -> functionExpression(groups[2], groups.getOrNull(3), "INPUT") }
        ),
        Rule(
            Regex("([A-Za-z0-9_$]+)\\s*=\\s*function\\([A-Za-z0-9_$]\\)\\s*\\{[^}]{0,2000}?enhanced_except_"),
            { groups -> functionExpression(groups[1], null, "INPUT") }
        )
    )

    private val anchoredSts = Regex("signatureTimestamp['\\\":\\s]+(\\d{4,8})")
    private val looseSts = Regex("[,{]sts\\s*:\\s*(\\d{4,8})")
    private val safeFunctionName = Regex("^[A-Za-z_$][A-Za-z0-9_$]{0,31}$")

    fun analyze(javascript: String): YoutubePlayerAnalysis {
        if (javascript.length > MAX_SOURCE_LENGTH) return unavailable(YoutubePlayerAnalysis.Reason.SOURCE_TOO_LARGE)
        if (javascript.isBlank()) return unavailable(YoutubePlayerAnalysis.Reason.EMPTY_SOURCE)
        val sts = extractSignatureTimestamp(javascript)
            ?: return unavailable(YoutubePlayerAnalysis.Reason.MISSING_SIGNATURE_TIMESTAMP)
        val anchored = YoutubePlayerUrlFactoryAnalyzer.discover(javascript)
        val semantic = YoutubePlayerSemanticAnalyzerV2.discover(javascript)
        val signatures = anchored.signatures + mergeExpressions(
            semantic.signatures,
            expressions(javascript, signatureRules)
        ).filterNot { candidate -> anchored.signatures.any { it.expression == candidate.expression } }
        val nExpressions = anchored.nTransforms + mergeExpressions(
            semantic.nTransforms,
            expressions(javascript, nRules)
        ).filterNot { candidate -> anchored.nTransforms.any { it.expression == candidate.expression } }
        if (signatures.isEmpty()) return unavailable(YoutubePlayerAnalysis.Reason.MISSING_SIGNATURE_TRANSFORM)
        if (nExpressions.isEmpty()) return unavailable(YoutubePlayerAnalysis.Reason.MISSING_N_TRANSFORM)

        val combinations = ArrayList<Pair<YoutubeSemanticTransformCandidate, YoutubeSemanticTransformCandidate>>()
        signatures.forEach { signature ->
            nExpressions.forEach { nExpression -> combinations += signature to nExpression }
        }
        combinations.sortWith(
            compareByDescending<Pair<YoutubeSemanticTransformCandidate, YoutubeSemanticTransformCandidate>> {
                it.first.confidence + it.second.confidence
            }.thenByDescending { it.first.confidence }
                .thenByDescending { it.second.confidence }
        )
        val transforms = combinations.asSequence()
            .map { (signature, nExpression) ->
                YoutubePlayerTransform(
                    signatureExpression = signature.expression,
                    nExpression = nExpression.expression,
                    confidence = signature.confidence + nExpression.confidence
                )
            }
            .distinctBy { it.signatureExpression to it.nExpression }
            .take(MAX_CANDIDATES)
            .toList()
        return YoutubePlayerAnalysis.Candidates(sts, transforms)
    }

    fun extractSignatureTimestamp(javascript: String): Int? {
        if (javascript.length > MAX_SOURCE_LENGTH) return null
        return anchoredSts.find(javascript)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: looseSts.find(javascript)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun mergeExpressions(
        semantic: List<YoutubeSemanticTransformCandidate>,
        legacy: List<String>
    ): List<YoutubeSemanticTransformCandidate> {
        val output = LinkedHashMap<String, YoutubeSemanticTransformCandidate>()
        semantic.take(MAX_SEMANTIC_EXPRESSIONS_PER_KIND).forEach { candidate ->
            output[candidate.expression] = candidate
        }
        legacy.forEach { expression ->
            if (output.size >= MAX_EXPRESSIONS_PER_KIND) return@forEach
            output.putIfAbsent(
                expression,
                YoutubeSemanticTransformCandidate(expression, LEGACY_CONFIDENCE)
            )
        }
        if (output.size < MAX_EXPRESSIONS_PER_KIND) {
            semantic.drop(MAX_SEMANTIC_EXPRESSIONS_PER_KIND).forEach { candidate ->
                if (output.size < MAX_EXPRESSIONS_PER_KIND) output.putIfAbsent(candidate.expression, candidate)
            }
        }
        return output.values.toList()
    }

    private fun expressions(javascript: String, rules: List<Rule>): List<String> {
        val expressions = LinkedHashSet<String>()
        for (rule in rules) {
            javascript.forEachGroupMatch(rule.regex, MAX_MATCHES_PER_RULE) { groups ->
                rule.expression(groups)?.let(expressions::add)
            }
            if (expressions.size >= MAX_EXPRESSIONS_PER_KIND) break
        }
        return expressions.take(MAX_EXPRESSIONS_PER_KIND)
    }

    private fun functionExpression(name: String, rawIndex: String?, input: String): String? {
        val safe = safeName(name) ?: return null
        val index = rawIndex.orEmpty().takeIf { it.isNotBlank() }?.toIntOrNull()
        return if (index == null) "$safe($input)" else "$safe[$index]($input)"
    }

    private fun safeName(name: String): String? = name.takeIf(safeFunctionName::matches)

    private fun unavailable(reason: YoutubePlayerAnalysis.Reason) = YoutubePlayerAnalysis.Unavailable(reason)

    fun buildNExpression(nClass: String): String {
        require(nClassRegex.matches(nClass))
        return "(function(n){try{var u=new g.$nClass('https://x.googlevideo.com/videoplayback?n='+n,true);var t=u.get('n');return(t&&t!==n)?t:n;}catch(e){return n;}})(INPUT)"
    }

    private val nClassRegex = Regex("^[A-Za-z0-9${'$'}_]{1,8}$")
    const val MAX_SOURCE_LENGTH: Int = 6_000_000

    private const val LEGACY_CONFIDENCE = 40
    private const val MAX_MATCHES_PER_RULE = 4
    private const val MAX_SEMANTIC_EXPRESSIONS_PER_KIND = 2
    private const val MAX_EXPRESSIONS_PER_KIND = 3
    private const val MAX_CANDIDATES = 6
}

internal inline fun String.forEachGroupMatch(regex: Regex, limit: Int, action: (List<String>) -> Unit) {
    val matcher = regex.toPattern().matcher(this)
    var count = 0
    while (count < limit && matcher.find()) {
        action(List(matcher.groupCount() + 1) { index -> matcher.group(index).orEmpty() })
        count++
    }
}
