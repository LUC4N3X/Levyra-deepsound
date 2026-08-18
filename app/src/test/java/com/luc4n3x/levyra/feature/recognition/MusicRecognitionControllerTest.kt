package com.luc4n3x.levyra.feature.recognition

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRecognitionControllerTest {

    @Test
    fun successfulRecognitionMovesThroughListeningIdentifyingResult() = runBlocking {
        val expected = RecognitionResult(title = "Song", artist = "Artist")
        val provider = FakeRecognitionProvider { RecognitionOutcome.Match(expected) }
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            provider = provider,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Result(expected), finalState)
    }

    @Test
    fun noMatchOutcomeProducesNoMatchState() = runBlocking {
        val provider = FakeRecognitionProvider { RecognitionOutcome.NoMatch }
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            provider = provider,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.NoMatch, finalState)
    }

    @Test
    fun providerErrorOutcomeIsPassedThrough() = runBlocking {
        val provider = FakeRecognitionProvider { RecognitionOutcome.Error(RecognitionErrorKind.Network) }
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            provider = provider,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.Network), finalState)
    }

    @Test
    fun permissionDeniedDuringCaptureMapsToPermissionDeniedError() = runBlocking {
        val capture = AudioCapture { throw MicrophonePermissionDeniedException() }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.PermissionDenied), finalState)
    }

    @Test
    fun captureFailureMapsToFingerprintError() = runBlocking {
        val capture = AudioCapture { throw MicrophoneCaptureException("boom") }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.Fingerprint), finalState)
    }

    @Test
    fun malformedCapturedAudioMapsToFingerprintError() = runBlocking {
        val capture = AudioCapture {
            CapturedAudio(samples = shortArrayOf(1, 2, 3), sampleRateHz = 44_100, channelCount = 2)
        }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.Fingerprint), finalState)
    }

    @Test
    fun providerTimeoutMapsToTimeoutError() = runBlocking {
        val provider = FakeRecognitionProvider { awaitCancellation() }
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            provider = provider,
            identifyTimeoutMs = 30L,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.Timeout), finalState)
    }

    @Test
    fun duplicateStartWhileActiveIsIgnored() = runBlocking {
        val callCount = AtomicInteger(0)
        val entered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val capture = AudioCapture {
            callCount.incrementAndGet()
            entered.complete(Unit)
            gate.await()
            CapturedAudio(samples = ShortArray(200), sampleRateHz = 8_000, channelCount = 1)
        }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        withTimeout(5_000L) { entered.await() }
        controller.start()
        controller.start()
        gate.complete(Unit)

        awaitTerminalState(controller)

        assertEquals(1, callCount.get())
    }

    @Test
    fun startIsAllowedAgainAfterATerminalState() = runBlocking {
        val callCount = AtomicInteger(0)
        val firstDone = CompletableDeferred<Unit>()
        val secondDone = CompletableDeferred<Unit>()
        val provider = FakeRecognitionProvider {
            val index = callCount.getAndIncrement()
            if (index == 0) firstDone.complete(Unit) else secondDone.complete(Unit)
            RecognitionOutcome.NoMatch
        }
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            provider = provider,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        withTimeout(5_000L) { firstDone.await() }
        withTimeout(5_000L) { controller.state.first { it is RecognitionState.NoMatch } }

        controller.start()
        withTimeout(5_000L) { secondDone.await() }

        assertEquals(2, callCount.get())
    }

    @Test
    fun cancelDuringCaptureReleasesResourcesAndReportsCancelled() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val released = CompletableDeferred<Unit>()
        val capture = AudioCapture {
            entered.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                released.complete(Unit)
            }
        }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            dispatcher = Dispatchers.Default
        )

        controller.start()
        withTimeout(5_000L) { entered.await() }
        assertEquals(RecognitionState.Listening, controller.state.value)

        controller.cancel()
        val finalState = awaitTerminalState(controller)

        assertEquals(RecognitionState.Error(RecognitionErrorKind.Cancelled), finalState)
        withTimeout(5_000L) { released.await() }
    }

    @Test
    fun cancelledRunCannotOverwriteNewerRun() = runBlocking {
        val captureCalls = AtomicInteger(0)
        val firstEntered = CompletableDeferred<Unit>()
        val allowFirstCleanup = CompletableDeferred<Unit>()
        val firstReleased = CompletableDeferred<Unit>()
        val expected = RecognitionResult(title = "New song", artist = "New artist")
        val capture = AudioCapture {
            if (captureCalls.getAndIncrement() == 0) {
                firstEntered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        allowFirstCleanup.await()
                        firstReleased.complete(Unit)
                    }
                }
            }
            CapturedAudio(samples = ShortArray(400), sampleRateHz = 8_000, channelCount = 1)
        }
        val controller = MusicRecognitionController(
            audioCapture = capture,
            provider = FakeRecognitionProvider { RecognitionOutcome.Match(expected) },
            dispatcher = Dispatchers.Default
        )

        controller.start()
        withTimeout(5_000L) { firstEntered.await() }
        controller.cancel()
        controller.start()
        assertEquals(RecognitionState.Result(expected), awaitTerminalState(controller))

        allowFirstCleanup.complete(Unit)
        withTimeout(5_000L) { firstReleased.await() }
        assertEquals(RecognitionState.Result(expected), controller.state.value)
    }

    @Test
    fun cancelWithoutActiveRecognitionIsNoOp() = runBlocking {
        val controller = MusicRecognitionController(
            audioCapture = fakeCapture(),
            dispatcher = Dispatchers.Default
        )

        controller.cancel()

        assertEquals(RecognitionState.Idle, controller.state.value)
    }

    private fun fakeCapture(): AudioCapture = AudioCapture {
        CapturedAudio(samples = ShortArray(400), sampleRateHz = 8_000, channelCount = 1)
    }

    private suspend fun awaitTerminalState(controller: MusicRecognitionController): RecognitionState =
        withTimeout(5_000L) {
            controller.state.first {
                it is RecognitionState.Result || it is RecognitionState.NoMatch || it is RecognitionState.Error
            }
        }

    private class FakeRecognitionProvider(
        private val outcome: suspend () -> RecognitionOutcome
    ) : RecognitionProvider {
        override val id: String = "fake"
        override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome = outcome()
    }
}
