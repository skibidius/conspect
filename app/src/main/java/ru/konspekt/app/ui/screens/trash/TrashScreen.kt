package ru.konspekt.app.ui.screens.trash

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject
import ru.konspekt.app.data.SubjectIcon
import ru.konspekt.app.data.SubjectTone
import ru.konspekt.app.ui.components.EmptyState
import ru.konspekt.app.ui.components.SubjectIconBadge
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.util.notesCountText

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    notes: List<Note>,
    subjects: List<Subject>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Корзина") },
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
                if (notes.isNotEmpty()) {
                    TextButton(onClick = {
                        haptics.tap()
                        confirmEmpty = true
                    }) {
                        Text("Очистить")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (notes.isEmpty()) {
            EmptyState(
                title = "Корзина пуста",
                subtitle = "Сюда попадают удалённые конспекты. Их можно вернуть."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${notesCountText(notes.size)} в корзине",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(notes, key = { it.id }) { note ->
                    TrashRow(
                        note = note,
                        subject = subjectFor(note, subjects),
                        onRestore = {
                            haptics.confirm()
                            onRestore(note.id)
                        },
                        onDeleteForever = {
                            haptics.tap()
                            pendingDelete = note
                        }
                    )
                }
            }
        }
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Удалить навсегда?") },
            text = { Text("«${note.title}» больше нельзя будет восстановить.") },
            confirmButton = {
                TextButton(onClick = {
                    haptics.confirm()
                    onDeleteForever(note.id)
                    pendingDelete = null
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tap()
                    pendingDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Очистить корзину?") },
            text = { Text("Все конспекты в корзине будут удалены навсегда.") },
            confirmButton = {
                TextButton(onClick = {
                    haptics.confirm()
                    onEmptyTrash()
                    confirmEmpty = false
                }) {
                    Text("Очистить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tap()
                    confirmEmpty = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TrashRow(
    note: Note,
    subject: Subject,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubjectIconBadge(subject = subject, size = 50.dp, corner = 16.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = "Восстановить",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDeleteForever) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = "Удалить навсегда",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun subjectFor(note: Note, subjects: List<Subject>): Subject =
    subjects.firstOrNull { it.id == note.subjectId } ?: Subject(
        id = note.subjectId,
        name = "Без предмета",
        shortName = "Без предмета",
        teacher = "",
        nextClass = "",
        tone = SubjectTone.SAGE,
        icon = SubjectIcon.FUNCTIONS
    )
