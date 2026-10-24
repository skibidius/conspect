package ru.konspekt.app.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.konspekt.app.BuildConfig
import ru.konspekt.app.ui.components.KonspektLogo
import ru.konspekt.app.ui.components.rememberHaptics

private const val DONATE_URL = "https://pay.cloudtips.ru/p/b5d67e09"

private data class LicenseEntry(
    val name: String,
    val version: String,
    val license: String,
    val copyright: String,
    val asset: String
)

private val licenseEntries = listOf(
    LicenseEntry(
        name = "Google Sans",
        version = "2025",
        license = "SIL Open Font License 1.1",
        copyright = "Copyright 2025 The Google Sans Project Authors",
        asset = "licenses/ofl-1.1.txt"
    ),
    LicenseEntry(
        name = "GigaAM v2",
        version = "NeMo CTC, int8",
        license = "MIT License",
        copyright = "Copyright (c) 2024 GigaChat Team",
        asset = "licenses/mit.txt"
    ),
    LicenseEntry(
        name = "sherpa-onnx",
        version = "6.25.21",
        license = "Apache License 2.0",
        copyright = "Copyright 2023–2025 k2-fsa / Next-gen Kaldi contributors",
        asset = "licenses/apache-2.0.txt"
    ),
    LicenseEntry(
        name = "lib-sherpa-onnx (Android wrapper)",
        version = "6.25.21",
        license = "Apache License 2.0",
        copyright = "Copyright bihe0832 / AndroidAppFactory contributors",
        asset = "licenses/apache-2.0.txt"
    ),
    LicenseEntry(
        name = "ONNX Runtime",
        version = "bundled by sherpa-onnx",
        license = "MIT License",
        copyright = "Copyright (c) Microsoft Corporation",
        asset = "licenses/mit.txt"
    ),
    LicenseEntry(
        name = "KaTeX",
        version = "bundled",
        license = "MIT License",
        copyright = "Copyright (c) 2013–2020 Khan Academy and other contributors",
        asset = "licenses/mit.txt"
    ),
    LicenseEntry(
        name = "LaTeX Renderer",
        version = "1.6.1",
        license = "MIT License",
        copyright = "Copyright (c) 2026 huarangmeng",
        asset = "licenses/mit.txt"
    ),
    LicenseEntry(
        name = "Gson",
        version = "2.11.0",
        license = "Apache License 2.0",
        copyright = "Copyright Google LLC",
        asset = "licenses/apache-2.0.txt"
    ),
    LicenseEntry(
        name = "AndroidX / Jetpack Compose / Material 3",
        version = "current",
        license = "Apache License 2.0",
        copyright = "Copyright The Android Open Source Project",
        asset = "licenses/apache-2.0.txt"
    ),
    LicenseEntry(
        name = "Kotlin",
        version = "current",
        license = "Apache License 2.0",
        copyright = "Copyright JetBrains s.r.o. and Kotlin contributors",
        asset = "licenses/apache-2.0.txt"
    ),
    LicenseEntry(
        name = "Material Icons",
        version = "current",
        license = "Apache License 2.0",
        copyright = "Copyright Google LLC",
        asset = "licenses/apache-2.0.txt"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val uriHandler = LocalUriHandler.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openedLicense by remember { mutableStateOf<LicenseEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("О приложении") },
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

        PrimaryTabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = tab == 0,
                onClick = {
                    haptics.tap()
                    tab = 0
                },
                text = { Text("О приложении") }
            )
            Tab(
                selected = tab == 1,
                onClick = {
                    haptics.tap()
                    tab = 1
                },
                text = { Text("Лицензии") }
            )
        }

        if (tab == 0) {
            AboutTab(
                onOpenDonate = {
                    haptics.tap()
                    uriHandler.openUri(DONATE_URL)
                },
                onOpenLicenses = {
                    haptics.tap()
                    tab = 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LicensesTab(
                onOpen = { entry ->
                    haptics.tap()
                    openedLicense = entry
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    openedLicense?.let { entry ->
        LicenseDialog(entry = entry, onDismiss = { openedLicense = null })
    }
}

@Composable
private fun AboutTab(
    onOpenDonate: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KonspektLogo(size = 76.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Conspect",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Версия ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Локальные конспекты лекций: запись, расшифровка\nи структурирование на устройстве.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        SectionCaption("ИНФОРМАЦИЯ")
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column {
                AboutListItem(
                    icon = Icons.Rounded.Lock,
                    title = "Приватность",
                    subtitle = "Аудио не сохраняется. Sherpa GigaAM работает офлайн."
                )
                HorizontalDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                AboutListItem(
                    icon = Icons.Rounded.Favorite,
                    title = "Поддержать проект",
                    subtitle = "CloudTips · добровольный донат",
                    onClick = onOpenDonate,
                    trailing = Icons.AutoMirrored.Rounded.OpenInNew
                )
                HorizontalDivider(Modifier.padding(start = 72.dp, end = 16.dp))
                AboutListItem(
                    icon = Icons.Rounded.Code,
                    title = "Лицензии",
                    subtitle = "Открытые библиотеки и модели",
                    onClick = onOpenLicenses,
                    trailing = Icons.Rounded.ChevronRight
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Собрано на Kotlin и Jetpack Compose.\nРаботает без аккаунта и без облака.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LicensesTab(
    onOpen: (LicenseEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Conspect использует открытые библиотеки и модели. Нажмите на компонент, чтобы открыть полный текст лицензии.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column {
                    licenseEntries.forEachIndexed { index, entry ->
                        LicenseListItem(entry = entry, onClick = { onOpen(entry) })
                        if (index != licenseEntries.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 16.dp, end = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseListItem(entry: LicenseEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${entry.version} · ${entry.license}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun AboutListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: ImageVector? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        },
        trailingContent = trailing?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun LicenseDialog(entry: LicenseEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(entry.asset) {
        runCatching {
            context.assets.open(entry.asset).bufferedReader().use { it.readText() }
        }.getOrDefault("Текст лицензии недоступен.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(entry.name)
                Text(
                    text = entry.license,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = entry.copyright,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
