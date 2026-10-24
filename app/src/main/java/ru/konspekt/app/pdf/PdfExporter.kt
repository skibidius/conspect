package ru.konspekt.app.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.SectionKind
import ru.konspekt.app.data.Subject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a print-quality, text-based PDF entirely on device.
 *
 * Text is drawn with PdfDocument's canvas rather than a screenshot, so every
 * word can be selected and copied in a PDF reader. Formulas stay in readable
 * LaTeX form and are also copyable.
 */
object PdfExporter {
    fun export(
        context: Context,
        note: Note,
        subject: Subject,
        shareAfter: Boolean = true,
        onResult: (File?, String) -> Unit
    ) {
        Thread {
            val main = Handler(Looper.getMainLooper())
            val file = try {
                buildPdf(context, note, subject)
            } catch (e: Exception) {
                main.post { onResult(null, "Не удалось сохранить PDF") }
                return@Thread
            }
            val saved = copyToDownloads(context, file, file.name)
            main.post {
                onResult(file, if (saved) "PDF сохранён в Загрузки" else "PDF готов")
                if (shareAfter) share(context, file)
            }
        }.start()
    }

    private fun buildPdf(context: Context, note: Note, subject: Subject): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val safeTitle = note.title
            .replace(Regex("[^\\p{L}\\p{N}\\- ]"), "")
            .replace(" ", "-")
            .take(48)
            .ifBlank { "konspekt" }
        val file = File(dir, "$safeTitle-$stamp.pdf")

        val document = PdfDocument()
        val writer = TextWriter(document, note, subject)
        writer.render()
        try {
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }

    private class TextWriter(
        private val document: PdfDocument,
        private val note: Note,
        private val subject: Subject
    ) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 46f
        private val contentWidth = pageWidth - margin * 2
        private val bottom = pageHeight - margin
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f

        private val ink = Color.rgb(27, 30, 24)
        private val bodyInk = Color.rgb(52, 58, 50)
        private val muted = Color.rgb(112, 122, 110)
        private val accent = Color.rgb(63, 99, 72)
        private val accentLight = Color.rgb(237, 246, 232)
        private val lineColor = Color.rgb(221, 228, 217)

        private val titlePaint = paint(24f, ink, Typeface.BOLD)
        private val subtitlePaint = paint(11f, muted)
        private val bodyPaint = paint(10.5f, bodyInk)
        private val smallPaint = paint(8.5f, muted)
        private val labelPaint = paint(8f, accent, Typeface.BOLD)
        private val headingPaint = paint(14f, ink, Typeface.BOLD)
        private val kindPaint = paint(7.5f, accent, Typeface.BOLD)
        private val formulaPaint = paint(9.5f, Color.rgb(52, 70, 54)).apply {
            typeface = Typeface.MONOSPACE
        }
        private val formulaLabelPaint = paint(8f, muted).apply { textAlign = Paint.Align.CENTER }
        private val bulletPaint = paint(10.5f, bodyInk)
        private val summaryPaint = paint(10.5f, bodyInk)

        fun render() {
            newPage()
            drawBrand()
            drawHeader()
            drawSummary()
            note.sections.forEach { drawSection(it) }
            if (note.skipped.isNotEmpty()) drawSkipped()
            finishPage()
        }

        private fun newPage() {
            finishPage()
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(info)
            canvas = page!!.canvas
            canvas.drawColor(Color.WHITE)
            y = margin
        }

