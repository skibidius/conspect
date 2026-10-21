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

    var settings by mutableStateOf(repository.loadSettings())
        private set

    var subjects by mutableStateOf(SeedData.subjects + repository.loadSubjects())
        private set

    var notes by mutableStateOf(
        (repository.loadNotes() + SeedData.notes).sortedByDescending { it.createdAt }
    )
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

    fun createNote(
        subjectId: String,
        topic: String,
        transcript: List<String>,
        durationMinutes: Int,
        demo: Boolean = true
    ): Note {
        val subject = subject(subjectId) ?: subjects.first()
        val curated = if (demo) curatedNoteFor(subject) else null
        val now = Date()
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(now)
        val time = SimpleDateFormat("HH:mm", Locale("ru")).format(now)

        val generated = if (curated != null) {
            curated.copy(
                id = "note-${UUID.randomUUID()}",
                subjectId = subjectId,
                title = topic.ifBlank { curated.title },
                dateLabel = date,
                timeLabel = time,
                createdAt = now.time,
                durationMinutes = durationMinutes.coerceAtLeast(1)
            )
        } else {
            LocalLectureStructurer.build(
                subject = subject,
                topic = topic.ifBlank { "Новая лекция" },
                transcript = transcript,
                durationMinutes = durationMinutes,
                dateLabel = date,
                timeLabel = time,
                filterTangents = settings.filterTangents
            )
        }

        val saved = generated.copy(
            summary = generated.summary,
            skipped = if (settings.filterTangents) generated.skipped else emptyList()
        )
        notes = (listOf(saved) + notes).sortedByDescending { it.createdAt }
        persistLocalNotes()
        return saved
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
