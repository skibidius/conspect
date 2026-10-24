package ru.konspekt.app.ui.screens.note

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.LatexAutoWrap
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.hrm.latex.renderer.model.LatexThemeColors
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.NoteSection
import ru.konspekt.app.data.SectionKind
import ru.konspekt.app.data.Subject
import ru.konspekt.app.pdf.PdfExporter
import ru.konspekt.app.ui.components.AiPill
import ru.konspekt.app.ui.components.DividerLine
import ru.konspekt.app.ui.components.SubjectPill
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.util.fragmentsHiddenText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    note: Note,
    subject: Subject,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val dark = LocalKonspektDark.current
    val haptics = rememberHaptics()
    var menuOpen by remember { mutableStateOf(false) }
    var showLatex by remember { mutableStateOf(false) }
    var skippedExpanded by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun shareNote() {
        haptics.tap()
        val text = buildString {
            appendLine(note.title)
            appendLine(subject.name)
            appendLine()
            appendLine(note.summary)
            note.sections.forEach { section ->
                appendLine()
                appendLine(section.title)
                if (section.body.isNotBlank()) appendLine(section.body)
                if (section.formula != null) appendLine(section.formula)
            }
            appendLine()
            append("Создано в Conspect")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться конспектом"))
    }

    fun exportPdf() {
        if (exporting) return
        haptics.confirm()
        exporting = true
        PdfExporter.export(context, note, subject) { file, message ->
            exporting = false
            onMessage(message)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Conspect") },
            navigationIcon = {
                IconButton(onClick = {
                    haptics.tap()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            actions = {
                IconButton(onClick = { shareNote() }) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { exportPdf() }, enabled = !exporting) {
                    if (exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "Экспорт в PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Box {
                    IconButton(onClick = {
                        haptics.tap()
                        menuOpen = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Ещё",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Показать LaTeX") },
                            onClick = {
                                haptics.tap()
                                menuOpen = false
                                showLatex = true
                            },
                            leadingIcon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Экспорт в PDF") },
                            onClick = {
                                haptics.tap()
                                menuOpen = false
                                exportPdf()
                            },
                            leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Скопировать текст") },
                            onClick = {
                                haptics.tap()
                                menuOpen = false
                                clipboard.setText(AnnotatedString(note.summary))
                                onMessage("Текст скопирован")
                            },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp
            )
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectPill(subject)
                    AiPill()
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = note.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (note.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = note.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${note.timeLabel} · ${note.durationMinutes} мин записи",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "${note.wordCount} слов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "КОРОТКО",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = note.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (note.keyTerms.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        note.keyTerms.forEach { term ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
            }

            items(note.sections.size) { index ->
                NoteSectionBlock(
                    index = index + 1,
                    section = note.sections[index],
                    onCopyFormula = { formula ->
                        haptics.tap()
                        clipboard.setText(AnnotatedString(formula))
                        onMessage("Формула скопирована")
                    }
                )
                Spacer(Modifier.height(4.dp))
            }

            if (note.skipped.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(18.dp))
                    SkippedCard(
                        note = note,
                        expanded = skippedExpanded,
                        onToggle = {
                            haptics.tap()
                            skippedExpanded = !skippedExpanded
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
                DividerLine()
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { exportPdf() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(17.dp),
                        enabled = !exporting
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Готовим файл…")
                        } else {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Экспорт в PDF")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            showLatex = true
                        },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("LaTeX")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = "Создано локально из ${note.durationMinutes} мин аудио",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showLatex) {
        LatexDialog(
            note = note,
            onDismiss = { showLatex = false },
            onCopy = {
                clipboard.setText(AnnotatedString(note.latexSource))
                onMessage("LaTeX-исходник скопирован")
            }
        )
    }
}

@Composable
private fun NoteSectionBlock(
    index: Int,
    section: NoteSection,
    onCopyFormula: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%02d".format(index),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .height(1.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            )
            if (section.kind != SectionKind.TEXT) {
                Spacer(Modifier.size(10.dp))
                Text(
                    text = when (section.kind) {
                        SectionKind.DEFINITION -> "ОПРЕДЕЛЕНИЕ"
                        SectionKind.THEOREM -> "ТЕОРЕМА"
                        SectionKind.EXAMPLE -> "ПРИМЕР"
                        SectionKind.SUMMARY -> "ИТОГ"
                        SectionKind.TEXT -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (section.kind) {
                        SectionKind.EXAMPLE -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    letterSpacing = 0.6.sp
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (section.body.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
            )
        }
        if (section.formula != null) {
            Spacer(Modifier.height(12.dp))
            FormulaCard(
                formula = section.formula,
                label = section.formulaLabel,
                onCopy = { onCopyFormula(section.formula) }
            )
        }
        if (section.bullets.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                section.bullets.forEach { bullet ->
                    Row {
                        if (section.kind == SectionKind.SUMMARY) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = bullet,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun FormulaCard(
    formula: String,
    label: String?,
    onCopy: () -> Unit
) {
    val latexColor = MaterialTheme.colorScheme.onSurface
    val latexCard = MaterialTheme.colorScheme.surfaceContainerLow
    val latexTheme = LatexTheme.auto(
        light = LatexThemeColors(color = latexColor, backgroundColor = latexCard),
        dark = LatexThemeColors(color = latexColor, backgroundColor = latexCard)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    text = "LATEX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Скопировать формулу",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LatexAutoWrap(
                    latex = formula,
                    modifier = Modifier.fillMaxWidth(),
                    config = LatexConfig(
                        fontSize = 20.sp,
                        theme = latexTheme,
                        accessibilityEnabled = true
                    )
                )
            }
            if (!label.isNullOrBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SkippedCard(
    note: Note,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SubdirectoryArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Отступления преподавателя",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${fragmentsHiddenText(note.skipped.size)} из конспекта",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Показать",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    note.skipped.forEachIndexed { index, fragment ->
                        if (index > 0) {
                            Spacer(Modifier.height(10.dp))
                        }
                        Row {
                            Text(
                                text = fragment.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "«${fragment.quote}»",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = fragment.reason.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LatexDialog(
    note: Note,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text("LaTeX-исходник")
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.latexSource.ifBlank { "% LaTeX появится после генерации" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy()
                    onDismiss()
                }
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(6.dp))
                Text("Скопировать")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