        private fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
        }

        private fun ensureSpace(height: Float) {
            if (y + height > bottom) newPage()
        }

        private fun drawBrand() {
            canvas.drawText("CONSPECT · ЛОКАЛЬНЫЙ КОНСПЕКТ", margin, y, paint(8f, accent, Typeface.BOLD))
            y += 20f
        }

        private fun drawHeader() {
            val date = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(note.createdAt))
            drawText(
                subject.name,
                paint(8.5f, accent, Typeface.BOLD)
            )
            y += 22f
            drawWrapped(note.title, titlePaint, lineHeight = 29f)
            if (note.subtitle.isNotBlank() && note.subtitle != subject.name) {
                y += 2f
                drawWrapped(note.subtitle, subtitlePaint, lineHeight = 16f)
            }
            y += 9f
            canvas.drawLine(margin, y, pageWidth - margin, y, paint(0.7f, lineColor))
            y += 13f
            canvas.drawText(
                "$date   ·   ${note.durationMinutes} мин записи   ·   ${note.wordCount} слов",
                margin,
                y,
                smallPaint
            )
            y += 20f
        }

        private fun drawSummary() {
            if (note.summary.isBlank()) return
            val labelHeight = 18f
            val padding = 14f
            val lineHeight = 16f
            val lines = wrap(note.summary, summaryPaint, contentWidth - padding * 2)
            val boxHeight = padding * 2 + labelHeight + lines.size * lineHeight
            ensureSpace(boxHeight + 14f)
            val top = y
            canvas.drawRoundRect(
                RectF(margin, top, pageWidth - margin, top + boxHeight),
                12f,
                12f,
                paint(1f, accentLight).apply { style = Paint.Style.FILL }
            )
            canvas.drawText("КОРОТКО", margin + padding, top + padding + 7f, labelPaint)
            var textY = top + padding + labelHeight + 8f
            lines.forEach { line ->
                canvas.drawText(line, margin + padding, textY, summaryPaint)
                textY += lineHeight
            }
            y = top + boxHeight + 20f
        }

        private fun drawSection(section: ru.konspekt.app.data.NoteSection) {
            ensureSpace(58f)
            val kind = when (section.kind) {
                SectionKind.DEFINITION -> "ОПРЕДЕЛЕНИЕ"
                SectionKind.THEOREM -> "ТЕОРЕМА"
                SectionKind.EXAMPLE -> "ПРИМЕР"
                SectionKind.SUMMARY -> "ИТОГ"
                SectionKind.TEXT -> ""
            }
            if (kind.isNotBlank()) {
                canvas.drawText(kind, margin, y, kindPaint)
                y += 12f
            }
            drawWrapped(section.title, headingPaint, lineHeight = 19f)
            y += 4f
            val body = withoutKeyTerms(section.body)
            if (body.isNotBlank()) {
                drawWrapped(body, bodyPaint, lineHeight = 15.5f)
                y += 3f
            }
            section.formula?.let { formula ->
                drawFormula(formula, section.formulaLabel)
            }
            val bullets = section.bullets
                .map(::withoutKeyTerms)
                .filter { it.isNotBlank() }
            if (bullets.isNotEmpty()) {
                bullets.forEach { bullet ->
                    drawBullet(bullet)
                }
                y += 2f
            }
            y += 14f
        }

        private fun drawFormula(formula: String, label: String?) {
            val padding = 10f
            val displayFormula = prettyFormula(formula)
            val lines = wrap(displayFormula, formulaPaint, contentWidth - padding * 2)
            val boxHeight = lines.size * 14f + padding * 2
            ensureSpace(boxHeight + 18f)
            val top = y
            canvas.drawRoundRect(
                RectF(margin, top, pageWidth - margin, top + boxHeight),
                9f,
                9f,
                paint(1f, Color.rgb(242, 246, 239)).apply { style = Paint.Style.FILL }
            )
            var textY = top + padding + 10f
            lines.forEach { line ->
                val width = formulaPaint.measureText(line)
                canvas.drawText(line, margin + (contentWidth - width) / 2f, textY, formulaPaint)
                textY += 14f
            }
            y = top + boxHeight + 4f
            if (!label.isNullOrBlank()) {
                ensureSpace(24f)
                canvas.drawText(label, pageWidth / 2f, y + 9f, formulaLabelPaint)
                y += 24f
            }
        }

        private fun drawBullet(text: String) {
            val bulletIndent = 12f
            val lineHeight = 15.5f
            val lines = wrap(text, bulletPaint, contentWidth - bulletIndent)
            ensureSpace(lineHeight + 2f)
            canvas.drawCircle(margin + 3f, y - 3.4f, 2.2f, paint(1f, accent).apply {
                style = Paint.Style.FILL
            })
            lines.forEach { line ->
                ensureSpace(lineHeight)
                canvas.drawText(line, margin + bulletIndent, y, bulletPaint)
                y += lineHeight
            }
            y += 2f
        }

        private fun drawSkipped() {
            ensureSpace(60f)
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, paint(0.7f, lineColor))
            y += 18f
            drawWrapped("Отступления преподавателя", headingPaint, lineHeight = 19f)
            y += 2f
            drawWrapped("Скрыто фрагментов: ${note.skipped.size}", smallPaint, lineHeight = 14f)
            y += 4f
            note.skipped.forEach { fragment ->
                ensureSpace(48f)
                drawWrapped("${fragment.time}  ${fragment.quote}", bodyPaint, lineHeight = 14.5f)
                drawWrapped(fragment.reason.uppercase(), smallPaint, lineHeight = 12f)
                y += 4f
            }
        }

        private fun drawText(text: String, paint: Paint) {
            ensureSpace(paint.textSize + 4f)
            canvas.drawText(text, margin, y, paint)
            y += paint.textSize + 4f
        }

        private fun drawWrapped(text: String, paint: Paint, lineHeight: Float) {
            text.split('\n').forEach { paragraph ->
                if (paragraph.isBlank()) {
                    y += lineHeight * 0.6f
                    return@forEach
                }
                wrap(paragraph, paint, contentWidth).forEach { line ->
                    ensureSpace(lineHeight)
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
            }
        }

        private fun prettyFormula(source: String): String {
            var s = source.trim()
            repeat(4) {
                s = Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""").replace(s, "($1)/($2)")
                s = Regex("""\\sqrt\{([^{}]*)\}""").replace(s, "√($1)")
            }
            s = s.replace("\\left", "").replace("\\right", "")
            listOf(
                "\\Delta" to "Δ",
                "\\delta" to "δ",
                "\\alpha" to "α",
                "\\beta" to "β",
                "\\gamma" to "γ",
                "\\lambda" to "λ",
                "\\mu" to "μ",
                "\\pi" to "π",
                "\\sigma" to "σ",
                "\\theta" to "θ",
                "\\varphi" to "φ",
                "\\phi" to "φ",
                "\\omega" to "ω",
                "\\infty" to "∞",
                "\\cdot" to "·",
                "\\times" to "×",
                "\\div" to "÷",
                "\\pm" to "±",
                "\\le" to "≤",
                "\\ge" to "≥",
                "\\neq" to "≠",
                "\\to" to "→",
                "\\approx" to "≈",
                "\\sum" to "Σ",
                "\\int" to "∫"
            ).forEach { (from, to) -> s = s.replace(from, to) }
            listOf("sin", "cos", "tan", "log", "ln", "lim", "max", "min", "exp").forEach {
                s = s.replace("\\$it", it)
            }
            s = Regex("""\\(?:text|mathrm|operatorname)\{([^{}]*)\}""").replace(s, "$1")
            s = Regex("""_\{([^{}]*)\}""").replace(s, "_($1)")
            s = Regex("""\^\{([^{}]*)\}""").replace(s, "^($1)")
            s = s.replace("^{2}", "²").replace("^{3}", "³")
            s = s.replace("\\,", " ").replace("\\;", " ").replace("\\!", "")
            s = s.replace("Δ x", "Δx")
            s = s.replace("{", "").replace("}", "")
            return s.replace("  ", " ").trim()
        }

        private fun withoutKeyTerms(text: String): String =
            text.lines()
                .filterNot {
                    val line = it.trim()
                    line.startsWith("Ключевые понятия", ignoreCase = true) ||
                        line.startsWith("Ключевые слова", ignoreCase = true)
                }
                .joinToString("\n")
                .trim()

        private fun wrap(text: String, paint: Paint, width: Float): List<String> {
            if (text.isBlank()) return emptyList()
            val result = mutableListOf<String>()
            text.split('\n').forEach { paragraph ->
                var remaining = paragraph.trim()
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, width, null)
                    if (count <= 0) break
                    var cut = count
                    while (cut > 0 && cut < remaining.length && remaining[cut] != ' ') cut--
                    if (cut <= 0 || cut == remaining.length) cut = count
                    result += remaining.substring(0, cut).trimEnd()
                    remaining = remaining.substring(cut).trimStart()
                }
            }
            return result
        }

        private fun paint(size: Float, color: Int, style: Int = Typeface.NORMAL): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create("sans-serif", style)
            }
    }

    private fun copyToDownloads(context: Context, file: File, name: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Conspect")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    private fun share(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться конспектом"))
        }
    }
}
