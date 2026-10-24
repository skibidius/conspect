package ru.konspekt.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject
import ru.konspekt.app.ui.components.EmptyState
import ru.konspekt.app.ui.components.NoteSelectionTopBar
import ru.konspekt.app.ui.components.SwipeableNoteRow
import ru.konspekt.app.ui.components.rememberNoteSelectionState
import ru.konspekt.app.ui.components.SectionHeader
import ru.konspekt.app.ui.components.UserAvatar
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.util.notesCountText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    subjects: List<Subject>,
    notes: List<Note>,
    notesCount: (String) -> Int,
    onOpenNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onDeleteNotes: (List<String>) -> Unit,
    onSharePdf: (String) -> Unit,
    onExportNotes: (List<String>) -> Unit,
    onOpenSubject: (String) -> Unit,
    onAllSubjects: () -> Unit,
    onAllNotes: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onStartRecording: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = rememberDateText()
    val haptics = rememberHaptics()
    val selection = rememberNoteSelectionState()
    val recent = notes.take(4)

    LaunchedEffect(recent.map { it.id }) {
        selection.retainExisting(recent.map { it.id })
    }

    fun finishSelection(action: (List<String>) -> Unit) {
        val ids = selection.selectedIds.toList()
        selection.clear()
        if (ids.isNotEmpty()) action(ids)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selection.isActive) {
            NoteSelectionTopBar(
                count = selection.count,
                onClear = selection::clear,
                onExport = { finishSelection(onExportNotes) },
                onDelete = { finishSelection(onDeleteNotes) }
            )
        } else {
        TopAppBar(
            title = {
                Text("Conspect", style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                IconButton(onClick = {
                    haptics.tap()
                    onSearch()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Поиск",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UserAvatar(
                    name = userName,
                    modifier = Modifier.clickable {
                        haptics.tap()
                        onProfile()
                    }
                )
                Spacer(Modifier.width(12.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 160.dp
            )
        ) {
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)) {
                    Text(
                        text = dateText.uppercase(Locale("ru")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Добрый день, $userName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Готовы записать новую лекцию?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                RecordHeroCard(
                    hasSubjects = subjects.isNotEmpty(),
                    noteCount = notes.size,
                    onStart = { onStartRecording(null) },
                    onAddSubjects = onAllSubjects
                )
            }

            item {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Предметы",
                    action = "Все",
                    onAction = onAllSubjects
                )
                Spacer(Modifier.height(12.dp))
            }

            if (subjects.isEmpty()) {
                item {
                    AddFirstSubjectCard(onClick = onAllSubjects)
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 4.dp)
                    ) {
                        items(subjects, key = { it.id }) { subject ->
                            SubjectMiniCard(
                                subject = subject,
                                noteCount = notesCount(subject.id),
                                onClick = { onOpenSubject(subject.id) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Недавние конспекты",
                    action = "Все",
                    onAction = onAllNotes
                )
                Spacer(Modifier.height(12.dp))
            }

            if (notes.isEmpty()) {
                item {
                    EmptyState(
                        title = "Пока нет конспектов",
                        subtitle = "Запишите первую лекцию — конспект появится здесь."
                    )
                }
            } else {
                items(recent, key = { it.id }) { note ->
                    val subject = subjects.firstOrNull { it.id == note.subjectId }
                    if (subject != null) {
                        SwipeableNoteRow(
                            note = note,
                            subject = subject,
                            selection = selection,
                            onClick = { onOpenNote(note.id) },
                            onSharePdf = { onSharePdf(note.id) },
                            onDelete = { onDeleteNote(note.id) },
                            modifier = Modifier.animateItem().padding(bottom = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordHeroCard(
    hasSubjects: Boolean,
    noteCount: Int,
    onStart: () -> Unit,
    onAddSubjects: () -> Unit
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Записать лекцию",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasSubjects) {
                    "Conspect прослушает лекцию и соберёт структурированный конспект с формулами LaTeX."
                } else {
                    "Добавьте предмет, чтобы начать запись."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    haptics.tap()
                    if (hasSubjects) onStart() else onAddSubjects()
                },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (hasSubjects) Icons.Rounded.Mic else Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (hasSubjects) "Начать запись" else "Добавить предмет")
            }
        }
    }
}

@Composable
private fun SubjectMiniCard(
    subject: Subject,
    noteCount: Int,
    onClick: () -> Unit
) {
    val dark = LocalKonspektDark.current
    val container = subject.tone.container(dark)
    val onContainer = subject.tone.onContainer(dark)
    val haptics = rememberHaptics()
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier
            .width(156.dp)
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        color = container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = subject.icon.vector,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = onContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleSmall,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = notesCountText(noteCount),
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun AddFirstSubjectCard(onClick: () -> Unit) {
    val haptics = rememberHaptics()
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Добавьте первый предмет, чтобы вести конспекты",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun rememberDateText(): String {
    val raw = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
}
