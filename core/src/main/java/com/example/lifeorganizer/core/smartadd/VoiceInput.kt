package com.example.lifeorganizer.core.smartadd

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * In-dialog speech recognition with live partial results.
 *
 * Uses Android's on-device recogniser when one is available (offline), otherwise the normal one.
 * The old implementation forced EXTRA_PREFER_OFFLINE, which fails without a downloaded language pack.
 */
class VoiceInput(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onEnd: (error: Int?) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var triedOnline = false

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(lang: String) {
        stop()
        val onDevice = !triedOnline && Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        recognizer = (if (onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        else SpeechRecognizer.createSpeechRecognizer(context)).apply {
            setRecognitionListener(listener(lang, onDevice))
            startListening(intent(lang))
        }
    }

    fun stop() {
        recognizer?.run {
            stopListening()
            destroy()
        }
        recognizer = null
    }

    private fun intent(lang: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag(lang))
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun listener(lang: String, onDevice: Boolean) = object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle) {
            partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                ?.takeIf { it.isNotBlank() }?.let(onPartial)
        }

        override fun onResults(results: Bundle) {
            val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) onFinal(text)
            stop()
            onEnd(null)
        }

        override fun onError(error: Int) {
            stop()
            // On-device model missing for this language: retry once with the online recogniser.
            val languageProblem = error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
            if (onDevice && languageProblem) {
                triedOnline = true
                start(lang)
                return
            }
            onEnd(error)
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        fun languageTag(lang: String) = when (lang) {
            "de" -> "de-DE"
            "tr" -> "tr-TR"
            "es" -> "es-ES"
            "zh" -> "zh-CN"
            else -> "en-US"
        }
    }
}
