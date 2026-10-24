package ru.konspekt.app.recording

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Offline GigaAM (sherpa-onnx) capture. The model is offline CTC, so audio is
 * split into short chunks with an energy-based silence detector and decoded on
 * a worker thread; the microphone keeps capturing while a chunk is recognized.
 */
class SherpaSpeechCapture(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onModelProgress: (Float) -> Unit = {}
) : SpeechCapture {
    private val main = Handler(Looper.getMainLooper())
    private val queue = LinkedBlockingQueue<FloatArray>()
    private var audioRecord: AudioRecord? = null
    private var loaderThread: Thread? = null
    private var captureThread: Thread? = null
    private var workerThread: Thread? = null
    private var recognizer: OfflineRecognizer? = null

    @Volatile
    private var running = false

    override fun start() {
        if (running) return
        running = true
        loaderThread = Thread {
            try {
                post { onStatus("Загружаем офлайн-модель…") }
                val ok = kotlinx.coroutines.runBlocking {
                    SherpaModelManager.ensureDownloaded(context) { p ->
                        post {
                            onModelProgress(p)
                            onStatus("Загрузка модели… ${(p * 100).toInt()}%")
                        }
                    }
                }
                if (!running) return@Thread
                if (!ok) {
                    post { onError("Не удалось скачать офлайн-модель") }
                    running = false
                    return@Thread
                }
                val rec = kotlinx.coroutines.runBlocking {
                    SherpaModelManager.ensureRecognizer(context)
                }
                if (!running) return@Thread
                if (rec == null) {
                    post { onError("Не удалось загрузить офлайн-модель") }
                    running = false
                    return@Thread
                }
                recognizer = rec
                startCapture(rec)
            } catch (e: Throwable) {
                post { onError("Ошибка офлайн-распознавания: ${e.message}") }
                running = false
            }
        }.apply { start() }
    }

    private fun startCapture(rec: OfflineRecognizer) {
        val record = openMicrophone() ?: run {
            post { onError("Микрофон недоступен") }
            running = false
            return
        }
        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Throwable) {
            runCatching { record.release() }
            audioRecord = null
            post { onError("Микрофон недоступен") }
            running = false
            return
        }

        workerThread = Thread {
            while (true) {
                val chunk = try {
                    queue.take()
                } catch (e: InterruptedException) {
                    break
                }
                if (chunk.isEmpty()) break
                try {
                    post { onStatus("Распознаём…") }
                    val stream = rec.createStream()
                    stream.acceptWaveform(chunk, SAMPLE_RATE)
                    rec.decode(stream)
                    val text = rec.getResult(stream).text.trim()
                    stream.release()
                    if (text.isNotBlank()) {
                        post { onFinal(text) }
                    }
                    if (running) post { onStatus("Слушаю микрофон…") }
                } catch (e: Throwable) {
                    post { onError("Ошибка распознавания: ${e.message}") }
                }
            }
        }.apply { start() }

        captureThread = Thread {
            val buffer = ShortArray(1600)
            val chunk = FloatArray(MAX_CHUNK_SAMPLES)
            var chunkLength = 0
            var lastVoiceAt = SystemClock.elapsedRealtime()
            var hasSpeech = false
            var lastAmplitudeAt = 0L

            fun flush() {
                if (chunkLength < MIN_CHUNK_SAMPLES) {
                    chunkLength = 0
                    hasSpeech = false
                    return
                }
                queue.put(chunk.copyOf(chunkLength))
                chunkLength = 0
                hasSpeech = false
            }

            while (running) {
                val read = runCatching { record.read(buffer, 0, buffer.size) }.getOrDefault(-1)
                if (read <= 0) {
                    if (!running) break
                    continue
                }
                var sum = 0.0
                var peak = 0.0
                for (i in 0 until read) {
                    val sample = buffer[i].toDouble()
                    sum += sample * sample
                    if (abs(sample) > peak) peak = abs(sample)
                }
                val rms = sqrt(sum / read) / 32768.0
                val now = SystemClock.elapsedRealtime()
                if (now - lastAmplitudeAt >= 80) {
                    lastAmplitudeAt = now
                    val level = ((20.0 * kotlin.math.log10(rms.coerceAtLeast(1e-7)) + 60.0) / 50.0)
                        .toFloat().coerceIn(0f, 1f)
                    post { onAmplitude(level) }
                }
                if (rms > 0.015 || peak > 3000) {
                    lastVoiceAt = now
                    hasSpeech = true
                }

                for (i in 0 until read) {
                    if (chunkLength >= chunk.size) break
                    chunk[chunkLength++] = buffer[i] / 32768.0f
                }

                val silentFor = now - lastVoiceAt
                if (chunkLength >= MAX_CHUNK_SAMPLES ||
                    (hasSpeech && silentFor >= SILENCE_MS && chunkLength >= MIN_CHUNK_SAMPLES)
                ) {
                    flush()
                }
            }
            flush()
        }.apply { start() }

        post { onStatus("Слушаю микрофон…") }
    }

    private fun openMicrophone(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(if (minBuffer > 0) minBuffer else 0, 8192)
        val sources = listOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        )
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
            if (record.state == AudioRecord.STATE_INITIALIZED) return record
            runCatching { record.release() }
        }
        return null
    }

    override fun stop() {
        if (!running && captureThread == null && workerThread == null) return
        running = false
        audioRecord?.let { runCatching { it.stop() } }
        loaderThread?.let { runCatching { it.join(1_500) } }
        captureThread?.let { runCatching { it.join(1_500) } }
        queue.offer(FloatArray(0))
        workerThread?.let { runCatching { it.join(5_000) } }
        loaderThread = null
        captureThread = null
        workerThread = null
        audioRecord?.let { runCatching { it.release() } }
        audioRecord = null
        // The cached recognizer stays loaded for the next recording.
    }

    private fun post(block: () -> Unit) {
        main.post(block)
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val MAX_CHUNK_SAMPLES = SAMPLE_RATE * 8
        const val MIN_CHUNK_SAMPLES = SAMPLE_RATE * 1
        const val SILENCE_MS = 900L
    }
}
