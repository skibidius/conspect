package ru.konspekt.app.ui.screens.profile

import android.os.Build
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.konspekt.app.data.Palette
import ru.konspekt.app.data.ThemeMode
import ru.konspekt.app.data.UserSettings
import ru.konspekt.app.ui.components.DividerLine
import ru.konspekt.app.ui.components.UserAvatar
import ru.konspekt.app.ui.theme.LocalKonspektDark
import ru.konspekt.app.ui.theme.konspektColorScheme

@Composable
fun ProfileScreen(
    settings: UserSettings,
    notesCount: Int,
    subjectsCount: Int,
    totalMinutes: Int,
    onBack: () -> Unit,
    onSettingsChange: ((UserSettings) -> UserSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Профиль и настройки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 36.dp)
        ) {
            ProfileHeader(settings.userName)
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
                Spacer(Modifier.height(18.dp))
                DividerLine()
                Spacer(Modifier.height(16.dp))
                SettingHeader(
                    icon = Icons.Rounded.Palette,
                    title = "Цветовая схема",
                    subtitle = "Спокойные тона Material You"
                )
                Spacer(Modifier.height(14.dp))
                PaletteSelector(
                    selected = settings.palette,
                    onSelect = { palette -> onSettingsChange { it.copy(palette = palette) } }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(12.dp))
                DividerLine()
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    icon = Icons.Rounded.AccountTree,
                    title = "Автоструктурирование",
                    subtitle = "Заголовки, формулы и тезисы",
                    checked = settings.autoStructure,
                    onCheckedChange = { enabled ->
                        onSettingsChange { it.copy(autoStructure = enabled) }
                    }
                )
                Spacer(Modifier.height(12.dp))
                DividerLine()
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    icon = Icons.Rounded.PlayCircle,
                    title = "Демо-лекция",
                    subtitle = "Показывать пример при записи",
                    checked = settings.demoMode,
                    onCheckedChange = { enabled ->
                        onSettingsChange { it.copy(demoMode = enabled) }
                    }
                )
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
                            text = "Всё остаётся на устройстве",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Аудио и текст лекций не отправляются в интернет. Конспекты хранятся только в памяти телефона.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column {
                    InfoRow("Версия", "1.0.0")
                    DividerLine(Modifier.padding(horizontal = 16.dp))
                    InfoRow("Обработка", "Локальная")
                    DividerLine(Modifier.padding(horizontal = 16.dp))
                    InfoRow("Данные", "Облако не используется")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Конспект · сделано для студентов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ProfileHeader(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = name, size = 66.dp)
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    text = "$name Смирнова",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "2 курс · прикладная математика",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "Локальный профиль",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
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
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "Системная",
        ThemeMode.LIGHT to "Светлая",
        ThemeMode.DARK to "Тёмная"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp)
    ) {
        options.forEach { (mode, label) ->
            val active = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(11.dp),
                color = if (active) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 9.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PaletteSelector(selected: Palette, onSelect: (Palette) -> Unit) {
    val dark = LocalKonspektDark.current
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
                    .clickable { onSelect(palette) }
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
