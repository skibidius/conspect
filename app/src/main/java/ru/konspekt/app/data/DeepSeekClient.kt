package ru.konspekt.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optional cloud mode. The transcript is sent to DeepSeek only after the user
 * explicitly enables it and saves an API key. Local mode never touches network.
 */
class DeepSeekClient {
    suspend fun generate(
        apiKey: String,
        subject: Subject,
        topic: String,
        transcript: List<String>,
        durationMinutes: Int,
        filterTangents: Boolean
    ): Note = withContext(Dispatchers.IO) {
        val cleanTranscript = transcript
            .filter { it.isNotBlank() }
            .joinToString("\n") { it.trim() }
            .take(80_000)
        require(cleanTranscript.isNotBlank()) { "Пустой транскрипт" }

        val request = JSONObject().apply {
            put("model", "deepseek-flash")
            put("temperature", 0.2)
            put("max_tokens", 12_000)
            put("response_format", JSONObject().put("type", "json_object"))
            put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", SYSTEM_PROMPT)
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", buildUserPrompt(subject, topic, durationMinutes, cleanTranscript))
                    )
            )
        }

        val response = post(apiKey, request.toString())
        val content = JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        parseNote(content, subject, topic, durationMinutes, cleanTranscript, filterTangents)
    }

    private fun post(apiKey: String, payload: String): String {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 300_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        }
        return try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrNull()
                val friendly = when (status) {
                    401, 403 -> "Неверный API-ключ DeepSeek"
                    402 -> "На балансе DeepSeek недостаточно средств"
                    429 -> "Слишком много запросов к DeepSeek"
                    else -> apiMessage ?: "DeepSeek вернул ошибку $status"
                }
                error(friendly)
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUserPrompt(
        subject: Subject,
        topic: String,
        durationMinutes: Int,
        transcript: String
    ): String = """
        Предмет: ${subject.name}
        Тема: ${topic.ifBlank { "определи по содержанию" }}
        Длительность: $durationMinutes мин.

        Транскрипт лекции:
        $transcript

        Собери конспект для студента. Верни только JSON без markdown.
    """.trimIndent()

    private fun parseNote(
        raw: String,
        subject: Subject,
        topic: String,
        durationMinutes: Int,
        transcript: String,
        filterTangents: Boolean
    ): Note {
        val content = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .let { text ->
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                if (start >= 0 && end > start) text.substring(start, end + 1) else text
            }

        val root = JSONObject(content)
        val sectionsJson = root.optJSONArray("sections") ?: JSONArray()
        val sections = buildList {
            for (i in 0 until sectionsJson.length()) {
                val item = sectionsJson.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                val body = item.optString("body").trim()
                val formula = item.optString("formula").trim()
                    .removeSurrounding("$$", "$$")
                    .removePrefix("\\[").removeSuffix("\\]")
                    .removePrefix("\\(").removeSuffix("\\)")
                    .trim()
                val bullets = item.optJSONArray("bullets")?.let { arr ->
                    (0 until arr.length()).mapNotNull { index -> arr.optString(index).trim().takeIf { it.isNotBlank() } }
                }.orEmpty()
                if (title.isBlank() && body.isBlank() && formula.isBlank() && bullets.isEmpty()) continue
                add(
                    NoteSection(
                        title = title.ifBlank { "Тезисы" },
                        kind = parseKind(item.optString("kind")),
                        body = body.cleanMarkdown(),
                        formula = formula.takeIf { it.isNotBlank() },
                        formulaLabel = item.optString("formulaLabel").trim().takeIf { it.isNotBlank() },
                        bullets = bullets.map { it.cleanMarkdown() }
                    )
                )
            }
        }
        require(sections.isNotEmpty()) { "DeepSeek не вернул разделы конспекта" }

        val keyTerms = root.optJSONArray("keyTerms")?.let { arr ->
            (0 until arr.length()).mapNotNull { index -> arr.optString(index).trim().takeIf { it.isNotBlank() } }
        }.orEmpty()

        val skipped = if (!filterTangents) emptyList() else root.optJSONArray("skipped")?.let { arr ->
            (0 until arr.length()).mapNotNull { index ->
                val item = arr.optJSONObject(index) ?: return@mapNotNull null
                val quote = item.optString("quote").trim()
                if (quote.isBlank()) null else SkippedFragment(
                    time = item.optString("time").trim().ifBlank { "—" },
                    quote = quote,
                    reason = item.optString("reason").trim().ifBlank { "Не по теме" }
                )
            }
        }.orEmpty()

        val now = Date()
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(now)
        val time = SimpleDateFormat("HH:mm", Locale("ru")).format(now)
        val title = root.optString("title").trim().ifBlank { topic.ifBlank { "Новая лекция" } }
        val summary = root.optString("summary").trim().ifBlank { sections.firstOrNull()?.body.orEmpty() }
        val subtitle = root.optString("subtitle").trim()
        val latex = root.optString("latexSource").trim().ifBlank {
            buildLatex(title, summary, sections)
        }

        return Note(
            id = "note-${UUID.randomUUID()}",
            subjectId = subject.id,
            title = title,
            subtitle = subtitle,
            summary = summary,
            createdAt = now.time,
            dateLabel = date,
            timeLabel = time,
            durationMinutes = durationMinutes.coerceAtLeast(1),
            sections = sections,
            keyTerms = keyTerms,
            skipped = skipped,
            latexSource = latex,
            wordCount = transcript.split(Regex("\\s+")).count { it.isNotBlank() }
        )
    }

    private fun parseKind(value: String): SectionKind = when (value.trim().uppercase(Locale.US)) {
        "DEFINITION", "ОПРЕДЕЛЕНИЕ" -> SectionKind.DEFINITION
        "THEOREM", "ТЕОРЕМА" -> SectionKind.THEOREM
        "EXAMPLE", "ПРИМЕР" -> SectionKind.EXAMPLE
        "SUMMARY", "ИТОГ" -> SectionKind.SUMMARY
        else -> SectionKind.TEXT
    }

    private fun String.cleanMarkdown(): String = this
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .trim()

    private fun buildLatex(title: String, summary: String, sections: List<NoteSection>): String {
        val body = sections.joinToString("\n\n") { section ->
            buildString {
                append("\\section*{${section.title.latexEscape()}}\n")
                if (section.body.isNotBlank()) append(section.body.latexEscape()).append("\n\n")
                if (section.formula != null) {
                    append("\\[\n").append(section.formula).append("\n\\]\n\n")
                }
                if (section.bullets.isNotEmpty()) {
                    append("\\begin{itemize}\n")
                    section.bullets.forEach { append("  \\item ").append(it.latexEscape()).append("\n") }
                    append("\\end{itemize}\n")
                }
            }
        }
        return """
            \documentclass[11pt]{article}
            \usepackage[T2A]{fontenc}
            \usepackage[utf8]{inputenc}
            \usepackage[russian]{babel}
            \usepackage{amsmath, amssymb}
            \usepackage{geometry}
            \geometry{margin=2.4cm}

            \title{${title.latexEscape()}}
            \author{Conspect}
            \date{\today}

            \begin{document}
            \maketitle
            \section*{Коротко}
            ${summary.latexEscape()}

            $body
            \end{document}
        """.trimIndent()
    }

    private fun String.latexEscape(): String = this
        .replace("\\", "\\textbackslash{}")
        .replace("&", "\\&")
        .replace("%", "\\%")
        .replace("$", "\\$")
        .replace("#", "\\#")
        .replace("_", "\\_")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("~", "\\textasciitilde{}")
        .replace("^", "\\textasciicircum{}")

    private companion object {
        const val ENDPOINT = "https://api.deepseek.com/chat/completions"
        val SYSTEM_PROMPT = """
            Ты — методист и редактор студенческих конспектов. Твоя задача — превратить
            транскрипт лекции на русском языке в точный, красивый и компактный конспект.

            Правила:
            1. Сохраняй только содержание по теме. Организационные объявления,
               шутки, истории и отступления вынеси в skipped и не включай в разделы.
            2. Структура: 3–7 разделов с содержательными названиями. Используй типы
               DEFINITION, THEOREM, EXAMPLE, SUMMARY, TEXT.
            3. Если в лекции есть формулы, запиши их в LaTeX в поле formula без $ и без \[ \].
            4. Пиши по-русски, ясно и без воды. Не выдумывай факты, которых не было.
            5. Верни СТРОГО валидный JSON такого вида:
            {
              "title": "string",
              "subtitle": "string",
              "summary": "2–3 предложения",
              "sections": [
                {
                  "title": "string",
                  "kind": "DEFINITION|THEOREM|EXAMPLE|SUMMARY|TEXT",
                  "body": "string",
                  "formula": "string или пустая строка",
                  "formulaLabel": "string или пустая строка",
                  "bullets": ["string"]
                }
              ],
              "keyTerms": ["string"],
              "skipped": [
                {"time": "mm:ss или —", "quote": "string", "reason": "Не по теме|Организационное"}
              ]
            }
            Поле latexSource не включай: LaTeX-документ будет собран приложением.
        """.trimIndent()
    }
}
