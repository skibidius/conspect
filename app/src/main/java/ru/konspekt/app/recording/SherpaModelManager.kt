package ru.konspekt.app.recording

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ModelUpdateCheck(
    val available: Boolean,
    val remoteBytes: Long? = null,
    val error: String? = null
)

/**
 * Downloads the compact GigaAM v2 Russian CTC model for sherpa-onnx.
 * Files are stored in app-private storage; the APK stays small.
 */
object SherpaModelManager {
    private const val BASE =
        "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-ctc-giga-am-v2-russian-2025-04-19/resolve/main"
    private const val MODEL_URL = "$BASE/model.int8.onnx"
    private const val TOKENS_URL = "$BASE/tokens.txt"
    private const val LICENSE_URL = "$BASE/LICENSE"
    private const val DIR_NAME = "sherpa-gigaam-v2-ru"
    private const val MODEL_NAME = "model.int8.onnx"
    private const val TOKENS_NAME = "tokens.txt"
    private const val MIN_MODEL_BYTES = 100L * 1024 * 1024
    private const val META_PREFS = "sherpa_model_meta"
    private const val KEY_SIGNATURE = "remote_signature"

    private val lock = Any()
    @Volatile
    private var recognizer: OfflineRecognizer? = null

    fun modelDir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun isReady(context: Context): Boolean {
        val dir = modelDir(context)
        return File(dir, MODEL_NAME).length() > MIN_MODEL_BYTES &&
            File(dir, TOKENS_NAME).isFile
    }

    fun sizeBytes(context: Context): Long =
        modelDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    suspend fun checkForUpdate(context: Context): ModelUpdateCheck =
        withContext(Dispatchers.IO) {
            if (!isReady(context)) {
                return@withContext ModelUpdateCheck(
                    available = false,
                    error = "Сначала скачайте модель"
                )
            }
            try {
                val remote = fetchRemoteInfo()
                val localBytes = File(modelDir(context), MODEL_NAME).length()
                val prefs = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
                val stored = prefs.getString(KEY_SIGNATURE, null)
                val available = if (stored != null) {
                    stored != remote.signature
                } else {
                    remote.bytes > 0L && localBytes != remote.bytes
                }
                if (stored == null || !available) {
                    prefs.edit().putString(KEY_SIGNATURE, remote.signature).apply()
                }
                ModelUpdateCheck(available = available, remoteBytes = remote.bytes)
            } catch (e: Throwable) {
                ModelUpdateCheck(
                    available = false,
                    error = e.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось проверить обновления"
                )
            }
        }

    suspend fun ensureDownloaded(
        context: Context,
        force: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            try {
                val dir = modelDir(context)
                if (force) {
                    recognizer?.let { runCatching { it.release() } }
                    recognizer = null
                    dir.deleteRecursively()
                }
                dir.mkdirs()
                val model = File(dir, MODEL_NAME)
                val tokens = File(dir, TOKENS_NAME)
                var downloadedModel = false
                if (model.length() <= MIN_MODEL_BYTES) {
                    download(MODEL_URL, model) { p -> onProgress(p * 0.98f) }
                    downloadedModel = true
                }
                if (!tokens.isFile) {
                    download(TOKENS_URL, tokens) { }
                    downloadedModel = true
                }
                if (downloadedModel) {
                    context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
                        .edit().remove(KEY_SIGNATURE).apply()
                }
                val license = File(dir, "LICENSE")
                if (!license.isFile) {
                    runCatching { download(LICENSE_URL, license) { } }
                }
                onProgress(1f)
                isReady(context)
            } catch (e: Throwable) {
                false
            }
        }
    }

    suspend fun ensureRecognizer(context: Context): OfflineRecognizer? =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                recognizer?.let { return@withContext it }
                if (!isReady(context)) return@withContext null
                try {
                    val dir = modelDir(context)
                    val config = OfflineRecognizerConfig(
                        modelConfig = OfflineModelConfig(
                            nemo = OfflineNemoEncDecCtcModelConfig(
                                model = File(dir, MODEL_NAME).absolutePath
                            ),
                            tokens = File(dir, TOKENS_NAME).absolutePath,
                            numThreads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
                            provider = "cpu",
                            debug = false
                        )
                    )
                    OfflineRecognizer(assetManager = null, config = config).also {
                        recognizer = it
                    }
                } catch (e: Throwable) {
                    null
                }
            }
        }

    fun delete(context: Context) {
        synchronized(lock) {
            recognizer?.let { runCatching { it.release() } }
            recognizer = null
            modelDir(context).deleteRecursively()
            context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_SIGNATURE).apply()
        }
    }

    private data class RemoteInfo(val bytes: Long, val signature: String)

    private fun fetchRemoteInfo(): RemoteInfo {
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Range", "bytes=0-0")
            setRequestProperty("Accept-Encoding", "identity")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("Сервер модели вернул ошибку $status")
            val contentRange = connection.getHeaderField("Content-Range")
            val rangeBytes = contentRange
                ?.substringAfterLast('/')
                ?.toLongOrNull()
            val length = rangeBytes
                ?: connection.contentLengthLong.takeIf { it > 0L }
                ?: error("Сервер не сообщил размер модели")
            val etag = connection.getHeaderField("ETag")?.trim()?.takeIf { it.isNotBlank() }
            val lastModified = connection.getHeaderField("Last-Modified")?.trim()
                ?.takeIf { it.isNotBlank() }
            val signature = etag ?: "${lastModified ?: "size"}|$length"
            connection.inputStream?.use { it.read() }
            return RemoteInfo(bytes = length, signature = signature)
        } finally {
            connection.disconnect()
        }
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val temp = File(dest.parentFile, "${dest.name}.part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 180_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var read = input.read(buffer)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress((downloaded.toDouble() / total).toFloat())
                            }
                        }
                        read = input.read(buffer)
                    }
                }
            }
            if (dest.exists()) dest.delete()
            if (!temp.renameTo(dest)) error("Не удалось сохранить файл модели")
        } finally {
            connection.disconnect()
            temp.delete()
        }
    }
}
