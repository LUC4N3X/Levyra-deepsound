from pathlib import Path

source = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeLocalDecoder.kt")
tests = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeLocalDecoderTest.kt")


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    matches = text.count(old)
    if matches != 1:
        raise SystemExit(f"{path}: expected one match, found {matches}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    source,
    '''        runtimeMutex.withLock {
            ensureRuntime(player, resolvedConfig, recordRendererFailures = false)
        }
''',
    '''        runtimeMutex.withLock {
            ensureRuntime(player, resolvedConfig)
        }
''',
)

replace_once(
    source,
    '''        return try {
            runtimeMutex.withLock {
                val active = ensureRuntime(player, resolvedConfig)
                val signatureResults = LinkedHashMap<String, String>()
                val nResults = LinkedHashMap<String, String>()
                signatures.forEach { input ->
                    val output = active.decodeSignature(input)
                    if (output.isBlank()) throw ParsingException("Empty local signature result")
                    signatureResults[input] = output
                }
                nValues.forEach { input ->
                    val output = active.transformN(input)
                    if (output.isBlank()) throw ParsingException("Empty local n-transform result")
                    nResults[input] = output
                }
                lastSuccessfulDecodePlayerHash = player.hash
                lastSuccessfulDecodeOrigin = resolvedConfig.origin
                lastSuccessfulDecodeAtMs.set(System.currentTimeMillis())
                YoutubeDecodedBatch(signatureResults, nResults, runtimeConfigEpoch)
            }
        } catch (error: Throwable) {
''',
    '''        return try {
            runtimeMutex.withLock {
                val recoveryIdentity = "${player.hash}:${configStore.epoch}"
                try {
                    val active = ensureRuntime(player, resolvedConfig)
                    val signatureResults = LinkedHashMap<String, String>()
                    val nResults = LinkedHashMap<String, String>()
                    signatures.forEach { input ->
                        val output = active.decodeSignature(input)
                        if (output.isBlank()) {
                            throw YoutubeAnalyzedConfigFailureException("Empty local signature result")
                        }
                        signatureResults[input] = output
                    }
                    nValues.forEach { input ->
                        val output = active.transformN(input)
                        if (output.isBlank()) {
                            throw YoutubeAnalyzedConfigFailureException("Empty local n-transform result")
                        }
                        nResults[input] = output
                    }
                    lastSuccessfulDecodePlayerHash = player.hash
                    lastSuccessfulDecodeOrigin = resolvedConfig.origin
                    lastSuccessfulDecodeAtMs.set(System.currentTimeMillis())
                    rendererRecovery.onSuccess()
                    YoutubeDecodedBatch(signatureResults, nResults, runtimeConfigEpoch)
                } catch (error: Throwable) {
                    if (YoutubeRendererFailureClassifier.countsAsRendererFailure(error)) {
                        rendererRecovery.onFailure(recoveryIdentity, SystemClock.elapsedRealtime())
                    }
                    throw error
                }
            }
        } catch (error: Throwable) {
''',
)

