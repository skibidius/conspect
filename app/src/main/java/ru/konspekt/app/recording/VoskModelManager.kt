package ru.konspekt.app.recording

import android.content.Context
import org.vosk.Model
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads the Russian Vosk model once into app-private storage and keeps it
 * cached in memory. The APK stays small; the model is fetched on first use.
 */
object VoskModelManager {
    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip"
    private const val DIR_NAME = "vosk-model-ru"

    private val lock = Any()
    @Volatile
    private var model: Model? = null

    fun isReady(context: Context): Boolean =
        File(context.filesDir, DIR_NAME).resolve("am").isDirectory

    suspend fun ensure(context: Context, onProgress: (Float) -> Unit = {}): Model? =
        withContext(Dispatchers.IO) {
            model?.let { return@withContext it }
            synchronized(lock) {
                model?.let { return@withContext it }
                try {
                    val dir = File(context.filesDir, DIR_NAME)
                    if (!isReady(context)) {
                        dir.deleteRecursively()
                        val zip = File(context.cacheDir, "vosk-model-ru.zip")
                        download(zip, onProgress)
                        unzip(zip, dir)
                        zip.delete()
                    }
                    val loaded = Model(dir.absolutePath)
                    model = loaded
                    loaded
                } catch (e: Exception) {
                    null
                }
            }
        }

    private fun download(dest: File, onProgress: (Float) -> Unit) {
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            requestMethod = "GET"
        }
        try {
            connection.connect()
            val total = connection.contentLength.toLong()
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = input.read(buffer)
                    var downloaded = 0L
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total.toFloat())
                        read = input.read(buffer)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(zip: File, dest: File) {
        dest.mkdirs()
        ZipInputStream(FileInputStream(zip)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val parts = entry.name.split('/').filter { it.isNotBlank() }
                if (parts.isEmpty()) {
                    input.closeEntry()
                    continue
                }
                // Strip the single top-level folder shipped inside the zip.
                val relative = if (parts.size > 1) {
                    parts.drop(1).joinToString("/")
                } else {
                    parts.joinToString("/")
                }
                val target = File(dest, relative)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = input.read(buffer)
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            read = input.read(buffer)
                        }
                    }
                }
                input.closeEntry()
            }
        }
    }
}
