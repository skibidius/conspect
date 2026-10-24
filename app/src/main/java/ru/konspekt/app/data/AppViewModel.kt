package ru.konspekt.app.data

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import ru.konspekt.app.BuildConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.konspekt.app.recording.AudioFileTranscriber
import ru.konspekt.app.recording.AudioTranscriptionProgress
import ru.konspekt.app.recording.AudioTranscriptionStage
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

    private val debugFixtures = BuildConfig.DEBUG

    var subjects by mutableStateOf(
        (if (debugFixtures) SeedData.subjects else emptyList()) + repository.loadSubjects()
    )
        private set

    var notes by mutableStateOf(
        (repository.loadNotes() + if (debugFixtures) SeedData.notes else emptyList())
            .sortedByDescending { it.createdAt }
    )
        private set

    var trash by mutableStateOf(repository.loadTrash())
        private set

    var hasDeepSeekKey by mutableStateOf(apiKeyStore.hasKey())
        private set

    var audioImport by mutableStateOf<AudioImportState>(AudioImportState.Idle)
        private set

    var importScreenVisible by mutableStateOf(false)
        private set

    private var audioImportJob: Job? = null

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

    fun updateSubject(id: String, name: String, tone: SubjectTone, icon: SubjectIcon) {
        val clean = name.trim()
        if (clean.isBlank()) return
        subjects = subjects.map {
            if (it.id == id) it.copy(name = clean, shortName = clean, tone = tone, icon = icon) else it
        }
        repository.saveSubjects(subjects.filter { it.id.startsWith("custom-") })
    }

    fun addSubject(name: String, tone: SubjectTone, icon: SubjectIcon): Subject {
        val clean = name.trim()
        val subject = Subject(
            id = "custom-${UUID.randomUUID()}",
            name = clean,
            shortName = clean,
            teacher = "Преподаватель",
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

    fun startAudioImport(subjectId: String, topic: String, uri: Uri) {
        if (audioImport is AudioImportState.Running) {
            importScreenVisible = true
            return
        }
        audioImportJob?.cancel()
        importScreenVisible = true
        audioImport = AudioImportState.Running(
            subjectId = subjectId,
            topic = topic,
            progress = AudioTranscriptionProgress(
                stage = AudioTranscriptionStage.MODEL,
                fraction = 0f
            ),
            startedAtMillis = SystemClock.elapsedRealtime()
        )

        audioImportJob = viewModelScope.launch {
            try {
                val transcription = AudioFileTranscriber.transcribe(
                    context = getApplication(),
                    uri = uri
                ) { progress ->
                    val current = audioImport
                    if (current is AudioImportState.Running) {
                        audioImport = current.copy(progress = progress)
                    }
                }
                if (transcription.lines.isEmpty()) {
                    error("Речь не распознана. Попробуйте файл с более чистым звуком.")
                }
                val durationMinutes = ((transcription.durationSeconds + 59) / 60)
                    .coerceAtLeast(1)
                val result = generateNote(
                    subjectId = subjectId,
                    topic = topic,
                    transcript = transcription.lines,
                    durationMinutes = durationMinutes,
                    demo = false
                )
                audioImport = AudioImportState.Completed(
                    subjectId = subjectId,
                    topic = topic,
                    noteId = result.noteId,
                    warning = result.warning
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                audioImport = AudioImportState.Failed(
                    subjectId = subjectId,
                    topic = topic,
                    message = error.message ?: "Не удалось обработать лекцию"
                )
            }
        }
    }

    fun cancelAudioImport() {
        audioImportJob?.cancel()
        audioImportJob = null
        audioImport = AudioImportState.Idle
        importScreenVisible = false
    }

    fun hideImportScreen() {
        importScreenVisible = false
    }

    fun showImportScreen() {
        if (audioImport !is AudioImportState.Idle) importScreenVisible = true
    }

    fun consumeAudioImport() {
        audioImport = AudioImportState.Idle
        importScreenVisible = false
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
            transcript.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.take(5)
                ?.joinToString(" ")
                ?.replaceFirstChar { it.titlecase(Locale("ru")) }
                ?.takeIf { it.isNotBlank() }
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
            val cloudResult = runCatching {
                deepSeek.generate(
                    apiKey = cloudKey,
                    subject = subject,
                    topic = topic,
                    transcript = transcript,
                    durationMinutes = durationMinutes,
                    filterTangents = settings.filterTangents
                )
            }
            val cloudNote = cloudResult.getOrNull()
            if (cloudNote != null) {
                saveGeneratedNote(cloudNote)
                return NoteGenerationResult(cloudNote.id)
            }
            val cloudError = cloudResult.exceptionOrNull()?.message
                ?.takeIf { it.isNotBlank() }
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
                cloudError?.let { "DeepSeek: $it. Конспект собран локально." }
                    ?: "DeepSeek недоступен. Конспект собран локально."
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

    fun exportBackupJson(): String = BackupManager.toJson(
        AppBackup(
            settings = settings,
            subjects = subjects,
            notes = notes,
            trash = trash
        )
    )

    fun importBackupJson(raw: String): Boolean {
        val backup = BackupManager.fromJson(raw) ?: return false
        settings = backup.settings
        subjects = backup.subjects
        notes = backup.notes.sortedByDescending { it.createdAt }
        trash = backup.trash
        repository.saveSettings(settings)
        repository.saveSubjects(subjects)
        repository.saveNotes(notes)
        repository.saveTrash(trash)
        return true
    }

    fun deleteNote(id: String) {
        val note = notes.firstOrNull { it.id == id } ?: return
        notes = notes.filterNot { it.id == id }
        trash = listOf(note) + trash.filterNot { it.id == id }
        persistLocalNotes()
        persistTrash()
    }

    fun restoreNote(id: String) {
        val note = trash.firstOrNull { it.id == id } ?: return
        trash = trash.filterNot { it.id == id }
        notes = (listOf(note) + notes).sortedByDescending { it.createdAt }
        persistLocalNotes()
        persistTrash()
    }

    fun deleteNotePermanently(id: String) {
        trash = trash.filterNot { it.id == id }
        persistTrash()
    }

    fun emptyTrash() {
        trash = emptyList()
        persistTrash()
    }

    fun deleteSubject(id: String) {
        subjects = subjects.filterNot { it.id == id }
        notes = notes.filterNot { it.subjectId == id }
        trash = trash.filterNot { it.subjectId == id }
        repository.saveSubjects(subjects.filter { it.id.startsWith("custom-") })
        persistLocalNotes()
        persistTrash()
    }

    private fun persistLocalNotes() {
        repository.saveNotes(notes.filter { note -> SeedData.notes.none { it.id == note.id } })
    }

    private fun persistTrash() {
        repository.saveTrash(trash)
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
