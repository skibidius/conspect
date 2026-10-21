package ru.konspekt.app.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.konspekt.app.data.Note
import ru.konspekt.app.data.ScheduleItem
import ru.konspekt.app.data.Subject
import ru.konspekt.app.ui.components.DurationPill
import ru.konspekt.app.ui.components.MetaSeparator
import ru.konspekt.app.ui.components.NoteRow
import ru.konspekt.app.ui.components.SectionHeader
import ru.konspekt.app.ui.components.SubjectIconBadge
import ru.konspekt.app.ui.components.KonspektLogo
import ru.konspekt.app.ui.components.UserAvatar
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
    schedule: List<ScheduleItem>,
    notesCount: (String) -> Int,
    onOpenNote: (String) -> Unit,
    onOpenSubject: (String) -> Unit,
    onAllSubjects: () -> Unit,
    onAllNotes: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    onStartRecording: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = rememberDateText()
    val current = schedule.firstOrNull { it.isNow } ?: schedule.firstOrNull()
    val currentSubject = current?.let { item -> subjects.firstOrNull { it.id == item.subjectId } }
    val next = schedule.dropWhile { it.isNow }.firstOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KonspektLogo(size = 30.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Conspect",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Поиск",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UserAvatar(
                    name = userName,
                    modifier = Modifier
                        .clickable(onClick = onProfile)
                        .padding(end = 12.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 160.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)) {
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
                        text = "Сегодня 3 пары. Начнём с математики?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        item {
            current?.let { item ->
                currentSubject?.let { subject ->
                    HeroLectureCard(
                        subject = subject,
                        topic = item.topic,
                        time = "${item.time}–${item.endTime}",
                        room = item.room,
                        onStart = { onStartRecording(subject.id) }
                    )
                }
            }
        }

        if (next != null) {
            val nextSubject = subjects.firstOrNull { it.id == next.subjectId }
            if (nextSubject != null) {
                item {
                    Spacer(Modifier.height(14.dp))
                    NextClassStrip(
                        subject = nextSubject,
                        item = next,
                        onClick = { onOpenSubject(nextSubject.id) }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader(
                title = "Предметы",
                action = "Все",
                onAction = onAllSubjects
            )
            Spacer(Modifier.height(12.dp))
        }

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

        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader(
                title = "Недавние конспекты",
                action = "Все",
                onAction = onAllNotes
            )
            Spacer(Modifier.height(12.dp))
        }

        items(notes.take(3), key = { it.id }) { note ->
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
private fun HeroLectureCard(
    subject: Subject,
    topic: String,
    time: String,
    room: String,
    onStart: () -> Unit
) {
    val dark = LocalKonspektDark.current
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(subject.tone.accent(dark))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "СЕЙЧАС · $time",
                            style = MaterialTheme.typography.labelMedium,
                            color = onContainer
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                LiveWaveBars(
                    color = onContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(width = 44.dp, height = 22.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = subject.name,
                style = MaterialTheme.typography.headlineSmall,
                color = onContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = topic,
                style = MaterialTheme.typography.bodyLarge,
                color = onContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ауд. $room",
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.68f)
                )
                Spacer(Modifier.width(8.dp))
                MetaSeparator(color = onContainer.copy(alpha = 0.4f))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = subject.teacher,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.68f)
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Начать запись")
            }
        }
    }
}

@Composable
private fun LiveWaveBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "hero-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val bars = 5
        val gap = size.width / (bars * 2f)
        val barWidth = gap * 0.72f
        repeat(bars) { index ->
            val factor = (kotlin.math.sin(phase + index * 0.9f) + 1f) / 2f
            val height = size.height * (0.28f + factor * 0.68f)
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap) + gap / 2f, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
            )
        }
    }
}

@Composable
private fun NextClassStrip(
    subject: Subject,
    item: ScheduleItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ДАЛЬШЕ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.width(14.dp))
            SubjectIconBadge(subject = subject, size = 42.dp, corner = 14.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.topic} · ауд. ${item.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
private fun SubjectMiniCard(
    subject: Subject,
    noteCount: Int,
    onClick: () -> Unit
) {
    val dark = LocalKonspektDark.current
    val container = subject.tone.container(dark)
    val onContainer = subject.tone.onContainer(dark)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(158.dp)
            .height(148.dp),
        shape = RoundedCornerShape(24.dp),
        color = container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(onContainer.copy(alpha = 0.09f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = subject.icon.vector,
                            contentDescription = null,
                            tint = onContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = onContainer.copy(alpha = 0.52f),
                        modifier = Modifier.size(17.dp)
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
private fun rememberDateText(): String {
    val raw = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
}
