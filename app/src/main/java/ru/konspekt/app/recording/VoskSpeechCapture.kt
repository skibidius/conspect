package ru.konspekt.app.recording

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
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
) : SpeechCapture {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var sessionId = 0

    override fun start() {
        if (running) return
        running = true
        val session = ++sessionId
        thread = Thread {
            try {
                post { onStatus("Загрузка офлайн-модели…") }
                var lastPercent = -1
                val model = runBlocking {
                    VoskModelManager.ensure(context) { progress ->
                        val percent = (progress * 100).toInt()
                        if (percent != lastPercent && running && sessionId == session) {
                            lastPercent = percent
                            post {
                                if (running && sessionId == session) {
                                    onModelProgress(progress)
                                    onStatus("Загрузка модели… $percent%")
                                }
                            }
                        }
                    }
                }
                if (!running || sessionId != session) return@Thread
                if (model == null) {
                    post { if (sessionId == session) onError("Не удалось загрузить модель распознавания") }
                    running = false
                    return@Thread
                }
                startRecognition(model, session)
            } catch (e: Exception) {
                if (sessionId == session) {
                    post { onError("Ошибка распознавания: ${e.message}") }
                }
                running = false
            }
        }.apply { start() }
    }

    private fun startRecognition(model: Model, session: Int) {
        if (!running || sessionId != session) return

        val record = openMicrophone() ?: run {
            post { if (sessionId == session) onError("Микрофон недоступен") }
            running = false
            return
        }
        val recognizerInstance = try {
            Recognizer(model, SAMPLE_RATE.toFloat()).apply {
                setPartialWords(true)
            }
        } catch (e: Exception) {
            record.release()
            post { if (sessionId == session) onError("Не удалось запустить распознавание") }
            running = false
            return
        }

        if (!running || sessionId != session) {
            runCatching { recognizerInstance.close() }
            record.release()
            return
        }

        recognizer = recognizerInstance
        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            cleanup()
            post { if (sessionId == session) onError("Микрофон недоступен") }
            running = false
            return
        }
        post { if (running && sessionId == session) onStatus("Готово") }
        readLoop(record, recognizerInstance, session)
    }

    /**
     * VOICE_RECOGNITION applies aggressive noise suppression on some phones,
     * which hurts Vosk. Try raw/unprocessed audio first and fall back to the
     * commonly available sources.
     */
    private fun openMicrophone(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(if (minBuffer > 0) minBuffer else 0, 8192)
        val sources = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.MIC)
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        }.distinct()

        for (source in sources) {
            val record = runCatching {
                AudioRecord(
                    source,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }.getOrNull() ?: continue
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                Log.d(TAG, "Using audio source $source")
                return record
            }
            runCatching { record.release() }
        }
        return null
    }

    private fun readLoop(record: AudioRecord, rec: Recognizer, session: Int) {
        val bytes = ByteArray(8192)
        var lastAmplitudeAt = 0L
        var lastPartial = ""

        while (running && sessionId == session) {
            val read = runCatching { record.read(bytes, 0, bytes.size) }.getOrDefault(-1)
            if (read <= 0) {
                if (!running || sessionId != session) break
                continue
            }

            var sum = 0.0
            var peak = 0.0
            var i = 0
            while (i + 1 < read) {
                val sample = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xff))
                    .toShort()
                    .toDouble()
                sum += sample * sample
                val absolute = abs(sample)
                if (absolute > peak) peak = absolute
                i += 2
            }
            val samples = read / 2
            val now = SystemClock.elapsedRealtime()
            if (samples > 0 && now - lastAmplitudeAt >= 80) {
                lastAmplitudeAt = now
                val rms = sqrt(sum / samples) / 32768.0
                val peakLevel = peak / 32768.0
                val level = max(
                    decibelLevel(rms),
                    decibelLevel(peakLevel) * 0.7f
                ).coerceIn(0f, 1f)
                post { if (running && sessionId == session) onAmplitude(level) }
            }

            val isFinal = runCatching { rec.acceptWaveForm(bytes, read) }.getOrDefault(false)
            if (isFinal) {
                lastPartial = ""
                val final = runCatching { rec.finalResult }.getOrNull()
                val text = final?.let { json ->
                    runCatching { JSONObject(json).optString("text") }.getOrDefault("")
                }.orEmpty()
                if (text.isNotBlank()) {
                    post { if (sessionId == session) onFinal(text.trim()) }
                }
            } else {
                val partial = runCatching { rec.partialResult }.getOrNull()
                val text = partial?.let { json ->
                    runCatching { JSONObject(json).optString("partial") }.getOrDefault("")
                }.orEmpty().trim()
                if (text != lastPartial) {
                    lastPartial = text
                    post { if (running && sessionId == session) onPartial(text) }
                }
            }
        }
    }

    /**
     * Maps microphone level to a useful 0..1 range. Linear RMS is almost
     * invisible for normal speech; -60 dB..-10 dB gives a responsive meter.
     */
    private fun decibelLevel(value: Double): Float {
        if (value <= 0.0) return 0f
        val db = 20.0 * log10(value)
        return ((db + 60.0) / 50.0).toFloat().coerceIn(0f, 1f)
    }

    override fun stop() {
        val worker = thread
        thread = null
        running = false
        sessionId += 1

        // Stop the recorder first: this unblocks AudioRecord.read() and lets
        // the worker exit before we close the native Vosk recognizer.
        audioRecord?.let { runCatching { it.stop() } }
        if (worker != null && worker != Thread.currentThread()) {
            runCatching { worker.join(1_500) }
        }
        cleanup()
    }

    private fun cleanup() {
        recognizer?.let { runCatching { it.close() } }
        recognizer = null
        audioRecord?.let { runCatching { it.release() } }
        audioRecord = null
    }

    private fun post(block: () -> Unit) {
        main.post(block)
    }

    private companion object {
        const val TAG = "VoskSpeechCapture"
        const val SAMPLE_RATE = 16000
    }
}
