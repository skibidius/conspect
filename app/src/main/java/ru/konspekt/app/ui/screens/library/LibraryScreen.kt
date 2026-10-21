package ru.konspekt.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject
import ru.konspekt.app.ui.components.EmptyState
import ru.konspekt.app.ui.components.NoteRow
import ru.konspekt.app.ui.components.UserAvatar
import ru.konspekt.app.ui.components.Wordmark
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.util.notesCountText

@Composable
fun LibraryScreen(
    notes: List<Note>,
    subjects: List<Subject>,
    onOpenNote: (String) -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var newestFirst by remember { mutableStateOf(true) }

    val filtered = notes
        .filter { selectedSubject == null || it.subjectId == selectedSubject }
        .let { list -> if (newestFirst) list.sortedByDescending { it.createdAt } else list.sortedByDescending { it.durationMinutes } }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 10.dp,
            bottom = 130.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Wordmark()
                Spacer(Modifier.weight(1f))
                UserAvatar(name = "Аня", modifier = Modifier.clickable(onClick = onProfile))
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 18.dp, bottom = 18.dp)) {
                Text(
                    text = "Библиотека",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "${notesCountText(notes.size)} · всё на устройстве",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Surface(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Найти тему, формулу или предмет",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LibraryChip(
                        text = "Все",
                        selected = selectedSubject == null,
                        onClick = { selectedSubject = null }
                    )
                }
                items(subjects, key = { it.id }) { subject ->
                    LibraryChip(
                        text = subject.shortName,
                        selected = selectedSubject == subject.id,
                        onClick = {
                            selectedSubject = if (selectedSubject == subject.id) null else subject.id
                        }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (selectedSubject == null) "Все конспекты" else "Конспекты",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${filtered.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                IconButton(onClick = { newestFirst = !newestFirst }) {
                    Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = if (newestFirst) "Сначала новые" else "Сначала длинные",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = "Ничего не найдено",
                    subtitle = "Попробуйте выбрать другой предмет."
                )
            }
        } else {
            items(filtered, key = { it.id }) { note ->
                val subject = subjects.firstOrNull { it.id == note.subjectId } ?: return@items
                NoteRow(
                    note = note,
                    subject = subject,
                    onClick = { onOpenNote(note.id) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
        )
    }
}
