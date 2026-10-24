package ru.konspekt.app.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject
import ru.konspekt.app.data.SubjectIcon
import ru.konspekt.app.data.SubjectTone
import ru.konspekt.app.ui.components.EmptyState
import ru.konspekt.app.ui.components.NoteSelectionTopBar
import ru.konspekt.app.ui.components.SwipeableNoteRow
import ru.konspekt.app.ui.components.rememberNoteSelectionState
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.util.notesCountText

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: Subject,
    notes: List<Note>,
    totalMinutes: Int,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onDeleteNotes: (List<String>) -> Unit,
    onSharePdf: (String) -> Unit,
    onExportNotes: (List<String>) -> Unit,
    onRecord: () -> Unit,
    onUpdateSubject: (String, String, SubjectTone, SubjectIcon) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalKonspektDark.current
    val haptics = rememberHaptics()
    var showEdit by remember { mutableStateOf(false) }
    val selection = rememberNoteSelectionState()

    LaunchedEffect(notes.map { it.id }) {
        selection.retainExisting(notes.map { it.id })
    }

    fun finishSelection(action: (List<String>) -> Unit) {
        val ids = selection.selectedIds.toList()
        selection.clear()
        if (ids.isNotEmpty()) action(ids)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        if (selection.isActive) {
            NoteSelectionTopBar(
                count = selection.count,
                onClear = selection::clear,
                onExport = { finishSelection(onExportNotes) },
                onDelete = { finishSelection(onDeleteNotes) }
            )
        } else {
        TopAppBar(
            title = { Text(subject.shortName) },
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
                IconButton(onClick = {
                    haptics.tap()
                    showEdit = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Редактировать предмет",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    haptics.tap()
                    onRecord()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Записать лекцию",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        }

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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = subject.tone.container(dark)
                ) {
                    Box {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(subject.tone.onContainer(dark).copy(alpha = 0.09f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = subject.icon.vector,
                                    contentDescription = null,
                                    tint = subject.tone.onContainer(dark),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = subject.tone.onContainer(dark)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = subject.tone.onContainer(dark).copy(alpha = 0.65f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.size(5.dp))
                                Text(
                                    text = subject.teacher.ifBlank { "Преподаватель не указан" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subject.tone.onContainer(dark).copy(alpha = 0.72f)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Surface(
                                shape = CircleShape,
                                color = subject.tone.onContainer(dark).copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "${notesCountText(notes.size)} · $totalMinutes мин",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subject.tone.onContainer(dark),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        haptics.tap()
                        onRecord()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Записать новую лекцию")
                }
            }

            item {
                Spacer(Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Конспекты",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${notes.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (notes.isEmpty()) {
                item {
                    EmptyState(
                        title = "Пока пусто",
                        subtitle = "Запишите первую лекцию — конспект появится здесь."
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
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

    if (showEdit) {
        EditSubjectDialog(
            subject = subject,
            onDismiss = { showEdit = false },
            onConfirm = { name, teacher, tone, icon ->
                onUpdateSubject(name, teacher, tone, icon)
                showEdit = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSubjectDialog(
    subject: Subject,
    onDismiss: () -> Unit,
    onConfirm: (String, String, SubjectTone, SubjectIcon) -> Unit
) {
    var name by remember { mutableStateOf(subject.name) }
    var teacher by remember { mutableStateOf(subject.teacher) }
    var tone by remember { mutableStateOf(subject.tone) }
    var icon by remember { mutableStateOf(subject.icon) }
    val haptics = rememberHaptics()
    val icons = SubjectIcon.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Редактировать предмет") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("Преподаватель") },
                    placeholder = { Text("например, доц. Соколова") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Цвет",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubjectTone.entries.forEach { item ->
                        val dark = LocalKonspektDark.current
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(item.container(dark))
                                .clickable {
                                    haptics.tap()
                                    tone = item
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (tone == item) {
                                Text(
                                    text = "✓",
                                    color = item.onContainer(dark),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Иконка",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icons.forEach { item ->
                        val selected = icon == item
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable {
                                    haptics.tap()
                                    icon = item
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.vector,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.confirm()
                    onConfirm(name.trim(), teacher.trim(), tone, icon)
                },
                enabled = name.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.tap()
                onDismiss()
            }) { Text("Отмена") }
        }
    )
}
