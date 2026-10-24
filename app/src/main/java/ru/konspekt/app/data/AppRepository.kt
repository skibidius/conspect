package ru.konspekt.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("konspekt_local", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadSettings(): UserSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return UserSettings()
        return runCatching {
            val json = JSONObject(raw)
            UserSettings(
                userName = json.optString("userName", "Аня").ifBlank { "Аня" },
                themeMode = when (json.optString("themeMode")) {
                    "LIGHT" -> ThemeMode.LIGHT
                    "DARK" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
                palette = when (json.optString("palette")) {
                    "SKY" -> Palette.SKY
                    "LAVENDER" -> Palette.LAVENDER
                    "CLAY" -> Palette.CLAY
                    else -> Palette.SAGE
                },
                dynamicColor = json.optBoolean("dynamicColor", true),
                filterTangents = json.optBoolean("filterTangents", true),
                aiMode = when (json.optString("aiMode")) {
                    "DEEPSEEK" -> AiMode.DEEPSEEK
                    else -> AiMode.LOCAL
                },
                speechEngine = when (json.optString("speechEngine")) {
                    "SHERPA", "VOSK" -> SpeechEngine.SHERPA
                    else -> SpeechEngine.SYSTEM
                }
            ).let { settings ->
                var engine = settings.speechEngine
                // One-time migration: system recognition becomes the default
                // for existing installs that never explicitly chose an engine.
                if (!prefs.getBoolean(KEY_ENGINE_MIGRATED, false)) {
                    engine = SpeechEngine.SYSTEM
                    prefs.edit().putBoolean(KEY_ENGINE_MIGRATED, true).apply()
                    if (engine != settings.speechEngine) {
                        prefs.edit()
                            .putString(
                                KEY_SETTINGS,
                                gson.toJson(settings.copy(speechEngine = engine))
                            )
                            .apply()
                    }
                }
                settings.copy(speechEngine = engine)
            }
        }.getOrDefault(UserSettings())
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

    fun loadTrash(): List<Note> {
        val raw = prefs.getString(KEY_TRASH, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson<List<Note>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveTrash(notes: List<Note>) {
        prefs.edit().putString(KEY_TRASH, gson.toJson(notes)).apply()
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
        const val KEY_TRASH = "trash"
        const val KEY_ENGINE_MIGRATED = "speech_engine_migrated_v2"
    }
}
