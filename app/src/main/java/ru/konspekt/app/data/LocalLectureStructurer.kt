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
        val sentences = normalizeSentences(onTopic)
        val summarySource = sentences.take(3).joinToString(" ")
        val summary = if (summarySource.length > 280) {
            summarySource.take(277).trimEnd() + "…"
        } else {
            summarySource.ifBlank { "Конспект по теме «$topic» готов. Основные тезисы собраны из лекции." }
        }

        val definitions = sentences.filter {
            it.contains(" это ", ignoreCase = true) ||
                it.contains("называется", ignoreCase = true) ||
                it.contains("определяется", ignoreCase = true)
        }.take(4)
        val examples = sentences.filter {
            !definitions.contains(it) && (
                it.contains("например", ignoreCase = true) ||
                    it.contains("пример", ignoreCase = true) ||
                    it.contains("рассмотрим", ignoreCase = true)
                )
        }.take(3)
        val facts = sentences
            .filterNot { it in definitions || it in examples }
            .take(8)
        val terms = deriveTerms(topic, onTopic)

        val sections = buildList {
            if (definitions.isNotEmpty()) {
                add(
                    NoteSection(
                        title = "Определения",
                        kind = SectionKind.DEFINITION,
                        body = definitions.joinToString("\n\n"),
                        bullets = definitions
                    )
                )
            }
            if (facts.isNotEmpty()) {
                add(
                    NoteSection(
                        title = "Основные тезисы",
                        kind = SectionKind.TEXT,
                        body = facts.chunked(3).joinToString("\n\n") { it.joinToString(" ") },
                        bullets = facts
                    )
                )
            }
            if (examples.isNotEmpty()) {
                add(
                    NoteSection(
                        title = "Примеры",
                        kind = SectionKind.EXAMPLE,
                        body = examples.joinToString("\n\n"),
                        bullets = examples
                    )
                )
            }
            add(
                NoteSection(
                    title = "Что запомнить",
                    kind = SectionKind.SUMMARY,
                    body = "",
                    bullets = buildList {
                        add("Тема: $topic.")
                        if (terms.isNotEmpty()) {
                            add("Ключевые понятия: ${terms.take(4).joinToString(" · ")}.")
                        }
                        add("Основных тезисов: ${facts.size}.")
                    }
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
            keyTerms = terms,
            skipped = skipped,
            latexSource = latex,
            wordCount = clean.sumOf { it.split(" ").size }
        )
    }

    private fun normalizeSentences(lines: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return lines
            .flatMap { line ->
                line.split(Regex("(?<=[.!?])\\s+"))
                    .flatMap { part ->
                        part.split(
                            Regex(
                                "(?i)(?=\\b(?:например|то есть|затем|теперь|далее|следовательно|итак|производная равна)\\b)"
                            )
                        )
                    }
            }
            .map { raw ->
                var text = raw.trim().replace(Regex("\\s+"), " ")
                listOf("ну ", "вот ", "то есть ", "как бы ", "значит ", "так ").forEach { prefix ->
                    if (text.startsWith(prefix, ignoreCase = true)) {
                        text = text.substring(prefix.length)
                    }
                }
                if (text.isBlank()) return@map ""
                text = text.replaceFirstChar { it.titlecase(Locale("ru")) }
                if (text.lastOrNull() !in listOf('.', '!', '?')) text = "$text."
                text
            }
            .filter { it.length > 3 }
            .filter { seen.add(it.lowercase(Locale("ru")).replace(Regex("[^\\p{L}\\p{N}]"), "")) }
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
        val stopWords = setOf(
            "сегодня", "например", "который", "которая", "которые", "будем",
            "можно", "нужно", "равна", "равно", "через", "между", "после",
            "перед", "теперь", "далее", "итак", "этого", "этот", "эта",
            "эти", "также", "очень", "просто", "только", "когда", "потому",
            "поэтому", "функция", "функции", "производная"
        )
        val words = text.split(Regex("[^\\p{L}\\p{N}\\-]+"))
            .filter { it.length in 5..16 && it.lowercase(Locale("ru")) !in stopWords }
            .map { it.lowercase(Locale("ru")) }
        return words.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key.replaceFirstChar { c -> c.titlecase(Locale("ru")) } }
            .distinct()
            .take(4)
    }
}
