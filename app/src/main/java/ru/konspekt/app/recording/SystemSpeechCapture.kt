package ru.konspekt.app.recording

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Optional high-accuracy engine backed by the on-device Android recognition
 * service. It uses much less RAM than a large offline model; on phones with Google
 * services the Russian language pack may be downloaded/offline. The UI tells
 * the user that the system service may process audio.
 */
class SystemSpeechCapture(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit
) : SpeechCapture {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var lastPartial = ""

    @Volatile
    private var running = false

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }

    override fun start() {
        if (running) return
        running = true
        main.post {
            if (!running) return@post
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("Системное распознавание недоступно на этом устройстве")
                running = false
                return@post
            }
            onStatus("Системное распознавание…")
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = r
            r.setRecognitionListener(listener)
            r.startListening(intent)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (running) onStatus("Слушаю микрофон…")
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            if (running) {
                onAmplitude(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!running) return
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    commitPartial()
                    restart()
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    fail("Нет доступа к микрофону")
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    fail("Нет сети для системного распознавания")
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                    fail("Русский язык недоступен в системном распознавании")
                else -> fail("Ошибка системного распознавания")
            }
        }

        override fun onResults(results: Bundle?) {
            if (!running) return
            val partial = lastPartial
            lastPartial = ""
            val finalText = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
                .trim()
            // Android's recognizer sometimes returns a shorter final phrase
            // than the last partial, dropping words that were already shown.
            val text = when {
                finalText.isBlank() -> partial
                partial.isBlank() -> finalText
                partial.length > finalText.length &&
                    partial.contains(finalText, ignoreCase = true) -> partial
                else -> finalText
            }
            if (text.isNotBlank()) onFinal(text)
            onPartial("")
            restart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!running) return
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
                .trim()
            if (text.isNotBlank()) {
                lastPartial = text
                onPartial(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun restart() {
        if (!running) return
        main.postDelayed({
            if (!running) return@postDelayed
            runCatching { recognizer?.startListening(intent) }
                .onFailure { fail("Не удалось продолжить системное распознавание") }
        }, 250)
    }

    private fun commitPartial() {
        val text = lastPartial.trim()
        lastPartial = ""
        if (text.isNotBlank()) {
            onFinal(text)
            onPartial("")
        }
    }

    private fun fail(message: String) {
        commitPartial()
        running = false
        onError(message)
        cleanup()
    }

    override fun stop() {
        running = false
        if (Looper.myLooper() == Looper.getMainLooper()) {
            commitPartial()
            cleanup()
        } else {
            main.post {
                commitPartial()
                cleanup()
            }
        }
    }

    private fun cleanup() {
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }
}
