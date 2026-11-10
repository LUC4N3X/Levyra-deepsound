package com.luc4n3x.levyra.extraction.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerAnalyzerTest {
    @Test
    fun replaysLegacyPlayerThroughThePublicApi() {
        val result = candidates(fixture("legacy"))

        assertEquals(20644, result.signatureTimestamp)
        assertEquals(
            listOf(YoutubePlayerTransform("Ab(4,INPUT)", "Nz[2](INPUT)", 80)),
            result.transforms
        )
    }

    @Test
    fun replaysRenamedPlayerWithoutRemoteConfiguration() {
        val result = candidates(fixture("semantic"))

        assertEquals(20711, result.signatureTimestamp)
        assertEquals("RenamedSignature(7,INPUT)", result.transforms.first().signatureExpression)
        assertEquals("RenamedThrottle[3](INPUT)", result.transforms.first().nExpression)
        assertTrue(result.transforms.first().confidence > 80)
    }

    @Test
    fun replayRejectsTransformsThatNoLongerDependOnTheSource() {
        assertUnavailable(fixture("unsupported"), YoutubePlayerAnalysis.Reason.MISSING_SIGNATURE_TRANSFORM)
    }

    @Test
    fun emptySourceHasAnExplicitOutcome() {
        assertUnavailable(" \n\t", YoutubePlayerAnalysis.Reason.EMPTY_SOURCE)
    }

    @Test
    fun oversizedSourceIsRejectedBeforePartialAnalysis() {
        val source = fixture("legacy") + " ".repeat(YoutubePlayerAnalyzer.MAX_SOURCE_LENGTH)

        assertUnavailable(source, YoutubePlayerAnalysis.Reason.SOURCE_TOO_LARGE)
        assertEquals(null, YoutubePlayerAnalyzer.extractSignatureTimestamp(source))
    }

    @Test
    fun sourceAtTheLimitRemainsEligible() {
        val source = fixture("legacy").padEnd(YoutubePlayerAnalyzer.MAX_SOURCE_LENGTH)

        assertEquals(20644, candidates(source).signatureTimestamp)
    }

    @Test
    fun transformsWithoutTimestampAreNotPublished() {
        val source = fixture("legacy").replace("signatureTimestamp:20644", "unknown:20644")

        assertUnavailable(source, YoutubePlayerAnalysis.Reason.MISSING_SIGNATURE_TIMESTAMP)
    }

    @Test
    fun timestampWithoutTransformsIsNotAPlayableCandidate() {
        assertUnavailable("var cfg={sts:20644};", YoutubePlayerAnalysis.Reason.MISSING_SIGNATURE_TRANSFORM)
    }

    @Test
    fun signatureWithoutNTransformHasADistinctOutcome() {
        val source = "var cfg={signatureTimestamp:20644};x&&(y=Ab(4,decodeURIComponent(z)));"

        assertUnavailable(source, YoutubePlayerAnalysis.Reason.MISSING_N_TRANSFORM)
    }

    @Test
    fun ambiguousTransformsRemainSeparateForHostVerification() {
        val source = fixture("legacy") + "q&&(r=Cd(5,decodeURIComponent(s)));"
        val result = candidates(source)

        assertEquals(listOf("Ab(4,INPUT)", "Cd(5,INPUT)"), result.transforms.map { it.signatureExpression })
    }

    @Test(expected = IllegalArgumentException::class)
    fun urlClassExpressionRejectsExecutableInput() {
        YoutubePlayerAnalyzer.buildNExpression("x);evil()")
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/youtube/players/$name.js")).readText()

    private fun candidates(source: String): YoutubePlayerAnalysis.Candidates =
        YoutubePlayerAnalyzer.analyze(source) as YoutubePlayerAnalysis.Candidates

    private fun assertUnavailable(source: String, reason: YoutubePlayerAnalysis.Reason) {
        assertEquals(YoutubePlayerAnalysis.Unavailable(reason), YoutubePlayerAnalyzer.analyze(source))
    }
}
