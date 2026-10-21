package ru.konspekt.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val apiKeyStore = ApiKeyStore(application)
    private val deepSeek = DeepSeekClient()

    var settings by mutableStateOf(repository.loadSettings())
        private set

    var subjects by mutableStateOf(SeedData.subjects + repository.loadSubjects())
        private set

    var notes by mutableStateOf(
        (repository.loadNotes() + SeedData.notes).sortedByDescending { it.createdAt }
    )
        private set

    var hasDeepSeekKey by mutableStateOf(apiKeyStore.hasKey())
        private set

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        settings = transform(settings)
        repository.saveSettings(settings)
    }

    fun subject(id: String): Subject? = subjects.firstOrNull { it.id == id }

    fun note(id: String): Note? = notes.firstOrNull { it.id == id }

    fun notesForSubject(id: String): List<Note> = notes.filter { it.subjectId == id }

    fun notesCount(subjectId: String): Int = notes.count { it.subjectId == subjectId }

    fun totalMinutesForSubject(subjectId: String): Int =
        notes.filter { it.subjectId == subjectId }.sumOf { it.durationMinutes }

    fun addSubject(name: String, tone: SubjectTone, icon: SubjectIcon): Subject {
        val clean = name.trim()
        val subject = Subject(
            id = "custom-${UUID.randomUUID()}",
            name = clean,
            shortName = clean,
            teacher = "Преподаватель",
            room = "—",
            nextClass = "Расписание не задано",
            tone = tone,
            icon = icon
        )
        subjects = subjects + subject
        repository.saveSubjects(subjects.filter { it.id.startsWith("custom-") })
        return subject
    }

    fun saveDeepSeekKey(key: String) {
        apiKeyStore.save(key)
        hasDeepSeekKey = apiKeyStore.hasKey()
    }

    fun clearDeepSeekKey() {
        apiKeyStore.clear()
        hasDeepSeekKey = false
    }

    suspend fun generateNote(
        subjectId: String,
        topic: String,
        transcript: List<String>,
        durationMinutes: Int,
        demo: Boolean = false
    ): NoteGenerationResult {
        val subject = subject(subjectId) ?: subjects.first()
        val now = Date()
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(now)
        val time = SimpleDateFormat("HH:mm", Locale("ru")).format(now)
        val curated = if (demo) curatedNoteFor(subject) else null
        val resolvedTopic = topic.ifBlank {
            transcript.firstOrNull { it.isNotBlank() }?.trim()?.take(64)?.trimEnd()
                ?: "Новая лекция"
        }

        if (curated != null) {
            val note = curated.copy(
                id = "note-${UUID.randomUUID()}",
                subjectId = subject.id,
                title = topic.ifBlank { curated.title },
                dateLabel = date,
                timeLabel = time,
                createdAt = now.time,
                durationMinutes = durationMinutes.coerceAtLeast(1)
            )
            saveGeneratedNote(note)
            return NoteGenerationResult(note.id)
        }

        val cloudKey = apiKeyStore.load()
        if (settings.aiMode == AiMode.DEEPSEEK && !cloudKey.isNullOrBlank()) {
            val cloudNote = runCatching {
                deepSeek.generate(
                    apiKey = cloudKey,
                    subject = subject,
                    topic = topic,
                    transcript = transcript,
                    durationMinutes = durationMinutes,
                    filterTangents = settings.filterTangents
                )
            }.getOrNull()
            if (cloudNote != null) {
                saveGeneratedNote(cloudNote)
                return NoteGenerationResult(cloudNote.id)
            }
            val fallback = LocalLectureStructurer.build(
                subject = subject,
                topic = resolvedTopic,
                transcript = transcript,
                durationMinutes = durationMinutes,
                dateLabel = date,
                timeLabel = time,
                filterTangents = settings.filterTangents
            )
            saveGeneratedNote(fallback)
            return NoteGenerationResult(
                fallback.id,
                "DeepSeek недоступен. Конспект собран локально."
            )
        }

        val local = LocalLectureStructurer.build(
            subject = subject,
            topic = resolvedTopic,
            transcript = transcript,
            durationMinutes = durationMinutes,
            dateLabel = date,
            timeLabel = time,
            filterTangents = settings.filterTangents
        )
        saveGeneratedNote(local)
        return NoteGenerationResult(
            local.id,
            if (settings.aiMode == AiMode.DEEPSEEK) {
                "Добавьте API-ключ DeepSeek — пока конспект собран локально."
            } else {
                null
            }
        )
    }

    private fun saveGeneratedNote(note: Note) {
        notes = (listOf(note) + notes).sortedByDescending { it.createdAt }
        persistLocalNotes()
    }

    fun deleteNote(id: String) {
        notes = notes.filterNot { it.id == id }
        persistLocalNotes()
    }

    private fun persistLocalNotes() {
        repository.saveNotes(notes.filter { note -> SeedData.notes.none { it.id == note.id } })
    }

    private fun curatedNoteFor(subject: Subject): Note? = when (subject.id) {
        "math" -> SeedData.notes.first { it.id == "note-derivative" }
        "physics" -> SeedData.notes.first { it.id == "note-newton" }
        "cs" -> SeedData.notes.first { it.id == "note-sorting" }
        "history" -> SeedData.notes.first { it.id == "note-hundred-years" }
        "english" -> SeedData.notes.first { it.id == "note-present-perfect" }
        else -> null
    }
}
