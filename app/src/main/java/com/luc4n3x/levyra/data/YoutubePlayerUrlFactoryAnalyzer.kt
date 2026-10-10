package com.luc4n3x.levyra.data

internal object YoutubePlayerUrlFactoryAnalyzer {
    private val factoryRegex = Regex(
        "(?<![A-Za-z0-9_$.])([A-Za-z_$][A-Za-z0-9_$]{0,31})\\s*=\\s*function\\([^(){}]{0,120}\\)\\s*\\{([^{}]{0,600}?)\\.set\\(\\s*\"alr\"\\s*,\\s*\"yes\"\\s*\\)\\s*;\\s*[A-Za-z0-9_$]+\\s*&&"
    )
    private val urlClassRegex = Regex(
        "new\\s+g\\.([A-Za-z0-9_$]{1,8})\\(\\s*[A-Za-z0-9_$]+\\s*,\\s*(?:!0|true)\\s*\\)"
    )

    fun discover(javascript: String): YoutubeSemanticDiscoveryResult {
        val factories = factoryRegex.findAll(javascript)
            .take(MAX_FACTORY_MATCHES)
            .toList()
        if (factories.size != 1) return YoutubeSemanticDiscoveryResult(emptyList(), emptyList())
        val factory = factories.single()
        val name = factory.groupValues[1]
        val body = factory.groupValues[2]
        val nTransforms = buildList {
            urlClassRegex.findAll(body).map { it.groupValues[1] }.distinct().toList().singleOrNull()?.let { nClass ->
                add(
                    YoutubeSemanticTransformCandidate(
                        YoutubePlayerConfigParser.buildNExpression(nClass),
                        URL_CLASS_CONFIDENCE
                    )
                )
            }
            add(YoutubeSemanticTransformCandidate(factoryNExpression(name), FACTORY_CONFIDENCE))
        }
        val signatures = listOf(
            YoutubeSemanticTransformCandidate(factorySignatureExpression(name), FACTORY_CONFIDENCE)
        )
        return YoutubeSemanticDiscoveryResult(signatures, nTransforms)
    }

    internal fun factoryNExpression(factory: String): String =
        "(function(n){var u=$factory(\"$PROBE_URL\",\"s\",void 0);u.set(\"n\",n);" +
            "var p=Object.getPrototypeOf(u),k=Object.keys(p).concat(Object.getOwnPropertyNames(p)),i;" +
            "for(i=0;i<k.length;i++){if([\"constructor\",\"set\",\"get\",\"clone\"].indexOf(k[i])<0&&" +
            "typeof u[k[i]]===\"function\"){u[k[i]]();break;}}" +
            "var r=u.get(\"n\");return typeof r===\"string\"?r:n;})(INPUT)"

    internal fun factorySignatureExpression(factory: String): String =
        "(function(s){var u=$factory(\"$PROBE_URL\",\"s\",encodeURIComponent(s));" +
            "var r=u.get(\"s\");return typeof r===\"string\"?decodeURIComponent(r):s;})(INPUT)"

    private const val PROBE_URL = "https://www.youtube.com/watch?v=levyra"
    private const val MAX_FACTORY_MATCHES = 4
    private const val URL_CLASS_CONFIDENCE = 190
    private const val FACTORY_CONFIDENCE = 120
}
