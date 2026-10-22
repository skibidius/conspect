package ru.konspekt.app.ui.screens.profile

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import ru.konspekt.app.BuildConfig
import ru.konspekt.app.recording.VoskModel
import ru.konspekt.app.recording.VoskModelManager
import ru.konspekt.app.data.AiMode
import ru.konspekt.app.data.Palette
import ru.konspekt.app.data.ThemeMode
import ru.konspekt.app.data.UserSettings
import ru.konspekt.app.ui.components.DividerLine
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.ui.theme.konspektColorScheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    settings: UserSettings,
    notesCount: Int,
    subjectsCount: Int,
    totalMinutes: Int,
    hasDeepSeekKey: Boolean,
    onSaveDeepSeekKey: (String) -> Unit,
    onClearDeepSeekKey: () -> Unit,
    onBack: () -> Unit,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedModel by remember { mutableStateOf(VoskModelManager.selected(context)) }
    var modelReady by remember {
        mutableStateOf(VoskModelManager.isReady(context, selectedModel))
    }
    var showModelPicker by remember { mutableStateOf(false) }
    var modelDownloading by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableFloatStateOf(0f) }
    var modelError by remember { mutableStateOf<String?>(null) }
    val haptics = rememberHaptics()
    val uriHandler = LocalUriHandler.current
    val animatedModelProgress by animateFloatAsState(
        targetValue = modelProgress,
        animationSpec = tween(300),
        label = "model-progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Настройки") },
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 36.dp)
        ) {
            SettingsCard {
                SettingHeader(
                    icon = Icons.Rounded.Person,
                    title = "Имя",
                    subtitle = "Как к вам обращаться"
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.userName,
                    onValueChange = { value -> onSettingsChange { it.copy(userName = value) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Имя") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            StatsRow(notesCount = notesCount, subjectsCount = subjectsCount, totalMinutes = totalMinutes)

            Spacer(Modifier.height(28.dp))
            SectionLabel("Оформление")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SettingHeader(
                    icon = Icons.Rounded.DarkMode,
                    title = "Тема",
                    subtitle = "Как выглядит приложение"
                )
                Spacer(Modifier.height(12.dp))
                ThemeModeSelector(
                    selected = settings.themeMode,
                    onSelect = { mode -> onSettingsChange { it.copy(themeMode = mode) } }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(14.dp))
                    DividerLine()
                    Spacer(Modifier.height(12.dp))
                    SwitchRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Динамические цвета",
                        subtitle = "Брать палитру из обоев",
                        checked = settings.dynamicColor,
                        onCheckedChange = { enabled ->
                            onSettingsChange { it.copy(dynamicColor = enabled) }
                        }
                    )
                }
                if (!settings.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(14.dp))
                    DividerLine()
                    Spacer(Modifier.height(14.dp))
                    SettingHeader(
                        icon = Icons.Rounded.Palette,
                        title = "Цветовая схема",
                        subtitle = "Спокойные тона Material You"
                    )
                    Spacer(Modifier.height(12.dp))
                    PaletteSelector(
                        selected = settings.palette,
                        onSelect = { palette -> onSettingsChange { it.copy(palette = palette) } }
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("AI-обработка")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SettingHeader(
                    icon = Icons.Rounded.SmartToy,
                    title = "Обработка конспекта",
                    subtitle = "Локально или DeepSeek Flash"
                )
                Spacer(Modifier.height(12.dp))
                AiModeSelector(
                    selected = settings.aiMode,
                    onSelect = { mode -> onSettingsChange { it.copy(aiMode = mode) } }
                )
                Spacer(Modifier.height(12.dp))
                if (settings.aiMode == AiMode.LOCAL) {
                    Text(
                        text = "Локальный режим: расшифровка и структурирование выполняются на устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    if (!hasDeepSeekKey) {
                        Text(
                            text = "Добавьте API-ключ DeepSeek, иначе конспект будет собран локально.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (hasDeepSeekKey) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "API-ключ сохранён",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Зашифрован на устройстве",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                haptics.tap()
                                onClearDeepSeekKey()
                            }) {
                                Text("Удалить")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API-ключ DeepSeek") },
                            placeholder = { Text("sk-…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (showApiKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    haptics.tap()
                                    showApiKey = !showApiKey
                                }) {
                                    Icon(
                                        imageVector = if (showApiKey) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (showApiKey) "Скрыть ключ" else "Показать ключ"
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                haptics.confirm()
                                onSaveDeepSeekKey(apiKeyInput)
                                apiKeyInput = ""
                            },
                            enabled = apiKeyInput.isNotBlank()
                        ) {
                            Text("Сохранить ключ")
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    TextButton(
                        onClick = {
                            haptics.tap()
                            uriHandler.openUri("https://platform.deepseek.com/api_keys")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("Получить API-ключ")
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Модель: deepseek-flash. В этом режиме текст лекции отправляется в DeepSeek. Аудио не отправляется.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("Запись и конспекты")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SwitchRow(
                    icon = Icons.Rounded.FilterAlt,
                    title = "Убирать отступления",
                    subtitle = "Не по теме не попадёт в конспект",
                    checked = settings.filterTangents,
                    onCheckedChange = { enabled ->
                        onSettingsChange { it.copy(filterTangents = enabled) }
                    }
                )

            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("Модели")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                val statusColor = when {
                    modelDownloading -> MaterialTheme.colorScheme.secondaryContainer
                    modelReady -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val statusContent = when {
                    modelDownloading -> MaterialTheme.colorScheme.onSecondaryContainer
                    modelReady -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (modelReady) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (modelReady) Icons.Rounded.CheckCircle
                            else Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = if (modelReady) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Распознавание речи",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (modelReady) {
                                "Vosk · русский · ${VoskModelManager.sizeBytes(context, selectedModel) / (1024 * 1024)} МБ"
                            } else {
                                "Vosk · русский · ${selectedModel.sizeLabel}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = statusColor
                    ) {
                        Text(
                            text = when {
                                modelDownloading -> "Загрузка"
                                modelReady -> "Скачана"
                                else -> "Не скачана"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusContent
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Surface(
                    onClick = {
                        if (!modelDownloading) {
                            haptics.tap()
                            showModelPicker = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Модель",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedModel.title} · ${selectedModel.sizeLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (modelDownloading) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Скачивание модели",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${(modelProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedModelProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }

                if (modelError != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = modelError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (!modelDownloading) {
                    Spacer(Modifier.height(16.dp))
                    if (modelReady) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    modelDownloading = true
                                    modelProgress = 0f
                                    modelError = null
                                    scope.launch {
                                        val ok = VoskModelManager.downloadModel(context, model = selectedModel, force = true) { p ->
                                            scope.launch { modelProgress = p }
                                        }
                                        modelDownloading = false
                                        modelReady = VoskModelManager.isReady(context, selectedModel)
                                        if (!ok) modelError = "Не удалось обновить модель"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Обновить")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    VoskModelManager.delete(context, selectedModel)
                                    modelReady = false
                                    modelProgress = 0f
                                    modelError = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Удалить")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptics.confirm()
                                modelDownloading = true
                                modelProgress = 0f
                                modelError = null
                                scope.launch {
                                    val ok = VoskModelManager.downloadModel(context, model = selectedModel) { p ->
                                        scope.launch { modelProgress = p }
                                    }
                                    modelDownloading = false
                                    modelReady = VoskModelManager.isReady(context, selectedModel)
                                    if (!ok) modelError = "Не удалось скачать модель"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Скачать · ${selectedModel.sizeLabel}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("Приватность")
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(Modifier.size(13.dp))
                    Column {
                        Text(
                            text = "Локально по умолчанию",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "По умолчанию аудио и текст остаются на телефоне. В режиме DeepSeek отправляется только текст лекции.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = "Версия ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showModelPicker) {
        ModelPickerDialog(
            selected = selectedModel,
            onSelect = { model ->
                VoskModelManager.select(context, model)
                selectedModel = model
                modelReady = VoskModelManager.isReady(context, model)
                modelProgress = 0f
                modelError = null
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false }
        )
    }
}

@Composable
private fun ModelPickerDialog(
    selected: VoskModel,
    onSelect: (VoskModel) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Модель распознавания") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoskModel.entries.forEach { model ->
                    val ready = VoskModelManager.isReady(context, model)
                    val active = selected == model
                    Surface(
                        onClick = {
                            haptics.tap()
                            onSelect(model)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (active) {
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ready) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = if (active) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (ready) {
                                        "Скачана · ${model.sizeLabel}"
                                    } else {
                                        "Не скачана · ${model.sizeLabel}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ready) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            RadioButton(selected = active, onClick = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.tap()
                onDismiss()
            }) { Text("Закрыть") }
        }
    )
}

@Composable
private fun StatsRow(notesCount: Int, subjectsCount: Int, totalMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(notesCount.toString(), "конспектов", Modifier.weight(1f))
        StatCard(
            if (totalMinutes >= 60) "${totalMinutes / 60}:${"%02d".format(totalMinutes % 60)}" else "${totalMinutes}м",
            if (totalMinutes >= 60) "часов" else "минут",
            Modifier.weight(1f)
        )
        StatCard(subjectsCount.toString(), "предметов", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 15.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.9.sp
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiModeSelector(selected: AiMode, onSelect: (AiMode) -> Unit) {
    val options = listOf(AiMode.LOCAL to "На устройстве", AiMode.DEEPSEEK to "DeepSeek")
    val haptics = rememberHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = {
                    haptics.tap()
                    onSelect(mode)
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "Системная",
        ThemeMode.LIGHT to "Светлая",
        ThemeMode.DARK to "Тёмная"
    )
    val haptics = rememberHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = {
                    haptics.tap()
                    onSelect(mode)
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PaletteSelector(selected: Palette, onSelect: (Palette) -> Unit) {
    val dark = LocalKonspektDark.current
    val haptics = rememberHaptics()
    val palettes = listOf(
        Palette.SAGE to "Лес",
        Palette.SKY to "Небо",
        Palette.LAVENDER to "Лаванда",
        Palette.CLAY to "Глина"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        palettes.forEach { (palette, label) ->
            val scheme = konspektColorScheme(palette, dark)
            val active = selected == palette
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptics.tap()
                        onSelect(palette)
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer)
                        .then(if (active) Modifier.border(2.dp, scheme.primary, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(scheme.primary)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.tap()
                onCheckedChange(it)
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