replace_once(
    source,
    '''    private suspend fun ensureRuntime(
        player: YoutubePlayerScript,
        config: YoutubePlayerCipherConfig,
        recordRendererFailures: Boolean = true
    ): YoutubeCipherWebRuntime {
        val current = runtime
        if (
            current != null &&
            YoutubeRuntimeReusePolicy.canReuse(
                isDead = current.isDead,
                runtimeHash = runtimeHash,
                requestedHash = player.hash,
                runtimeConfigKey = runtimeConfigKey,
                requestedConfigKey = player.configKey,
                runtimeEpoch = runtimeConfigEpoch,
                currentEpoch = configStore.epoch
            )
        ) {
            return current
        }
        invalidateRuntimeLocked()
        val recoveryIdentity = "${player.hash}:${configStore.epoch}"
        if (!rendererRecovery.shouldAttempt(recoveryIdentity, SystemClock.elapsedRealtime())) {
            throw YoutubeRendererBackoffException()
        }
        val created = try {
            YoutubeCipherWebRuntime.create(context, player, config)
        } catch (error: Throwable) {
            if (recordRendererFailures && YoutubeRendererFailureClassifier.countsAsRendererFailure(error)) {
                rendererRecovery.onFailure(recoveryIdentity, SystemClock.elapsedRealtime())
            }
            throw error
        }
        rendererRecovery.onSuccess()
        runtime = created
        runtimeHash = player.hash
        runtimeConfigKey = player.configKey
        runtimeConfigEpoch = configStore.epoch
        runtimeConfigOrigin = config.origin
        return created
    }
''',
    '''    private suspend fun ensureRuntime(
        player: YoutubePlayerScript,
        config: YoutubePlayerCipherConfig
    ): YoutubeCipherWebRuntime {
        val current = runtime
        if (
            current != null &&
            YoutubeRuntimeReusePolicy.canReuse(
                isDead = current.isDead,
                runtimeHash = runtimeHash,
                requestedHash = player.hash,
                runtimeConfigKey = runtimeConfigKey,
                requestedConfigKey = player.configKey,
                runtimeEpoch = runtimeConfigEpoch,
                currentEpoch = configStore.epoch
            )
        ) {
            return current
        }
        invalidateRuntimeLocked()
        val recoveryIdentity = "${player.hash}:${configStore.epoch}"
        if (!rendererRecovery.shouldAttempt(recoveryIdentity, SystemClock.elapsedRealtime())) {
            throw YoutubeRendererBackoffException()
        }
        val created = YoutubeCipherWebRuntime.create(context, player, config)
        runtime = created
        runtimeHash = player.hash
        runtimeConfigKey = player.configKey
        runtimeConfigEpoch = configStore.epoch
        runtimeConfigOrigin = config.origin
        return created
    }
''',
)

replace_once(
    source,
    '''internal class YoutubeRendererBackoffException : ParsingException("Local decoder renderer in recovery backoff")

internal class YoutubeRendererFailureException(message: String) : ParsingException(message)

internal object YoutubeRendererFailureClassifier {
    fun countsAsRendererFailure(error: Throwable): Boolean = error is YoutubeRendererFailureException

    fun provesAnalyzedConfigWrong(error: Throwable): Boolean =
        error !is YoutubeRendererBackoffException && error !is YoutubeRendererFailureException
}
''',
    '''internal class YoutubeRendererBackoffException : ParsingException("Local decoder renderer in recovery backoff")

internal class YoutubeRendererFailureException(message: String) : ParsingException(message)

internal class YoutubeAnalyzedConfigFailureException(message: String) : ParsingException(message)

internal object YoutubeRendererFailureClassifier {
    fun countsAsRendererFailure(error: Throwable): Boolean = error is YoutubeRendererFailureException

    fun provesAnalyzedConfigWrong(error: Throwable): Boolean =
        error is YoutubeAnalyzedConfigFailureException
}
''',
)

replace_once(
    source,
    '''        if (index < 0) throw ParsingException("YouTube player export marker not found")
''',
    '''        if (index < 0) {
            throw YoutubeAnalyzedConfigFailureException("YouTube player export marker not found")
        }
''',
)

replace_once(
    source,
    '''        if (isDead || closed.get()) throw ParsingException("Local decoder runtime unavailable")
''',
    '''        if (isDead || closed.get()) {
            throw YoutubeRendererFailureException("Local decoder runtime unavailable")
        }
''',
)

replace_once(
    source,
    '''                if (isDead || closed.get()) throw ParsingException("Local decoder renderer unavailable")
''',
    '''                if (isDead || closed.get()) {
                    throw YoutubeRendererFailureException("Local decoder renderer unavailable")
                }
''',
)

replace_once(
    source,
    '''                throw ParsingException("Invalid local n-transform result")
''',
    '''                throw YoutubeAnalyzedConfigFailureException("Invalid local n-transform result")
''',
)

replace_once(
    source,
    '''        } catch (error: Throwable) {
            if (YoutubeCipherRuntimeFailurePolicy.marksRuntimeDead(error)) isDead = true
            throw error
        } finally {
''',
    '''        } catch (error: Throwable) {
            if (error is TimeoutCancellationException && currentCoroutineContext().isActive) {
                isDead = true
                throw YoutubeRendererFailureException("Local decoder renderer evaluation timeout")
            }
            if (YoutubeCipherRuntimeFailurePolicy.marksRuntimeDead(error)) isDead = true
            throw error
        } finally {
''',
)

