package ru.konspekt.app.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.Subject
import ru.konspekt.app.ui.components.EmptyState
import ru.konspekt.app.ui.components.NoteRow
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
    onRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalKonspektDark.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text(subject.shortName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            actions = {
                IconButton(onClick = onRecord) {
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
                                    text = subject.teacher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subject.tone.onContainer(dark).copy(alpha = 0.72f)
                                )
                                Spacer(Modifier.size(14.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Apartment,
                                    contentDescription = null,
                                    tint = subject.tone.onContainer(dark).copy(alpha = 0.65f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.size(5.dp))
                                Text(
                                    text = "ауд. ${subject.room}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subject.tone.onContainer(dark).copy(alpha = 0.72f)
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = subject.tone.onContainer(dark).copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = subject.tone.onContainer(dark),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.size(5.dp))
                                        Text(
                                            text = subject.nextClass,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = subject.tone.onContainer(dark)
                                        )
                                    }
                                }
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
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRecord,
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
}
