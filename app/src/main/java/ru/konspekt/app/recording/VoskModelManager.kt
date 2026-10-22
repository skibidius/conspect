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
 * Russian Vosk models available to the app. The compact model is the default;
 * the accurate model is optional and needs much more storage and RAM.
 */
enum class VoskModel(
    val id: String,
    val title: String,
    val sizeLabel: String,
    val description: String,
    val url: String,
    val dirName: String,
    val approxBytes: Long
) {
    COMPACT(
        id = "compact",
        title = "Компактная",
        sizeLabel = "45 МБ",
        description = "Быстрая и лёгкая. Подходит для большинства лекций.",
        url = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip",
        dirName = "vosk-model-ru",
        approxBytes = 45L * 1024 * 1024
    ),
    ACCURATE(
        id = "accurate",
        title = "Точная",
        sizeLabel = "1,8 ГБ",
        description = "Заметно точнее. Нужно 6+ ГБ RAM и много свободного места.",
        url = "https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip",
        dirName = "vosk-model-ru-accurate",
        approxBytes = 1_800L * 1024 * 1024
    )
}

/**
 * Downloads a Vosk model into app-private storage and keeps it cached in
 * memory. The APK stays small; models are fetched on demand.
 */
object VoskModelManager {
    private const val PREFS = "konspekt_local"
    private const val KEY_MODEL = "vosk_model_id"

    private val lock = Any()
    @Volatile
    private var loadedModel: Model? = null
    @Volatile
    private var loadedModelId: String? = null

    fun selected(context: Context): VoskModel {
        val id = prefs(context).getString(KEY_MODEL, VoskModel.COMPACT.id)
        return VoskModel.entries.firstOrNull { it.id == id } ?: VoskModel.COMPACT
    }

    fun select(context: Context, model: VoskModel) {
        prefs(context).edit().putString(KEY_MODEL, model.id).apply()
    }

    fun isReady(context: Context, model: VoskModel = selected(context)): Boolean =
        modelDir(context, model).resolve("am").isDirectory

    suspend fun ensure(
        context: Context,
        model: VoskModel = selected(context),
        onProgress: (Float) -> Unit = {}
    ): Model? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (loadedModelId == model.id) {
                loadedModel?.let { return@withContext it }
            }
            loadedModel?.let { runCatching { it.close() } }
            loadedModel = null
            loadedModelId = null
            try {
                val dir = modelDir(context, model)
                if (!isReady(context, model)) prepareFiles(context, model, dir, onProgress)
                val loaded = Model(dir.absolutePath)
                loadedModel = loaded
                loadedModelId = model.id
                loaded
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun downloadModel(
        context: Context,
        model: VoskModel = selected(context),
        force: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        synchronized(lock) {
            try {
                val dir = modelDir(context, model)
                if (force) {
                    if (loadedModelId == model.id) {
                        loadedModel?.let { runCatching { it.close() } }
                        loadedModel = null
                        loadedModelId = null
                    }
                    dir.deleteRecursively()
                }
                if (!isReady(context, model)) prepareFiles(context, model, dir, onProgress)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun delete(context: Context, model: VoskModel = selected(context)) {
        synchronized(lock) {
            if (loadedModelId == model.id) {
                loadedModel?.let { runCatching { it.close() } }
                loadedModel = null
                loadedModelId = null
            }
            modelDir(context, model).deleteRecursively()
        }
    }

    fun sizeBytes(context: Context, model: VoskModel = selected(context)): Long =
        modelDir(context, model).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun modelDir(context: Context, model: VoskModel = selected(context)): File =
        File(context.filesDir, model.dirName)

    private fun prepareFiles(
        context: Context,
        model: VoskModel,
        dir: File,
        onProgress: (Float) -> Unit
    ) {
        dir.deleteRecursively()
        val zip = File(context.cacheDir, "vosk-${model.id}.zip")
        try {
            download(model.url, model.approxBytes, zip, onProgress)
            unzip(zip, dir)
        } finally {
            zip.delete()
        }
    }

    private fun download(
        url: String,
        expectedBytes: Long,
        dest: File,
        onProgress: (Float) -> Unit
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 180_000
            requestMethod = "GET"
        }
        try {
            connection.connect()
            val total = connection.contentLength.toLong().takeIf { it > 0 } ?: expectedBytes
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var read = input.read(buffer)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded.toDouble() / total.toDouble()) * 100).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress((downloaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f))
                            }
                        }
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
                        val buffer = ByteArray(128 * 1024)
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

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