replace_once(
    source,
    '''        waiters[requestId]?.completeExceptionally(ParsingException(message))
''',
    '''        waiters[requestId]?.completeExceptionally(YoutubeAnalyzedConfigFailureException(message))
''',
)

replace_once(
    source,
    '''                runtime.ready.completeExceptionally(
                    ParsingException(
                        "Local decoder exports unavailable sig=$signatureAvailable sigValid=$signatureValidated n=$nAvailable nValid=$nValidated"
                    )
                )
''',
    '''                runtime.ready.completeExceptionally(
                    YoutubeAnalyzedConfigFailureException(
                        "Local decoder exports unavailable sig=$signatureAvailable sigValid=$signatureValidated n=$nAvailable nValid=$nValidated"
                    )
                )
''',
)

replace_once(
    tests,
    '''import java.io.File
''',
    '''import java.io.File
import java.io.IOException
''',
)

replace_once(
    tests,
    '''        assertTrue(classifier.countsAsRendererFailure(YoutubeRendererFailureException("Local decoder renderer ready timeout")))
        assertTrue(classifier.countsAsRendererFailure(YoutubeRendererFailureException("Local player JS load failed: net::ERR_FAILED")))
''',
    '''        assertTrue(classifier.countsAsRendererFailure(YoutubeRendererFailureException("Local decoder renderer ready timeout")))
        assertTrue(classifier.countsAsRendererFailure(YoutubeRendererFailureException("Local decoder renderer evaluation timeout")))
        assertTrue(classifier.countsAsRendererFailure(YoutubeRendererFailureException("Local player JS load failed: net::ERR_FAILED")))
''',
)

replace_once(
    tests,
    '''    @Test
    fun backoffAndRendererFailuresDoNotRejectAnalyzedConfig() {
        val classifier = YoutubeRendererFailureClassifier

        assertFalse(classifier.provesAnalyzedConfigWrong(YoutubeRendererBackoffException()))
        assertFalse(classifier.provesAnalyzedConfigWrong(YoutubeRendererFailureException("Local decoder render process gone")))
        assertTrue(classifier.provesAnalyzedConfigWrong(ParsingException("Local decoder exports unavailable sig=false")))
        assertTrue(classifier.provesAnalyzedConfigWrong(ParsingException("Invalid local n-transform result")))
    }
''',
    '''    @Test
    fun onlyExplicitValidationFailuresRejectAnalyzedConfig() {
        val classifier = YoutubeRendererFailureClassifier
        val outerTimeout = runCatching {
            runBlocking { withTimeout(1L) { delay(50L) } }
        }.exceptionOrNull()

        assertNotNull(outerTimeout)
        assertFalse(classifier.provesAnalyzedConfigWrong(YoutubeRendererBackoffException()))
        assertFalse(classifier.provesAnalyzedConfigWrong(YoutubeRendererFailureException("Local decoder render process gone")))
        assertFalse(classifier.provesAnalyzedConfigWrong(CancellationException("caller went away")))
        assertFalse(classifier.provesAnalyzedConfigWrong(outerTimeout!!))
        assertFalse(classifier.provesAnalyzedConfigWrong(ParsingException("Unrelated decoder failure")))
        assertFalse(classifier.provesAnalyzedConfigWrong(IOException("player cache unavailable")))
        assertTrue(
            classifier.provesAnalyzedConfigWrong(
                YoutubeAnalyzedConfigFailureException("Local decoder exports unavailable sig=false")
            )
        )
        assertTrue(
            classifier.provesAnalyzedConfigWrong(
                YoutubeAnalyzedConfigFailureException("Invalid local n-transform result")
            )
        )
    }
''',
)

for temporary in (
    Path(".github/workflows/pr294-self-fix.yml"),
    Path(".github/pr294_fix.py"),
):
    if temporary.exists():
        temporary.unlink()
