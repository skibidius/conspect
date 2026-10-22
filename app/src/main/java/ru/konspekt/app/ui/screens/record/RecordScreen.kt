package ru.konspekt.app.ui.screens.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import ru.konspekt.app.BuildConfig
import ru.konspekt.app.data.NoteGenerationResult
import ru.konspekt.app.data.Subject
import ru.konspekt.app.recording.VoskModelManager
import ru.konspekt.app.recording.VoskSpeechCapture
import ru.konspekt.app.ui.components.KonspektLogo
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import java.util.Locale

private enum class RecordStage { RECORDING, PAUSED, PROCESSING }

private data class TranscriptLine(
    val time: String,
    val text: String,
    val offTopic: Boolean = false
)

private data class DemoLine(val text: String, val offTopic: Boolean = false)

private val demoScript = listOf(
    DemoLine("Сегодня разберём производную сложной функции и правило цепочки."),
    DemoLine("Напомню, что производная описывает мгновенную скорость изменения функции."),
    DemoLine("Формально это предел отношения приращения функции к приращению аргумента."),
    DemoLine("Теперь перейдём к сложной функции: у неё есть внешняя и внутренняя части."),
    DemoLine("Обозначим внутреннюю функцию через g, а внешнюю — через f."),
    DemoLine("Небольшое отступление про конференцию в Казани и потерянный доклад.", true),
    DemoLine("Получаем правило: f от g штрих умножить на g штрих."),
    DemoLine("Разберём пример: y равно синус от трёх икс квадрат плюс один."),
    DemoLine("Внешняя функция — синус, внутренняя — квадратичная."),
    DemoLine("Производная равна косинусу, умноженному на шесть икс."),
    DemoLine("Не забывайте проверять область определения функции.")
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    subject: Subject,
    topic: String,
    demoMode: Boolean,
    cloudMode: Boolean,
    onClose: () -> Unit,
    onFinish: suspend (transcript: List<String>, durationSeconds: Int, demo: Boolean) -> NoteGenerationResult,
    onCompleted: (NoteGenerationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialDemoLines = remember(demoMode) {
        if (!demoMode) emptyList() else demoScript.take(4).mapIndexed { index, line ->
            TranscriptLine(
                time = String.format(Locale.US, "00:%02d", 4 + index * 7),
                text = line.text,
                offTopic = line.offTopic
            )
        }
    }
    var stage by remember { mutableStateOf(RecordStage.RECORDING) }
    var elapsed by remember { mutableIntStateOf(if (demoMode) 42 else 0) }
    val lines = remember { mutableStateListOf<TranscriptLine>().apply { addAll(initialDemoLines) } }
    var nextDemoIndex by remember { mutableIntStateOf(if (demoMode) 4 else 0) }
    var partialText by remember { mutableStateOf("") }
    var speechError by remember { mutableStateOf<String?>(null) }
    var recognitionStatus by remember { mutableStateOf<String?>(null) }
    var modelDownloading by remember {
        mutableStateOf(!demoMode && !VoskModelManager.isReady(context))
    }
    var modelProgress by remember { mutableFloatStateOf(0f) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var showNoSpeechDialog by remember { mutableStateOf(false) }
    var processingStep by remember { mutableIntStateOf(0) }
    var processingError by remember { mutableStateOf<String?>(null) }
    var amplitude by remember { mutableStateOf(0f) }
    val haptics = rememberHaptics()
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    BackHandler {
        if (stage != RecordStage.PROCESSING) showCloseDialog = true
    }

    val speechCapture = remember {
        VoskSpeechCapture(
            context = context,
            onPartial = { partialText = it },
            onFinal = { text ->
                if (text.isNotBlank()) {
                    lines.add(
                        TranscriptLine(
                            time = formatTime(elapsed),
                            text = text.trim(),
                            offTopic = isOffTopic(text)
                        )
                    )
                    partialText = ""
                }
            },
            onAmplitude = { amplitude = it },
            onStatus = { status ->
                recognitionStatus = status
                if (status == "Готово") modelDownloading = false
            },
            onError = { message ->
                speechError = message
                partialText = ""
                modelDownloading = false
            },
            onModelProgress = { progress ->
                modelDownloading = true
                modelProgress = progress
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            speechCapture.start()
        } else {
            speechError = "Нет доступа к микрофону"
            modelDownloading = false
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            speechCapture.stop()
        }
    }

    LaunchedEffect(Unit) {
        if (!demoMode) {
            if (permissionGranted) {
                speechCapture.start()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(stage) {
        if (stage == RecordStage.RECORDING) {
            while (true) {
                delay(1000)
                elapsed += 1
            }
        }
    }

    LaunchedEffect(stage, demoMode) {
        if (demoMode && stage == RecordStage.RECORDING) {
            while (nextDemoIndex < demoScript.size) {
                delay(3100)
                if (stage != RecordStage.RECORDING) break
                val item = demoScript[nextDemoIndex]
                lines.add(
                    TranscriptLine(
                        time = formatTime(elapsed),
                        text = item.text,
                        offTopic = item.offTopic
                    )
                )
                nextDemoIndex += 1
            }
        }
    }

    LaunchedEffect(stage) {
        if (stage == RecordStage.PROCESSING) {
            runCatching { speechCapture.stop() }
            val transcript = if (demoMode) {
                demoScript.map { it.text }
            } else {
                (lines.map { it.text } + partialText).filter { it.isNotBlank() }
            }
            val generation = async {
                onFinish(transcript, (elapsed / 60).coerceAtLeast(1), demoMode)
            }
            processingStep = 1
            delay(500)
            processingStep = 2
            delay(500)
            processingStep = 3
            val result = runCatching { generation.await() }.getOrNull()
            if (result == null) {
                processingError = "Не удалось собрать конспект. Попробуйте записать лекцию ещё раз."
                return@LaunchedEffect
            }
            processingStep = 4
            delay(500)
            processingStep = 5
            delay(450)
            runCatching { onCompleted(result) }.onFailure {
                processingError = "Не удалось открыть конспект. Попробуйте ещё раз."
            }
        }
    }

    if (stage == RecordStage.PROCESSING) {
        ProcessingScreen(
            subject = subject,
            topic = topic,
            step = processingStep,
            modifier = modifier
        )
        if (processingError != null) {
            AlertDialog(
                onDismissRequest = {},
                shape = RoundedCornerShape(28.dp),
                title = { Text("Не получилось") },
                text = { Text(processingError.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = {
                        haptics.tap()
                        onClose()
                    }) { Text("Закрыть") }
                }
            )
        }
        return
    }

    val dark = LocalKonspektDark.current
    val isPaused = stage == RecordStage.PAUSED
    val active = stage == RecordStage.RECORDING
    val transcriptState = rememberLazyListState()

    LaunchedEffect(lines.size, partialText) {
        val target = (lines.size + if (partialText.isNotBlank()) 1 else 0).coerceAtLeast(1)
        transcriptState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = topic.ifBlank { "Новая лекция" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptics.tap()
                    if (stage == RecordStage.PROCESSING) onClose() else showCloseDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Закрыть"
                    )
                }
            },
            actions = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (cloudMode) Icons.Rounded.Cloud else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (cloudMode) "DeepSeek" else "Локально",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        val smoothAmplitude by animateFloatAsState(
            targetValue = amplitude,
            animationSpec = tween(120),
            label = "amplitude"
        )
        RecordingStage(
            subject = subject,
            elapsed = elapsed,
            active = active,
            paused = isPaused,
            demoMode = demoMode,
            amplitude = smoothAmplitude,
            onPause = {
                haptics.tap()
                if (stage == RecordStage.RECORDING) {
                    stage = RecordStage.PAUSED
                    if (!demoMode) speechCapture.stop()
                } else {
                    stage = RecordStage.RECORDING
                    if (!demoMode) speechCapture.start()
                }
            },
            onStop = {
                haptics.confirm()
                if (!demoMode && lines.isEmpty() && partialText.isBlank()) {
                    showNoSpeechDialog = true
                } else {
                    if (!demoMode && partialText.isNotBlank()) {
                        lines.add(
                            TranscriptLine(
                                time = formatTime(elapsed),
                                text = partialText.trim(),
                                offTopic = isOffTopic(partialText)
                            )
                        )
                        partialText = ""
                    }
                    stage = RecordStage.PROCESSING
                }
            },
            onFlag = {
                haptics.tap()
                lines.add(
                    TranscriptLine(
                        time = formatTime(elapsed),
                        text = "Важный момент отмечен",
                        offTopic = false
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.92f)
        )

        TranscriptPanel(
            lines = lines,
            partialText = partialText,
            speechError = speechError,
            statusText = recognitionStatus,
            active = active,
            demoMode = demoMode,
            modelDownloading = modelDownloading,
            modelProgress = modelProgress,
            listState = transcriptState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.08f)
        )
    }

    if (showNoSpeechDialog) {
        AlertDialog(
            onDismissRequest = { showNoSpeechDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Речь не распознана") },
            text = {
                Text(
                    "Проверьте доступ к микрофону и офлайн-пакет русского языка " +
                        "в настройках распознавания речи. Затем попробуйте ещё раз."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.tap()
                    showNoSpeechDialog = false
                }) { Text("Продолжить") }
            }
        )
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Прервать запись?") },
            text = { Text("Аудио и текущий транскрипт будут удалены. Конспект не создастся.") },
            confirmButton = {
                TextButton(onClick = {
                    haptics.confirm()
                    showCloseDialog = false
                    speechCapture.stop()
                    onClose()
                }) {
                    Text("Прервать", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tap()
                    showCloseDialog = false
                }) {
                    Text("Продолжить")
                }
            }
        )
    }
}

@Composable
private fun RecordingStage(
    subject: Subject,
    elapsed: Int,
    active: Boolean,
    paused: Boolean,
    demoMode: Boolean,
    amplitude: Float,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onFlag: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalKonspektDark.current
    val transition = rememberInfiniteTransition(label = "recording-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        val orbSize = if (maxWidth < 340.dp) 150.dp else 178.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(orbSize + 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primary.copy(alpha = 0.10f),
                        radius = size.minDimension * (0.34f + 0.08f * pulse)
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.12f),
                        radius = size.minDimension * 0.38f,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = onContainer.copy(alpha = 0.05f),
                        radius = size.minDimension * (0.44f + 0.03f * pulse)
                    )
                }
                Surface(
                    modifier = Modifier.size(orbSize),
                    shape = CircleShape,
                    color = if (paused) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = onContainer.copy(alpha = 0.04f),
                                radius = size.minDimension * 0.34f,
                                center = Offset(size.width * 0.82f, size.height * 0.18f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatTime(elapsed),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                ),
                                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else onContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (paused) MaterialTheme.colorScheme.outline
                                            else MaterialTheme.colorScheme.error
                                        )
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (paused) "Пауза" else "Идёт запись",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant
                                    else onContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            RecordingWave(
                active = active,
                amplitude = amplitude,
                synthetic = demoMode,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(width = 132.dp, height = 28.dp)
            )
            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAction(
                    icon = Icons.Rounded.Flag,
                    label = "Момент",
                    size = 46.dp,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    enabled = active,
                    onClick = onFlag
                )
                CircleAction(
                    icon = Icons.Rounded.Stop,
                    label = "Стоп",
                    size = 68.dp,
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    onClick = onStop
                )
                CircleAction(
                    icon = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    label = if (paused) "Продолжить" else "Пауза",
                    size = 50.dp,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onPause
                )
            }

        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = container
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = content,
                    modifier = Modifier.size(size * 0.36f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun RecordingWave(
    active: Boolean,
    amplitude: Float,
    synthetic: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val bars = 13
        val step = size.width / bars
        repeat(bars) { index ->
            val animated = (kotlin.math.sin(phase + index * 0.7f) + 1f) / 2f
            val live = if (synthetic) animated else amplitude.coerceIn(0f, 1f)
            val wobble = (kotlin.math.sin(phase + index * 0.65f) + 1f) / 2f
            val factor = if (active) {
                (0.12f + live * (0.55f + 0.33f * wobble)).coerceIn(0.12f, 1f)
            } else {
                0.12f
            }
            val barHeight = size.height * factor
            drawRoundRect(
                color = color.copy(alpha = if (active) 0.8f else 0.3f),
                topLeft = Offset(index * step + step * 0.2f, (size.height - barHeight) / 2f),
                size = Size(step * 0.46f, barHeight),
                cornerRadius = CornerRadius(step * 0.24f)
            )
        }
    }
}

@Composable
private fun ModelDownloadCard(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(250),
        label = "model-download"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Загружаем модель речи",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Один раз · ~44 МБ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "После загрузки распознавание работает офлайн. Аудио не сохраняется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun TranscriptPanel(
    lines: List<TranscriptLine>,
    partialText: String,
    speechError: String?,
    statusText: String?,
    active: Boolean,
    demoMode: Boolean,
    modelDownloading: Boolean,
    modelProgress: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val offTopicCount = lines.count { it.offTopic } + if (partialText.isNotBlank() && isOffTopic(partialText)) 1 else 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Транскрипт",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(Modifier.weight(1f))
                if (offTopicCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "$offTopicCount не по теме",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (modelDownloading) {
                    ModelDownloadCard(
                        progress = modelProgress,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 4.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                items(lines, key = { "${it.time}-${it.text.hashCode()}" }) { line ->
                    TranscriptRow(line)
                }
                if (partialText.isNotBlank()) {
                    item {
                        TranscriptRow(
                            TranscriptLine(
                                time = "сейчас",
                                text = partialText,
                                offTopic = isOffTopic(partialText)
                            ),
                            partial = true
                        )
                    }
                }
                if (active && speechError == null) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusText?.takeIf { it != "Готово" } ?: "Слушаю микрофон…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (speechError != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = speechError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    Color.Transparent
                                )
                            )
                        )
                )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Отступления не попадут в конспект",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TranscriptRow(line: TranscriptLine, partial: Boolean = false) {
    val dark = LocalKonspektDark.current
    val base = if (line.offTopic) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    partial -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                    line.offTopic -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(
            text = line.time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.width(44.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = base
            )
            if (line.offTopic) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "НЕ ПО ТЕМЕ · СКРЫТО",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun ProcessingScreen(
    subject: Subject,
    topic: String,
    step: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Расшифровываем лекцию",
        "Убираем отступления",
        "Структурируем тезисы",
        "Верстаем формулы LaTeX",
        "Готовим PDF-версию"
    )
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = step / steps.size.toFloat(),
        animationSpec = tween(500),
        label = "progress"
    )
    val transition = rememberInfiniteTransition(label = "processing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.7f))
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = primary.copy(alpha = 0.08f),
                    radius = size.minDimension / 2f
                )
                drawArc(
                    color = primary,
                    startAngle = rotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                    size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            KonspektLogo(size = 54.dp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Собираем конспект",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(5.dp))
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
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                steps.forEachIndexed { index, label ->
                    val done = index < step
                    val current = index == step
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        done -> MaterialTheme.colorScheme.primary
                                        current -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                done -> Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                current -> CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                else -> Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (done || current) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
        Spacer(Modifier.weight(0.8f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Всё обрабатывается на этом устройстве",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun formatTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

private fun isOffTopic(text: String): Boolean {
    val lower = text.lowercase(Locale("ru"))
    return listOf(
        "консультац", "отступлен", "конференц", "столов", "перерыв",
        "домашн", "зачёт", "зачет", "расписан", "перенес"
    ).any { lower.contains(it) }
}
