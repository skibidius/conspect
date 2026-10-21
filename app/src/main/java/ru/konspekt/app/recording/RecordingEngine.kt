package ru.konspekt.app.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import java.io.File
import kotlin.math.abs

/**
 * Native microphone capture. Audio is written to the app cache and deleted
 * after the note is generated, so it never leaves the device.
 */
class AudioCapture(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    var isRecording = false
        private set

    fun start(): Boolean {
        if (isRecording) return true
        val output = File(context.cacheDir, "lecture-${SystemClock.elapsedRealtime()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return runCatching {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            file = output
            isRecording = true
            true
        }.getOrElse {
            mediaRecorder.release()
            output.delete()
            false
        }
    }

    fun amplitude(): Float {
        val value = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (value / 32767f).coerceIn(0f, 1f)
    }

    fun pause() {
        if (!isRecording) return
        runCatching { recorder?.pause() }
    }

    fun resume() {
        if (!isRecording) return
        runCatching { recorder?.resume() }
    }

    fun stop() {
        recorder?.let { recorder ->
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }
        recorder = null
        isRecording = false
        file?.delete()
        file = null
    }
}

/**
 * Uses Android's built-in recognizer, explicitly preferring the offline model.
 * No network permission is declared by the app.
 */
class OfflineSpeechCapture(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onUnavailable: (String) -> Unit
) : RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var stopped = false
    private var restartOnResults = true

    fun start(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onUnavailable("Офлайн-распознавание недоступно")
            return false
        }
        stopped = false
        val created = runCatching {
            SpeechRecognizer.createSpeechRecognizer(context)
        }.getOrNull() ?: run {
            onUnavailable("Офлайн-распознавание недоступно")
            return false
        }
        recognizer = created
        created.setRecognitionListener(this)
        return listen()
    }

    private fun listen(): Boolean = runCatching {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
        true
    }.getOrDefault(false)

    fun stop() {
        stopped = true
        restartOnResults = false
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) {
        if (stopped) return
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            listen()
            return
        }
        onUnavailable(
            when (error) {
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Офлайн-модель не установлена"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет доступа к микрофону"
                else -> "Распознавание недоступно на этом устройстве"
            }
        )
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (text.isNotBlank()) onFinal(text)
        if (!stopped && restartOnResults) listen()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (text.isNotBlank()) onPartial(text)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
