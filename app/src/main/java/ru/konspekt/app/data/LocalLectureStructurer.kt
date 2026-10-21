package ru.konspekt.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Fully on-device note structurer. It removes digressions and turns a
 * transcript into a readable note shell without any network access.
 */
object LocalLectureStructurer {

    private val offTopicMarkers = listOf(
        "консультац", "отступлен", "конференц", "столов", "перерыв",
        "домашн", "зачёт", "зачет", "расписан", "перенес", "перенос"
    )

    fun build(
        subject: Subject,
        topic: String,
        transcript: List<String>,
        durationMinutes: Int,
        dateLabel: String,
        timeLabel: String,
        filterTangents: Boolean
    ): Note {
        val clean = transcript.map { it.trim() }.filter { it.length > 2 }
        val (onTopic, digressions) = clean.partition { line ->
            offTopicMarkers.none { marker -> line.lowercase(Locale("ru")).contains(marker) }
        }
        val summarySource = onTopic.take(3).joinToString(" ")
        val summary = if (summarySource.length > 240) {
            summarySource.take(237).trimEnd() + "…"
        } else {
            summarySource.ifBlank { "Конспект по теме «$topic» готов. Основные тезисы собраны из лекции." }
        }

        val definitions = onTopic.filter {
            it.contains(" это ", ignoreCase = true) ||
                it.contains("называется", ignoreCase = true) ||
                it.contains("определяется", ignoreCase = true)
        }.take(3)
        val facts = onTopic.filterNot { it in definitions }.take(6)

        val sections = buildList {
            if (definitions.isNotEmpty()) {
                add(
                    NoteSection(
                        title = "Определения",
                        kind = SectionKind.DEFINITION,
                        body = definitions.joinToString(" "),
                        bullets = definitions.map { it.trim() }
                    )
                )
            }
            if (facts.isNotEmpty()) {
                add(
                    NoteSection(
                        title = "Основные тезисы",
                        kind = SectionKind.TEXT,
                        body = facts.joinToString(" "),
                        bullets = facts.map { it.trim() }
                    )
                )
            }
            add(
                NoteSection(
                    title = "Что запомнить",
                    kind = SectionKind.SUMMARY,
                    body = "",
                    bullets = listOf(
                        "Тема: $topic.",
                        "Ключевые мысли собраны из ${onTopic.size} реплик преподавателя.",
                        if (filterTangents) {
                            "Отфильтровано отступлений: ${digressions.size}."
                        } else {
                            "Отступления сохранены в конспекте."
                        }
                    )
                )
            )
        }

        val skipped = if (!filterTangents) emptyList() else digressions.mapIndexed { index, line ->
            SkippedFragment(
                time = String.format(Locale.US, "%02d:%02d", 9 + index * 4, 12 + index * 7),
                quote = line,
                reason = if (line.lowercase(Locale("ru")).contains("консультац")) "Организационное" else "Не по теме"
            )
        }

        val latex = buildLatex(topic, summary, definitions)

        return Note(
            id = "note-${UUID.randomUUID()}",
            subjectId = subject.id,
            title = topic,
            subtitle = subject.name,
            summary = summary,
            createdAt = Date().time,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            durationMinutes = durationMinutes,
            sections = sections,
            keyTerms = deriveTerms(topic, onTopic),
            skipped = skipped,
            latexSource = latex,
            wordCount = clean.sumOf { it.split(" ").size }
        )
    }

    private fun buildLatex(topic: String, summary: String, definitions: List<String>): String {
        val date = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())
        val safeSummary = summary.replace("\\", "").replace("&", "\\&").replace("%", "\\%")
        val defs = definitions.joinToString("\n\n") {
            "\\item ${it.replace("\\", "").replace("&", "\\&").replace("%", "\\%")}"
        }
        return """
            \documentclass[11pt]{article}
            \usepackage[T2A]{fontenc}
            \usepackage[utf8]{inputenc}
            \usepackage[russian]{babel}
            \usepackage{geometry}
            \geometry{margin=2.4cm}

            \title{$topic}
            \author{Conspect}
            \date{$date}

            \begin{document}
            \maketitle
            \section*{Коротко}
            $safeSummary

            \section*{Тезисы}
            \begin{itemize}
            $defs
            \end{itemize}
            \end{document}
        """.trimIndent()
    }

    private fun deriveTerms(topic: String, lines: List<String>): List<String> {
        val text = "$topic ${lines.joinToString(" ")}"
        val words = text.split(Regex("[^\\p{L}\\p{N}\\-]+"))
            .filter { it.length in 5..16 }
            .map { it.lowercase(Locale("ru")) }
        return words.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key.replaceFirstChar { c -> c.titlecase(Locale("ru")) } }
            .distinct()
            .take(4)
    }
}
