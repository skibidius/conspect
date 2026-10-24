package ru.konspekt.app.ui.screens.importaudio

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
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
import ru.konspekt.app.data.AudioImportState
import ru.konspekt.app.data.Subject
import ru.konspekt.app.recording.AudioTranscriptionProgress
import ru.konspekt.app.recording.AudioTranscriptionStage
import ru.konspekt.app.ui.components.KonspektLogo
import ru.konspekt.app.ui.components.rememberHaptics
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportAudioScreen(
    subject: Subject,
    topic: String,
    state: AudioImportState,
    onClose: () -> Unit,
    onCompleted: (AudioImportState.Completed) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()

    BackHandler(onBack = onClose)
    LaunchedEffect(state) {
        if (state is AudioImportState.Completed) {
            onCompleted(state)
        }
    }

    val running = (state as? AudioImportState.Running)?.progress
    val failed = state as? AudioImportState.Failed
    val completed = state is AudioImportState.Completed
    var elapsedTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val runningStartedAt = (state as? AudioImportState.Running)?.startedAtMillis
    LaunchedEffect(runningStartedAt) {
        while (runningStartedAt != null) {
            elapsedTick = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val elapsedSeconds = runningStartedAt?.let {
        ((elapsedTick - it) / 1000).coerceAtLeast(0).toInt()
    }
    val elapsedLabel = elapsedSeconds?.let { "Прошло ${formatDuration(it)}" }
    val progress = when {
        completed -> 1f
        running != null -> running.fraction.coerceIn(0f, 1f)
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(320),
        label = "import-progress"
    )
    val stageTitle = when {
        completed -> "Конспект готов"
        failed != null -> "Ошибка"
        running?.stage == AudioTranscriptionStage.MODEL -> "Загружаем модель GigaAM"
        running?.stage == AudioTranscriptionStage.PREPARING -> "Подготавливаем аудио"
        running?.stage == AudioTranscriptionStage.RECOGNIZING -> "Расшифровываем лекцию"
        running?.stage == AudioTranscriptionStage.FINALIZING -> "Собираем конспект"
        else -> "Подготавливаем аудио"
    }
    val detail = when {
        completed -> "Сохраняем результат…"
        failed != null -> failed.message
        running == null -> "Читаем аудиодорожку…"
        running.stage == AudioTranscriptionStage.MODEL -> {
            val model = if (running.stageFraction >= 1f) {
                "Модель готова, загружаем в память…"
            } else {
                "Скачано ${(running.stageFraction * 100).toInt()}% модели"
            }
            elapsedLabel?.let { "$it · $model" } ?: model
        }
        running.stage == AudioTranscriptionStage.PREPARING -> {
            val prep = if (running.totalSeconds > 0) {
                "Длительность: ${formatDuration(running.totalSeconds)}"
            } else {
                "Читаем аудиодорожку…"
            }
            elapsedLabel?.let { "$it · $prep" } ?: prep
        }
        running.stage == AudioTranscriptionStage.RECOGNIZING -> {
            val processed = formatDuration(running.processedSeconds)
            val total = if (running.totalSeconds > 0) {
                " из ${formatDuration(running.totalSeconds)}"
            } else {
                ""
            }
            val eta = running.etaSeconds?.let { " · осталось ~${formatEta(it)}" }.orEmpty()
            val elapsed = elapsedLabel?.let { "$it · " }.orEmpty()
            "$elapsed$processed$total$eta"
        }
        else -> elapsedLabel?.let { "$it · структурируем тезисы…" }
            ?: "Структурируем тезисы и сохраняем конспект"
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(tween(220)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AudioFile,
                            contentDescription = null,
                            tint = if (failed == null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (failed == null) "Офлайн-распознавание" else "Ошибка",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (failed == null) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error
                            )
                            if (failed == null) {
                                Spacer(Modifier.height(2.dp))
                                AnimatedContent(
                                    targetState = stageTitle,
                                    transitionSpec = {
                                        (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 })
                                            .togetherWith(fadeOut(tween(120)))
                                    },
                                    label = "import-stage"
                                ) { title ->
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (failed == null) {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (failed == null) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
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
                            text = if (running != null) {
                                "Можно вернуться назад: обработка продолжится в фоне."
                            } else {
                                "Обработка идёт на телефоне. Прогресс рассчитан по длительности аудио."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = failed.message,
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
