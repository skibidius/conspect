package ru.konspekt.app.recording

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import kotlin.math.sqrt

/**
 * Fully offline speech recognition powered by Vosk. Audio is read from the
 * microphone, fed into the recognizer, and only text leaves the device.
 */
class VoskSpeechCapture(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onModelProgress: (Float) -> Unit = {}
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            try {
                post { onStatus("Загрузка офлайн-модели…") }
                var lastPercent = -1
                val model = runBlocking {
                    VoskModelManager.ensure(context) { progress ->
                        val percent = (progress * 100).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            post {
                                onModelProgress(progress)
                                onStatus("Загрузка модели… $percent%")
                            }
                        }
                    }
                }
                if (model == null) {
                    post { onError("Не удалось загрузить модель распознавания") }
                    running = false
                    return@Thread
                }
                startRecognition(model)
            } catch (e: Exception) {
                post { onError("Ошибка распознавания: ${e.message}") }
                running = false
            }
        }.apply { start() }
    }

    private fun startRecognition(model: Model) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, 4096)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            post { onError("Микрофон недоступен") }
            running = false
            return
        }
        val recognizerInstance = Recognizer(model, SAMPLE_RATE.toFloat()).apply {
            setPartialWords(true)
        }
        recognizer = recognizerInstance
        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            record.release()
            recognizerInstance.close()
            post { onError("Микрофон недоступен") }
            running = false
            return
        }
        post { onStatus("Готово") }
        readLoop(record, recognizerInstance)
    }

    private fun readLoop(record: AudioRecord, rec: Recognizer) {
        val bytes = ByteArray(4096)
        var lastAmplitudeAt = 0L
        while (running) {
            val read = record.read(bytes, 0, bytes.size)
            if (read <= 0) continue

            var sum = 0.0
            var i = 0
            while (i + 1 < read) {
                val sample = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xff)).toShort().toDouble()
                sum += sample * sample
                i += 2
            }
            val samples = read / 2
            val now = SystemClock.elapsedRealtime()
            if (samples > 0 && now - lastAmplitudeAt >= 90) {
                val rms = sqrt(sum / samples) / 32768.0
                lastAmplitudeAt = now
                post { onAmplitude(rms.toFloat().coerceIn(0f, 1f)) }
            }

            if (rec.acceptWaveForm(bytes, read)) {
                rec.partialResult?.takeIf { it.isNotBlank() }?.let { json ->
                    val text = runCatching { JSONObject(json).optString("partial") }.getOrDefault("")
                    if (text.isNotBlank()) post { onPartial(text.trim()) }
                }
                rec.finalResult?.takeIf { it.isNotBlank() }?.let { json ->
                    val text = runCatching { JSONObject(json).optString("text") }.getOrDefault("")
                    if (text.isNotBlank()) post { onFinal(text.trim()) }
                }
            }
        }
    }

    fun stop() {
        running = false
        thread?.let { runCatching { it.interrupt() } }
        thread = null
        recognizer?.let { runCatching { it.close() } }
        recognizer = null
        audioRecord?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        audioRecord = null
    }

    private fun post(block: () -> Unit) {
        main.post(block)
    }

    private companion object {
        const val SAMPLE_RATE = 16000
    }
}
