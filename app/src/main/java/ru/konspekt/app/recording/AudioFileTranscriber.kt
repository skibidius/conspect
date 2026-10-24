package ru.konspekt.app.recording

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

enum class AudioTranscriptionStage {
    MODEL,
    PREPARING,
    RECOGNIZING,
    FINALIZING
}

data class AudioTranscriptionProgress(
    val stage: AudioTranscriptionStage,
    val fraction: Float,
    val stageFraction: Float = 0f,
    val processedSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val etaSeconds: Int? = null,
    val chunksDone: Int = 0,
    val chunksTotal: Int = 0
)

data class AudioTranscription(
    val lines: List<String>,
    val durationSeconds: Int
)

/**
 * Decodes any audio file supported by Android's MediaCodec into 16 kHz mono
 * PCM and transcribes it with the offline GigaAM (sherpa-onnx) model.
 *
 * The decoder and recognizer both run on a background dispatcher. Progress is
 * based on the amount of audio actually processed, not on timer ticks.
 */
object AudioFileTranscriber {
    suspend fun transcribe(
        context: Context,
        uri: Uri,
        onProgress: (AudioTranscriptionProgress) -> Unit
    ): AudioTranscription = withContext(Dispatchers.IO) {
        val main = Handler(Looper.getMainLooper())
        var lastEmitAt = 0L

        fun emit(update: AudioTranscriptionProgress) {
            main.post { onProgress(update) }
        }

        fun emitThrottled(update: AudioTranscriptionProgress, force: Boolean = false) {
            val now = SystemClock.elapsedRealtime()
            if (force || now - lastEmitAt >= 250) {
                lastEmitAt = now
                emit(update)
            }
        }

        emit(
            AudioTranscriptionProgress(
                stage = AudioTranscriptionStage.MODEL,
                fraction = 0f
            )
        )
        val downloaded = SherpaModelManager.ensureDownloaded(context) { p ->
            emit(
                AudioTranscriptionProgress(
                    stage = AudioTranscriptionStage.MODEL,
                    fraction = p * 0.02f,
                    stageFraction = p
                )
            )
        }
        if (!downloaded) error("Не удалось скачать модель распознавания")
        val recognizer = SherpaModelManager.ensureRecognizer(context)
            ?: error("Не удалось загрузить модель распознавания")
        emit(
            AudioTranscriptionProgress(
                stage = AudioTranscriptionStage.PREPARING,
                fraction = 0.02f,
                stageFraction = 1f
            )
        )

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)
            } ?: error("Не удалось открыть аудиофайл")

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: error("В файле нет аудиодорожки")
            extractor.selectTrack(trackIndex)

            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: error("Неизвестный формат аудио")
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val totalSeconds = if (durationUs > 0L) {
                (durationUs / 1_000_000L).toInt().coerceAtLeast(1)
            } else {
                0
            }
            val totalChunks = if (totalSeconds > 0) {
                ceil(totalSeconds.toDouble() / CHUNK_SECONDS).toInt()
            } else {
                0
            }

            runCatching {
                inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val rec: OfflineRecognizer = recognizer
            val pending = FloatArray(SAMPLE_RATE * CHUNK_SECONDS)
            var pendingLength = 0
            var decodedSamples = 0L
            var chunksDone = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                SAMPLE_RATE
            }
            var channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            val resampler = PcmResampler(SAMPLE_RATE)
            val lines = mutableListOf<String>()
            val recognitionStartedAt = SystemClock.elapsedRealtime()

            fun publishRecognition(force: Boolean = false) {
                val processed = (decodedSamples / SAMPLE_RATE)
                    .toInt()
                    .coerceAtLeast(0)
                    .let { if (totalSeconds > 0) it.coerceAtMost(totalSeconds) else it }
                val stageFraction = if (totalSeconds > 0) {
                    (processed.toFloat() / totalSeconds).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val elapsedSeconds = ((SystemClock.elapsedRealtime() - recognitionStartedAt) / 1000f)
                    .coerceAtLeast(0f)
                val eta = if (
                    totalSeconds > 0 &&
                    processed >= 15 &&
                    elapsedSeconds >= 3f
                ) {
                    val speed = processed / elapsedSeconds
                    if (speed > 0.05f) {
                        ((totalSeconds - processed) / speed).roundToInt().coerceAtLeast(0)
                    } else {
                        null
                    }
                } else {
                    null
                }
                emitThrottled(
                    AudioTranscriptionProgress(
                        stage = AudioTranscriptionStage.RECOGNIZING,
                        fraction = 0.02f + stageFraction * 0.97f,
                        stageFraction = stageFraction,
                        processedSeconds = processed,
                        totalSeconds = totalSeconds,
                        etaSeconds = eta,
                        chunksDone = chunksDone,
                        chunksTotal = totalChunks
                    ),
                    force = force
                )
            }

            emit(
                AudioTranscriptionProgress(
                    stage = AudioTranscriptionStage.PREPARING,
                    fraction = 0.02f,
                    stageFraction = 1f,
                    totalSeconds = totalSeconds,
                    chunksTotal = totalChunks
                )
            )

            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        val size = if (inputBuffer != null) extractor.readSampleData(inputBuffer, 0) else -1
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex, 0, size, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        if (outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                        resampler.reset(sampleRate, channels)
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outputIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                        if (bufferInfo.size > 0 && outputBuffer != null) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val pcm = resampler.process(
                                buffer = outputBuffer,
                                size = bufferInfo.size,
                                sampleRate = sampleRate,
                                channels = channels,
                                pcmEncoding = pcmEncoding
                            )
                            if (pcm.length > 0) {
                                decodedSamples += pcm.length
                                var offset = 0
                                while (offset < pcm.length) {
                                    pending[pendingLength++] = pcm.samples[offset] / 32768.0f
                                    offset += 1
                                    if (pendingLength == pending.size) {
                                        val text = recognizeChunk(rec, pending, pendingLength)
                                        if (text.isNotBlank()) lines.add(text)
                                        pendingLength = 0
                                        chunksDone += 1
                                    }
                                }
                                publishRecognition()
                            }
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (pendingLength > 0) {
                val tail = recognizeChunk(rec, pending, pendingLength)
                if (tail.isNotBlank()) lines.add(tail)
                chunksDone += 1
            }
            val duration = if (totalSeconds > 0) {
                totalSeconds
            } else {
                (decodedSamples / SAMPLE_RATE).toInt().coerceAtLeast(1)
            }
            emit(
                AudioTranscriptionProgress(
                    stage = AudioTranscriptionStage.FINALIZING,
                    fraction = 0.99f,
                    stageFraction = 1f,
                    processedSeconds = duration,
                    totalSeconds = duration,
                    chunksDone = chunksDone,
                    chunksTotal = if (totalChunks > 0) totalChunks else chunksDone
                )
            )
            AudioTranscription(lines = lines.distinct(), durationSeconds = duration)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun recognizeChunk(
        recognizer: OfflineRecognizer,
        samples: FloatArray,
        length: Int
    ): String {
        val stream = recognizer.createStream()
        return try {
            val input = if (length == samples.size) samples else samples.copyOf(length)
            stream.acceptWaveform(input, SAMPLE_RATE)
            recognizer.decode(stream)
            recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    private class PcmResampler(private val targetRate: Int) {
        private var sourcePosition = 0.0
        private var currentRate = 0
        private var currentChannels = 0

        fun reset(rate: Int, channels: Int) {
            currentRate = rate
            currentChannels = channels.coerceAtLeast(1)
            sourcePosition = 0.0
        }

        fun process(
            buffer: ByteBuffer,
            size: Int,
            sampleRate: Int,
            channels: Int,
            pcmEncoding: Int
        ): PcmChunk {
            if (sampleRate != currentRate || channels != currentChannels) {
                reset(sampleRate, channels)
            }
            val frames = when (pcmEncoding) {
                AudioFormat.ENCODING_PCM_FLOAT -> size / 4 / currentChannels
                else -> size / 2 / currentChannels
            }
            if (frames <= 0) return PcmChunk.EMPTY

            val mono = FloatArray(frames)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                val floats = FloatArray(frames * currentChannels)
                buffer.asFloatBuffer().get(floats)
                for (frame in 0 until frames) {
                    var sum = 0f
                    val base = frame * currentChannels
                    for (ch in 0 until currentChannels) {
                        sum += floats[base + ch]
                    }
                    mono[frame] = sum / currentChannels
                }
            } else {
                val shorts = ShortArray(frames * currentChannels)
                buffer.asShortBuffer().get(shorts)
                for (frame in 0 until frames) {
                    var sum = 0f
                    val base = frame * currentChannels
                    for (ch in 0 until currentChannels) {
                        sum += shorts[base + ch].toFloat()
                    }
                    mono[frame] = sum / currentChannels
                }
            }

            val ratio = currentRate.toDouble() / targetRate.toDouble()
            val estimated = ((frames / ratio) + 8).toInt().coerceAtLeast(1)
            val out = ShortArray(estimated)
            var outCount = 0
            while (sourcePosition + 1 < frames && outCount < out.size) {
                val index = sourcePosition.toInt()
                val fraction = sourcePosition - index
                val value = mono[index] * (1.0 - fraction) + mono[index + 1] * fraction
                out[outCount++] = value.coerceIn(-32768.0, 32767.0).roundToInt().toShort()
                sourcePosition += ratio
            }
            sourcePosition = (sourcePosition - frames).coerceAtLeast(0.0)
            if (outCount == 0) return PcmChunk.EMPTY
            return PcmChunk(out, outCount)
        }
    }

    private data class PcmChunk(val samples: ShortArray, val length: Int) {
        companion object {
            val EMPTY = PcmChunk(ShortArray(0), 0)
        }
    }

    private const val SAMPLE_RATE = 16_000
    private const val CHUNK_SECONDS = 30
}
