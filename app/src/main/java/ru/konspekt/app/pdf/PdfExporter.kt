package ru.konspekt.app.pdf

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
 * Builds a print-quality PDF completely on device.
 * HTML + bundled KaTeX renders formulas; the rendered page is sliced into A4 pages.
 */
object PdfExporter {
    private var activeView: WebView? = null

    fun export(
        context: Context,
        note: Note,
        subject: Subject,
        onResult: (File?, String) -> Unit
    ) {
        val activity = context.findActivity()
        val density = context.resources.displayMetrics.density
        val initialWidth = (794f * density).toInt().coerceAtLeast(800)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            setBackgroundColor(Color.WHITE)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            layoutParams = FrameLayout.LayoutParams(initialWidth, 1800)
            translationX = -4000f
            translationY = -4000f
        }
        activeView = webView

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    buildPdf(context, view, note, subject, onResult)
                }, 900)
            }
        }

        val root = activity?.window?.decorView as? ViewGroup
        if (root != null) {
            root.addView(webView)
        }
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(initialWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, initialWidth, 1800)

        webView.loadDataWithBaseURL(
            "file:///android_asset/katex/",
            buildHtml(note, subject),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun buildPdf(
        context: Context,
        webView: WebView,
        note: Note,
        subject: Subject,
        onResult: (File?, String) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        webView.evaluateJavascript(
            "(function(){var d=document.documentElement;return Math.ceil(Math.max(d.scrollHeight, document.body.scrollHeight));})()"
        ) { raw ->
            val cssHeight = raw?.trim('"')?.toFloatOrNull()?.coerceAtLeast(500f) ?: 0f
            if (cssHeight <= 0f) {
                cleanup(webView)
                onResult(null, "Не удалось измерить конспект")
                return@evaluateJavascript
            }
            val viewWidth = (794f * density).toInt().coerceAtLeast(800)
            val viewHeight = (cssHeight * density).toInt().coerceIn(1200, 36000)
            webView.layoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
            webView.measure(
                View.MeasureSpec.makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY)
            )
            webView.layout(0, 0, viewWidth, viewHeight)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    renderPdf(context, webView, note, onResult)
                } catch (e: Exception) {
                    cleanup(webView)
                    onResult(null, "Не удалось сохранить PDF")
                }
            }, 350)
        }
    }

    private fun renderPdf(
        context: Context,
        webView: WebView,
        note: Note,
        onResult: (File?, String) -> Unit
    ) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val safeTitle = note.title
            .replace(Regex("[^\\p{L}\\p{N}\\- ]"), "")
            .replace(" ", "-")
            .take(48)
            .ifBlank { "konspekt" }
        val file = File(dir, "$safeTitle-$stamp.pdf")

        // A4 in PDF points.
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42
        val contentWidth = pageWidth - margin * 2
        val contentHeight = pageHeight - margin * 2
        val viewWidth = webView.width.coerceAtLeast(1)
        val viewHeight = webView.height.coerceAtLeast(1)
        val scale = contentWidth.toFloat() / viewWidth.toFloat()

        // Draw the fully laid-out WebView once into a bitmap and then slice it.
        // This keeps WebView rendering independent from PDF canvas transforms.
        val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val bitmapCanvas = Canvas(bitmap)
        bitmapCanvas.drawColor(Color.WHITE)
        webView.draw(bitmapCanvas)
        val document = PdfDocument()
        var sourceTop = 0
        var pageNumber = 1
        while (sourceTop < viewHeight) {
            val sourceBottom = (sourceTop + (contentHeight / scale).toInt()).coerceAtMost(viewHeight)
            val src = Rect(0, sourceTop, viewWidth, sourceBottom)
            val dstHeight = ((sourceBottom - sourceTop) * scale).toInt().coerceAtLeast(1)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(
                bitmap,
                src,
                Rect(margin, margin, margin + contentWidth, margin + dstHeight),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            document.finishPage(page)
            sourceTop = sourceBottom
            pageNumber += 1
        }

        try {
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
            bitmap.recycle()
            cleanup(webView)
        }

        val saved = copyToDownloads(context, file, file.name)
        onResult(file, if (saved) "PDF сохранён в Загрузки" else "PDF готов")
        share(context, file)
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

    private fun cleanup(webView: WebView) {
        try {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        } catch (_: Exception) {
        }
        if (activeView === webView) activeView = null
    }

    private fun Context.findActivity(): android.app.Activity? {
        var current = this
        while (current is ContextWrapper) {
            if (current is android.app.Activity) return current
            current = current.baseContext
        }
        return null
    }

    private fun buildHtml(note: Note, subject: Subject): String {
        val sections = note.sections.joinToString("\n") { section ->
            val kind = when (section.kind) {
                SectionKind.DEFINITION -> "Определение"
                SectionKind.THEOREM -> "Теорема"
                SectionKind.EXAMPLE -> "Пример"
                SectionKind.SUMMARY -> "Итог"
                SectionKind.TEXT -> ""
            }
            val kindBadge = if (kind.isBlank()) "" else "<div class=\"kind\">${kind.escapeHtml()}</div>"
            val body = section.body.takeIf { it.isNotBlank() }?.let {
                "<p>${it.escapeHtml()}</p>"
            }.orEmpty()
            val formula = section.formula?.let {
                "<div class=\"formula\"><div class=\"math\" data-tex=\"${it.escapeAttr()}\"></div></div>" +
                    (section.formulaLabel?.let { label ->
                        "<div class=\"formula-label\">${label.escapeHtml()}</div>"
                    }.orEmpty())
            }.orEmpty()
            val bullets = if (section.bullets.isEmpty()) "" else {
                "<ul>" + section.bullets.joinToString("") { "<li>${it.escapeHtml()}</li>" } + "</ul>"
            }
            """
            <section class="section">
                $kindBadge
                <h2>${section.title.escapeHtml()}</h2>
                $body
                $formula
                $bullets
            </section>
            """.trimIndent()
        }

        val skipped = if (note.skipped.isEmpty()) "" else """
            <section class="skipped">
                <h2>Отступления преподавателя</h2>
                <p class="muted">Скрыто фрагментов: ${note.skipped.size}</p>
                ${note.skipped.joinToString("") { fragment ->
                    "<div class=\"skipped-item\"><div class=\"time\">${fragment.time}</div>" +
                        "<div><p>${fragment.quote.escapeHtml()}</p><span>${fragment.reason.escapeHtml()}</span></div></div>"
                }}
            </section>
        """.trimIndent()

        val terms = if (note.keyTerms.isEmpty()) "" else {
            "<div class=\"terms\">" + note.keyTerms.joinToString("") {
                "<span>${it.escapeHtml()}</span>"
            } + "</div>"
        }

        val date = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(note.createdAt))

        return """
            <!doctype html>
            <html lang="ru">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=794, initial-scale=1.0">
              <link rel="stylesheet" href="katex.min.css">
              <style>
                @page { size: A4; margin: 18mm 17mm 16mm; }
                * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                body {
                  margin: 0; color: #1B1E18; background: #fff;
                  font-family: Roboto, "Noto Sans", sans-serif; font-size: 10.5pt; line-height: 1.58;
                }
                .brand { display: flex; align-items: center; gap: 8px; color: #3F6348; font-size: 8pt; letter-spacing: 1.6px; text-transform: uppercase; font-weight: 700; }
                .logo { width: 20px; height: 20px; border-radius: 7px; background: #3F6348; position: relative; }
                .logo:after { content:""; position:absolute; left:5px; top:4px; width:10px; height:12px; border-radius:2px; background:#FBF9F4; }
                .subject { margin-top: 18px; color: #4C6B52; font-size: 9.5pt; font-weight: 600; }
                h1 { font-size: 27pt; line-height: 1.12; margin: 7px 0 8px; letter-spacing: -0.8px; color: #162018; }
                .subtitle { color: #5B665A; font-size: 12pt; margin-bottom: 14px; }
                .meta { display: flex; gap: 12px; color: #707A6E; font-size: 8.5pt; border-top: 1px solid #DDE4D9; border-bottom: 1px solid #DDE4D9; padding: 9px 0; }
                .summary { margin-top: 18px; padding: 16px 18px; background: #EDF6E8; border-radius: 12px; }
                .summary .label { color: #3F6348; font-size: 8pt; letter-spacing: 1.2px; text-transform: uppercase; font-weight: 700; margin-bottom: 6px; }
                .summary p { margin: 0; font-size: 11pt; line-height: 1.55; }
                .terms { margin: 12px 0 4px; display: flex; flex-wrap: wrap; gap: 6px; }
                .terms span { border: 1px solid #CFD8CB; border-radius: 20px; padding: 3px 9px; font-size: 8pt; color: #52614F; }
                .section { margin-top: 22px; page-break-inside: avoid; }
                .section h2 { font-size: 14.5pt; line-height: 1.3; margin: 4px 0 7px; color: #1B241B; }
                .kind { color: #4C6B52; font-size: 7.5pt; font-weight: 700; letter-spacing: 1.1px; text-transform: uppercase; }
                p { margin: 0 0 8px; color: #343A32; }
                ul { margin: 6px 0 4px; padding-left: 18px; color: #343A32; }
                li { margin: 4px 0; }
                .formula { margin: 12px 0 6px; padding: 15px 12px; border-radius: 10px; background: #F2F6EF; text-align: center; overflow: hidden; }
                .formula .katex { font-size: 1.2em; }
                .formula-label { text-align: center; color: #788176; font-size: 8pt; margin-bottom: 4px; }
                .skipped { margin-top: 24px; padding: 15px 17px; background: #F7F3EA; border-radius: 12px; page-break-inside: avoid; }
                .skipped h2 { font-size: 12pt; margin: 0 0 3px; color: #4F493A; }
                .muted { color: #857D69; font-size: 8.5pt; margin: 0 0 9px; }
                .skipped-item { display: flex; gap: 9px; border-top: 1px solid #E7E0D2; padding-top: 8px; margin-top: 8px; }
                .skipped-item p { margin: 0 0 2px; font-size: 9pt; color: #625D50; font-style: italic; }
                .skipped-item span { font-size: 7.5pt; color: #9A907A; text-transform: uppercase; letter-spacing: 0.7px; }
                .time { font-size: 8pt; color: #857D69; min-width: 33px; }
                .footer { margin-top: 28px; padding-top: 10px; border-top: 1px solid #DDE4D9; display: flex; justify-content: space-between; color: #8B9387; font-size: 7.5pt; letter-spacing: 0.3px; }
              </style>
            </head>
            <body>
              <div class="brand"><div class="logo"></div><span>CONSPECT · локальный AI-конспект</span></div>
              <div class="subject">${subject.name.escapeHtml()}</div>
              <h1>${note.title.escapeHtml()}</h1>
              ${if (note.subtitle.isNotBlank()) "<div class=\"subtitle\">${note.subtitle.escapeHtml()}</div>" else ""}
              <div class="meta">
                <span>${date}</span><span>${note.durationMinutes} мин записи</span><span>${note.wordCount} слов</span>
              </div>
              <div class="summary">
                <div class="label">Коротко</div>
                <p>${note.summary.escapeHtml()}</p>
              </div>
              $terms
              $sections
              $skipped
              <div class="footer"><span>Conspect · ${subject.shortName.escapeHtml()}</span><span>Создано на устройстве</span></div>
              <script src="katex.min.js"></script>
              <script>
                document.querySelectorAll('[data-tex]').forEach(function (el) {
                  try {
                    katex.render(el.getAttribute('data-tex'), el, {displayMode: true, throwOnError: false, strict: false});
                  } catch (e) { el.textContent = el.getAttribute('data-tex'); }
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun String.escapeHtml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun String.escapeAttr(): String = escapeHtml()
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
        .replace("\n", " ")
}
