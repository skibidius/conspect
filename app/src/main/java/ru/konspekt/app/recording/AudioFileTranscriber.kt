package ru.konspekt.app.recording

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Recognizer

data class AudioTranscription(
    val lines: List<String>,
    val durationSeconds: Int
)

/**
 * Decodes any audio file supported by Android's MediaCodec into 16 kHz mono
 * PCM and transcribes it with the compact offline Vosk model.
 */
object AudioFileTranscriber {
    suspend fun transcribe(
        context: Context,
        uri: Uri,
        onStatus: (String) -> Unit,
        onProgress: (Float) -> Unit
    ): AudioTranscription = withContext(Dispatchers.IO) {
        val main = Handler(Looper.getMainLooper())
        fun status(text: String) = main.post { onStatus(text) }
        fun progress(value: Float) = main.post { onProgress(value.coerceIn(0f, 1f)) }

        status("Загружаем офлайн-модель…")
        val model = VoskModelManager.ensure(context, VoskModel.COMPACT) { p ->
            progress(p * 0.2f)
        } ?: error("Не удалось загрузить модель распознавания")

        status("Читаем аудио…")
        progress(0.2f)

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var recognizer: Recognizer? = null
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

            runCatching {
                inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }

            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val rec = Recognizer(model, SAMPLE_RATE.toFloat())
            recognizer = rec

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
            var lastProgress = 0f
            var decodedSamples = 0L

            while (!outputDone) {
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
                            if (pcm.isNotEmpty()) {
                                decodedSamples += pcm.size / 2L
                                if (rec.acceptWaveForm(pcm, pcm.size)) {
                                    val text = parseText(rec.finalResult)
                                    if (text.isNotBlank()) lines.add(text)
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                        if (durationUs > 0) {
                            val p = (bufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat())
                                .coerceIn(0f, 1f)
                            val scaled = 0.2f + p * 0.75f
                            if (scaled - lastProgress >= 0.01f) {
                                lastProgress = scaled
                                progress(scaled)
                            }
                        }
                    }
                }
            }

            val tail = parseText(rec.finalResult)
            if (tail.isNotBlank()) lines.add(tail)
            val duration = if (durationUs > 0) {
                (durationUs / 1_000_000L).toInt().coerceAtLeast(1)
            } else {
                (decodedSamples / SAMPLE_RATE).toInt().coerceAtLeast(1)
            }
            progress(1f)
            AudioTranscription(lines = lines.distinct(), durationSeconds = duration)
        } finally {
            runCatching { recognizer?.close() }
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun parseText(json: String?): String {
        if (json.isNullOrBlank()) return ""
        return runCatching { JSONObject(json).optString("text").trim() }.getOrDefault("")
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
        ): ByteArray {
            if (sampleRate != currentRate || channels != currentChannels) {
                reset(sampleRate, channels)
            }
            val frames = when (pcmEncoding) {
                AudioFormat.ENCODING_PCM_FLOAT -> size / 4 / currentChannels
                else -> size / 2 / currentChannels
            }
            if (frames <= 0) return ByteArray(0)

            val mono = DoubleArray(frames)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                val floats = FloatArray(frames * currentChannels)
                buffer.asFloatBuffer().get(floats)
                for (frame in 0 until frames) {
                    var sum = 0.0
                    for (ch in 0 until currentChannels) {
                        sum += floats[frame * currentChannels + ch]
                    }
                    mono[frame] = sum / currentChannels
                }
            } else {
                val shorts = ShortArray(frames * currentChannels)
                buffer.asShortBuffer().get(shorts)
                for (frame in 0 until frames) {
                    var sum = 0.0
                    for (ch in 0 until currentChannels) {
                        sum += shorts[frame * currentChannels + ch].toDouble()
                    }
                    mono[frame] = sum / currentChannels
                }
            }

            val ratio = currentRate.toDouble() / targetRate.toDouble()
            val estimated = max(0, ((frames - sourcePosition) / ratio).toInt() + 1)
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
            if (outCount == 0) return ByteArray(0)
            return ByteBuffer.allocate(outCount * 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .also { it.asShortBuffer().put(out, 0, outCount) }
                .array()
        }
    }

    private const val SAMPLE_RATE = 16_000
}
