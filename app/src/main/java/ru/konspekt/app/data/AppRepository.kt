package ru.konspekt.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("konspekt_local", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadSettings(): UserSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return UserSettings()
        return runCatching { gson.fromJson(raw, UserSettings::class.java) }.getOrDefault(UserSettings())
    }

    fun saveSettings(settings: UserSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun loadNotes(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson<List<Note>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveNotes(notes: List<Note>) {
        prefs.edit().putString(KEY_NOTES, gson.toJson(notes)).apply()
    }

    fun loadSubjects(): List<Subject> {
        val raw = prefs.getString(KEY_SUBJECTS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Subject>>() {}.type
            gson.fromJson<List<Subject>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveSubjects(subjects: List<Subject>) {
        prefs.edit().putString(KEY_SUBJECTS, gson.toJson(subjects)).apply()
    }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_NOTES = "notes"
        const val KEY_SUBJECTS = "subjects"
    }
}
