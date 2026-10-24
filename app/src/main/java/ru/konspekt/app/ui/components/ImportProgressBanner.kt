package ru.konspekt.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.AudioImportState
import ru.konspekt.app.recording.AudioTranscriptionStage

@Composable
fun ImportProgressBanner(
    state: AudioImportState.Running,
    onClick: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val progress = state.progress.fraction.coerceIn(0f, 1f)
    val remaining = state.progress.etaSeconds
    val detail = when {
        remaining != null -> "Осталось ~${formatEta(remaining)}"
        state.progress.stage == AudioTranscriptionStage.MODEL -> "Осталось: готовим модель…"
        state.progress.stage == AudioTranscriptionStage.PREPARING -> "Осталось: читаем аудио…"
        state.progress.stage == AudioTranscriptionStage.FINALIZING -> "Осталось: собираем конспект…"
        else -> "Осталось: оцениваем…"
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Расшифровываем лекцию",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        haptics.confirm()
                        onStop()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.StopCircle,
                        contentDescription = "Остановить обработку",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}

private fun formatEta(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return when {
        safe >= 3600 -> "${safe / 3600} ч ${(safe % 3600 + 59) / 60} мин"
        safe >= 60 -> "${(safe + 59) / 60} мин"
        else -> "меньше минуты"
    }
}
