package com.luc4n3x.levyra.data

import android.content.Context
import android.icu.util.ULocale
import android.os.Build
import android.os.CancellationSignal
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import android.view.translation.TranslationCapability
import android.view.translation.TranslationContext
import android.view.translation.TranslationManager
import android.view.translation.TranslationRequest
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponse
import android.view.translation.TranslationResponseValue
import android.view.translation.TranslationSpec
import android.view.translation.Translator
import androidx.annotation.RequiresApi
import com.luc4n3x.levyra.domain.LyricsTranslationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal class AndroidLyricsTranslationBackend(context: Context) : LyricsTranslationBackend {
    private val appContext = context.applicationContext

    override val version: String = "android-platform-v1"

    override suspend fun translate(
        batches: List<String>,
        targetLanguageTag: String
    ): LyricsBatchTranslation {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        }
        return withContext(Dispatchers.Default) {
            translateOnAndroid12(batches, targetLanguageTag)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun translateOnAndroid12(
        batches: List<String>,
        targetLanguageTag: String
    ): LyricsBatchTranslation {
        if (batches.isEmpty()) return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        val sourceLocale = detectSourceLocale(batches.joinToString("\n").take(MAX_LANGUAGE_SAMPLE_CHARS))
            ?: return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        val targetLocale = ULocale.forLanguageTag(canonicalTranslationLanguageTag(targetLanguageTag))
        if (targetLocale.language.isNullOrBlank()) {
            return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        }
        if (sameLanguageVariant(sourceLocale, targetLocale)) return LyricsBatchTranslation.SameLanguage
        val manager = appContext.getSystemService(TranslationManager::class.java)
            ?: return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        val capability = manager.getOnDeviceTranslationCapabilities(
            TranslationSpec.DATA_FORMAT_TEXT,
            TranslationSpec.DATA_FORMAT_TEXT
        ).filter { candidate ->
            localeMatches(candidate.sourceSpec.locale, sourceLocale) &&
                localeMatches(candidate.targetSpec.locale, targetLocale)
        }.maxByOrNull(::capabilityPriority)
            ?: return LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
        if (capability.state == TranslationCapability.STATE_NOT_AVAILABLE) {
            return LyricsBatchTranslation.Unavailable(capability.unavailableState())
        }
        val flags = if (
            capability.supportedTranslationFlags and TranslationContext.FLAG_LOW_LATENCY != 0
        ) {
            TranslationContext.FLAG_LOW_LATENCY
        } else {
            0
        }
        val translationContext = TranslationContext.Builder(capability.sourceSpec, capability.targetSpec)
            .setTranslationFlags(flags)
            .build()
        val translator = createTranslator(manager, translationContext)
            ?: return LyricsBatchTranslation.Unavailable(capability.unavailableState())
        return try {
            val translated = ArrayList<String>(batches.size)
            batches.chunked(MAX_BATCHES_PER_REQUEST).forEach { requestBatches ->
                translated += translateRequest(translator, requestBatches)
                    ?: return LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
            }
            LyricsBatchTranslation.Success(translated)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            LyricsBatchTranslation.Unavailable(LyricsTranslationState.FAILED)
        } finally {
            translator.destroy()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun detectSourceLocale(text: String): ULocale? {
        if (text.isBlank()) return null
        val classifier = appContext.getSystemService(TextClassificationManager::class.java)?.textClassifier
            ?: return null
        val result = classifier.detectLanguage(TextLanguage.Request.Builder(text).build())
        if (result.localeHypothesisCount <= 0) return null
        val locale = result.getLocale(0)
        return locale.takeIf { result.getConfidenceScore(it) >= MIN_LANGUAGE_CONFIDENCE }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun createTranslator(
        manager: TranslationManager,
        context: TranslationContext
    ): Translator? = suspendCancellableCoroutine { continuation ->
        manager.createOnDeviceTranslator(context, appContext.mainExecutor) { translator ->
            if (continuation.isActive) {
                continuation.resume(translator)
            } else {
                translator?.destroy()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun translateRequest(
        translator: Translator,
        batches: List<String>
    ): List<String>? = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
        val request = TranslationRequest.Builder()
            .setFlags(TranslationRequest.FLAG_TRANSLATION_RESULT)
            .setTranslationRequestValues(batches.map(TranslationRequestValue::forText))
            .build()
        translator.translate(request, cancellationSignal, appContext.mainExecutor) { response ->
            if (!continuation.isActive) return@translate
            if (response.translationStatus != TranslationResponse.TRANSLATION_STATUS_SUCCESS) {
                continuation.resume(null)
                return@translate
            }
            val values = response.translationResponseValues
            val translated = batches.indices.mapNotNull { index ->
                values[index]
                    ?.takeIf { it.statusCode == TranslationResponseValue.STATUS_SUCCESS }
                    ?.text
                    ?.toString()
                    ?.takeIf(String::isNotBlank)
            }
            continuation.resume(translated.takeIf { it.size == batches.size })
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun TranslationCapability.unavailableState(): LyricsTranslationState = when (state) {
        TranslationCapability.STATE_AVAILABLE_TO_DOWNLOAD -> LyricsTranslationState.MODEL_DOWNLOAD_REQUIRED
        TranslationCapability.STATE_DOWNLOADING -> LyricsTranslationState.MODEL_DOWNLOADING
        else -> LyricsTranslationState.UNAVAILABLE
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun capabilityPriority(capability: TranslationCapability): Int = when (capability.state) {
        TranslationCapability.STATE_ON_DEVICE -> 3
        TranslationCapability.STATE_DOWNLOADING -> 2
        TranslationCapability.STATE_AVAILABLE_TO_DOWNLOAD -> 1
        else -> 0
    }

    private fun localeMatches(candidate: ULocale, requested: ULocale): Boolean {
        return sameTranslationLanguageVariant(candidate.toLanguageTag(), requested.toLanguageTag())
    }

    private fun sameLanguageVariant(source: ULocale, target: ULocale): Boolean {
        return sameTranslationLanguageVariant(source.toLanguageTag(), target.toLanguageTag())
    }

    companion object {
        private const val MAX_BATCHES_PER_REQUEST = 12
        private const val MAX_LANGUAGE_SAMPLE_CHARS = 8_000
        private const val MIN_LANGUAGE_CONFIDENCE = 0.45f
    }
}

internal fun canonicalTranslationLanguageTag(languageTag: String): String {
    val locale = java.util.Locale.forLanguageTag(languageTag.trim().replace('_', '-'))
    if (!locale.language.equals("zh", ignoreCase = true)) return locale.toLanguageTag()
    val script = when {
        locale.script.equals("Hant", ignoreCase = true) -> "Hant"
        locale.script.equals("Hans", ignoreCase = true) -> "Hans"
        locale.country.uppercase(java.util.Locale.ROOT) in TRADITIONAL_CHINESE_REGIONS -> "Hant"
        else -> "Hans"
    }
    return "zh-$script"
}

internal fun sameTranslationLanguageVariant(sourceTag: String, targetTag: String): Boolean {
    val source = java.util.Locale.forLanguageTag(canonicalTranslationLanguageTag(sourceTag))
    val target = java.util.Locale.forLanguageTag(canonicalTranslationLanguageTag(targetTag))
    if (!source.language.equals(target.language, ignoreCase = true)) return false
    return !source.language.equals("zh", ignoreCase = true) ||
        source.script.equals(target.script, ignoreCase = true)
}

private val TRADITIONAL_CHINESE_REGIONS = setOf("TW", "HK", "MO")
