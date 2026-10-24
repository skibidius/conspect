package ru.konspekt.app.data

import ru.konspekt.app.recording.AudioTranscriptionProgress

sealed interface AudioImportState {
    data object Idle : AudioImportState

    data class Running(
        val subjectId: String,
        val topic: String,
        val progress: AudioTranscriptionProgress
    ) : AudioImportState

    data class Completed(
        val subjectId: String,
        val topic: String,
        val noteId: String,
        val warning: String? = null
    ) : AudioImportState

    data class Failed(
        val subjectId: String,
        val topic: String,
        val message: String
    ) : AudioImportState
}
