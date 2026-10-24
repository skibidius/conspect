package ru.konspekt.app.ui.screens.importaudio

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.konspekt.app.data.NoteGenerationResult
import ru.konspekt.app.data.Subject
import ru.konspekt.app.recording.AudioFileTranscriber
import ru.konspekt.app.recording.AudioTranscriptionProgress
import ru.konspekt.app.recording.AudioTranscriptionStage
import ru.konspekt.app.ui.components.KonspektLogo
import ru.konspekt.app.ui.components.rememberHaptics

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportAudioScreen(
    subject: Subject,
    topic: String,
    uri: Uri,
    onClose: () -> Unit,
    onFinish: suspend (lines: List<String>, durationSeconds: Int) -> NoteGenerationResult,
    onCompleted: (NoteGenerationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var update by remember {
        mutableStateOf(
            AudioTranscriptionProgress(
                stage = AudioTranscriptionStage.MODEL,
                fraction = 0f
            )
        )
    }

    var error by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        if (started) return@LaunchedEffect
        started = true
        val transcription = runCatching {
            AudioFileTranscriber.transcribe(
                context = context,
                uri = uri,
                onProgress = { update = it }
            )
        }.getOrElse { throwable ->
            error = throwable.message ?: "Не удалось прочитать аудио"
            null
        } ?: return@LaunchedEffect

        if (transcription.lines.isEmpty()) {
            error = "Речь не распознана. Попробуйте файл с более чистым звуком."
            return@LaunchedEffect
        }
        update = update.copy(
            stage = AudioTranscriptionStage.FINALIZING,
            fraction = 0.995f,
            stageFraction = 1f,
            processedSeconds = transcription.durationSeconds,
            totalSeconds = transcription.durationSeconds
        )
        val result = runCatching {
            onFinish(transcription.lines, transcription.durationSeconds)
        }.getOrElse {
            error = "Не удалось собрать конспект"
            null
        }
        if (result != null) onCompleted(result)
    }

    val progress = update.fraction.coerceIn(0f, 1f)
    val stageTitle = when (update.stage) {
        AudioTranscriptionStage.MODEL -> "Загружаем модель GigaAM"
        AudioTranscriptionStage.PREPARING -> "Подготавливаем аудио"
        AudioTranscriptionStage.RECOGNIZING -> "Расшифровываем лекцию"
        AudioTranscriptionStage.FINALIZING -> "Собираем конспект"
    }
    val detail = when (update.stage) {
        AudioTranscriptionStage.MODEL ->
            if (update.stageFraction >= 1f) {
                "Модель готова, загружаем её в память…"
            } else {
                "Скачано ${(update.stageFraction * 100).toInt()}% модели"
            }
        AudioTranscriptionStage.PREPARING ->
            if (update.totalSeconds > 0) {
                "Длительность: ${formatDuration(update.totalSeconds)}"
            } else {
                "Читаем аудиодорожку…"
            }
        AudioTranscriptionStage.RECOGNIZING -> {
            val processed = formatDuration(update.processedSeconds)
            val total = if (update.totalSeconds > 0) {
                " из ${formatDuration(update.totalSeconds)}"
            } else {
                ""
            }
            val eta = update.etaSeconds?.let { " · осталось ~${formatEta(it)}" }.orEmpty()
            "Обработано $processed$total$eta"
        }
        AudioTranscriptionStage.FINALIZING ->
            "Структурируем тезисы и сохраняем конспект"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Импорт лекции") },
            navigationIcon = {
                IconButton(onClick = {
                    haptics.tap()
                    onClose()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KonspektLogo(size = 72.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Расшифровываем аудио",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${subject.shortName} · ${topic.ifBlank { "Новая лекция" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AudioFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(
                                text = if (error == null) "Офлайн-распознавание" else "Ошибка",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (error == null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error
                            )
                            if (error == null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stageTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (error == null) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (error == null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Обработка идёт на телефоне. Прогресс рассчитан по длительности аудио.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                haptics.tap()
                                onClose()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Закрыть")
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Используется модель GigaAM через sherpa-onnx. Аудио не отправляется в интернет.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return if (safe >= 3600) {
        "%d:%02d:%02d".format(safe / 3600, (safe % 3600) / 60, safe % 60)
    } else {
        "%d:%02d".format(safe / 60, safe % 60)
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
