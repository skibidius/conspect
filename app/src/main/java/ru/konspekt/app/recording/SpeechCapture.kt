package ru.konspekt.app.recording

/** Common interface for offline Sherpa GigaAM and Android's system speech recognizer. */
interface SpeechCapture {
    fun start()
    fun stop()
}
