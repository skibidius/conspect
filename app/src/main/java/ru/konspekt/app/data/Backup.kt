package ru.konspekt.app.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Portable backup of everything the user creates. API keys and downloaded
 * model binaries are intentionally not included.
 */
data class AppBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: UserSettings = UserSettings(),
    val subjects: List<Subject> = emptyList(),
    val notes: List<Note> = emptyList(),
    val trash: List<Note> = emptyList()
)

object BackupManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(backup: AppBackup): String = gson.toJson(backup)

    fun fromJson(raw: String): AppBackup? = runCatching {
        gson.fromJson(raw, AppBackup::class.java)
    }.getOrNull()
}
