package ru.konspekt.app.ui.screens.subjects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.konspekt.app.data.Subject
import ru.konspekt.app.data.SubjectIcon
import ru.konspekt.app.data.SubjectTone
import ru.konspekt.app.ui.components.SubjectIconBadge
import ru.konspekt.app.ui.components.UserAvatar
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.util.notesCountText
import ru.konspekt.app.util.subjectsCountText

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    notesCount: (String) -> Int,
    totalMinutes: (String) -> Int,
    onOpenSubject: (String) -> Unit,
    onAddSubject: (String, SubjectTone, SubjectIcon) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    val totalNotes = subjects.sumOf { notesCount(it.id) }
    val haptics = rememberHaptics()

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = { Text("Предметы") },
            actions = {
                IconButton(onClick = {
                    haptics.tap()
                    showAddDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Добавить предмет",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UserAvatar(
                    name = "Аня",
                    modifier = Modifier
                        .clickable {
                            haptics.tap()
                            onProfile()
                        }
                        .padding(end = 12.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 160.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "${subjectsCountText(subjects.size)} · ${notesCountText(totalNotes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (subjects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Добавьте первый предмет, чтобы вести конспекты.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SemesterSummaryCard(
                        subjectsCount = subjects.size,
                        notesCount = totalNotes,
                        minutes = subjects.sumOf { totalMinutes(it.id) }
                    )
                }
            }

            items(subjects, key = { it.id }) { subject ->
            SubjectGridCard(
                subject = subject,
                noteCount = notesCount(subject.id),
                minutes = totalMinutes(subject.id),
                onClick = { onOpenSubject(subject.id) },
                onLongClick = { subjectToDelete = subject }
            )
        }

            item {
                AddSubjectCard(onClick = { showAddDialog = true })
            }
        }
    }

    if (showAddDialog) {
        AddSubjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, tone, icon ->
                onAddSubject(name, tone, icon)
                showAddDialog = false
            }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Удалить предмет?") },
            text = {
                Text("«${subject.name}» и все его конспекты будут удалены безвозвратно.")
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.confirm()
                    onDeleteSubject(subject.id)
                    subjectToDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tap()
                    subjectToDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SemesterSummaryCard(
    subjectsCount: Int,
    notesCount: Int,
    minutes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "ЛОКАЛЬНАЯ СВОДКА",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${notesCountText(notesCount)} за семестр",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${minutes / 60} ч ${minutes % 60} мин лекций сохранено",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$subjectsCount",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectGridCard(
    subject: Subject,
    noteCount: Int,
    minutes: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dark = LocalKonspektDark.current
    val container = subject.tone.container(dark)
    val onContainer = subject.tone.onContainer(dark)
    val haptics = rememberHaptics()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
            .combinedClickable(
                onClick = {
                    haptics.tap()
                    onClick()
                },
                onLongClick = {
                    haptics.confirm()
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(24.dp),
        color = container
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(onContainer.copy(alpha = 0.09f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = subject.icon.vector,
                            contentDescription = null,
                            tint = onContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = onContainer.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${notesCountText(noteCount)} · ${minutes} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.66f)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = onContainer.copy(alpha = 0.66f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = subject.nextClass,
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSubjectCard(onClick: () -> Unit) {
    val haptics = rememberHaptics()
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Новый предмет",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Создать курс",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, SubjectTone, SubjectIcon) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(SubjectTone.SAGE) }
    var icon by remember { mutableStateOf(SubjectIcon.FUNCTIONS) }
    val haptics = rememberHaptics()
    val icons = listOf(
        SubjectIcon.FUNCTIONS,
        SubjectIcon.SCIENCE,
        SubjectIcon.TERMINAL,
        SubjectIcon.LANDMARK,
        SubjectIcon.LANGUAGE,
        SubjectIcon.BIOTECH
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Новый предмет") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
                                .clip(CircleShape)
                                .background(item.container(dark))
                                .then(
                                    if (tone == item) {
                                        Modifier.border(2.dp, item.accent(dark), CircleShape)
                                    } else Modifier
                                )
                                .clickable {
                                    haptics.tap()
                                    tone = item
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (tone == item) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(item.accent(dark))
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    onConfirm(name, tone, icon)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.tap()
                onDismiss()
            }) { Text("Отмена") }
        }
    )
}
