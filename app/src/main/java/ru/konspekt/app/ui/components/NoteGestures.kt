package ru.konspekt.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject

/**
 * Selection is local to each lecture list. It is shared by the row gestures and
 * the contextual top bar so a long press feels like Android's own selection UI.
 */
@Stable
class NoteSelectionState {
    var selectedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    val isActive: Boolean get() = selectedIds.isNotEmpty()
    val count: Int get() = selectedIds.size

    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun clear() {
        selectedIds = emptySet()
    }

    fun retainExisting(ids: Collection<String>) {
        if (selectedIds.isEmpty()) return
        val existing = ids.toSet()
        val filtered = selectedIds.intersect(existing)
        if (filtered != selectedIds) selectedIds = filtered
    }
}

@Composable
fun rememberNoteSelectionState(): NoteSelectionState = remember { NoteSelectionState() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = rememberHaptics()
    TopAppBar(
        title = { Text("Выбрано: $count") },
        navigationIcon = {
            IconButton(onClick = {
                haptics.tap()
                onClear()
            }) {
                Icon(Icons.Rounded.Close, contentDescription = "Отменить выбор")
            }
        },
        actions = {
            IconButton(onClick = {
                haptics.tap()
                onExport()
            }) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = "Экспортировать выбранные конспекты в PDF"
                )
            }
            IconButton(onClick = {
                haptics.tap()
                onDelete()
            }) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить выбранные конспекты",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * A note row with Android-style swipe actions:
 * swipe right -> render and share a PDF, swipe left -> move to trash.
 * While selecting, horizontal swipes are disabled and taps toggle selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableNoteRow(
    note: Note,
    subject: Subject,
    selection: NoteSelectionState,
    onClick: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val haptics = rememberHaptics()
    val selected = note.id in selection.selectedIds
    val selectionMode = selection.isActive

    if (selectionMode) {
        NoteRow(
            note = note,
            subject = subject,
            onClick = { selection.toggle(note.id) },
            onLongClick = { selection.toggle(note.id) },
            selected = selected,
            selectionMode = true,
            showDate = showDate,
            modifier = modifier
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.confirm()
                    onSharePdf()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.confirm()
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.34f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(22.dp)),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeActionBackground(direction = dismissState.dismissDirection)
        }
    ) {
        NoteRow(
            note = note,
            subject = subject,
            onClick = onClick,
            onLongClick = {
                selection.toggle(note.id)
            },
            showDate = showDate,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SwipeActionBackground(direction: SwipeToDismissBoxValue) {
    val isShare = direction == SwipeToDismissBoxValue.StartToEnd
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    if (!isShare && !isDelete) {
        Box(Modifier.fillMaxSize())
        return
    }
    val container = if (isShare) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val content = if (isShare) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
    val label = if (isShare) "PDF" else "В корзину"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .padding(horizontal = 22.dp),
        contentAlignment = if (isShare) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isShare) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
        }
    }
}
