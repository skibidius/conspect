package ru.konspekt.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.konspekt.app.BuildConfig
import ru.konspekt.app.data.AppViewModel
import ru.konspekt.app.data.AudioImportState
import ru.konspekt.app.data.SpeechEngine
import ru.konspekt.app.pdf.PdfExporter
import ru.konspekt.app.ui.components.ImportProgressBanner
import ru.konspekt.app.ui.components.NewRecordingSheet
import ru.konspekt.app.ui.components.rememberHaptics
import ru.konspekt.app.ui.screens.about.AboutScreen
import ru.konspekt.app.ui.screens.home.HomeScreen
import ru.konspekt.app.ui.screens.importaudio.ImportAudioScreen
import ru.konspekt.app.ui.screens.library.LibraryScreen
import ru.konspekt.app.ui.screens.note.NoteScreen
import ru.konspekt.app.ui.screens.profile.ProfileScreen
import ru.konspekt.app.ui.screens.record.RecordScreen
import ru.konspekt.app.ui.screens.search.SearchScreen
import ru.konspekt.app.ui.screens.subjects.SubjectDetailScreen
import ru.konspekt.app.ui.screens.subjects.SubjectsScreen
import ru.konspekt.app.ui.screens.trash.TrashScreen

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination("home", "Главная", Icons.Rounded.Home),
    BottomDestination("subjects", "Предметы", Icons.Rounded.GridView),
    BottomDestination("library", "Библиотека", Icons.Rounded.MenuBook),
    BottomDestination("search", "Поиск", Icons.Rounded.Search)
)

@Composable
fun KonspektApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isRoot = currentRoute == null || currentRoute in bottomDestinations.map { it.route }

    var showRecordingSheet by remember { mutableStateOf(false) }
    var preselectedSubject by remember { mutableStateOf<String?>(null) }
    var pendingRecordSubject by remember { mutableStateOf<String?>(null) }
    var exportingNoteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchExportRunning by remember { mutableStateOf(false) }
    val audioImportState = viewModel.audioImport
    val importScreenVisible = viewModel.importScreenVisible
    var displayedImport by remember {
        mutableStateOf<AudioImportState?>(
            audioImportState.takeIf { it !is AudioImportState.Idle }
        )
    }
    LaunchedEffect(audioImportState) {
        if (audioImportState !is AudioImportState.Idle) {
            displayedImport = audioImportState
        }
    }
    val importOverlayVisible = importScreenVisible && audioImportState !is AudioImportState.Idle
    val overlayImportState = audioImportState.takeIf { it !is AudioImportState.Idle }
        ?: displayedImport

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val subjectId = pendingRecordSubject
        pendingRecordSubject = null
        if (granted) {
            preselectedSubject = subjectId
            showRecordingSheet = true
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Без доступа к микрофону запись невозможна",
                    actionLabel = "Настройки",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
        }
    }

    fun openRecording(subjectId: String? = null) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            preselectedSubject = subjectId
            showRecordingSheet = true
        } else {
            pendingRecordSubject = subjectId
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val haptics = rememberHaptics()

    fun navigateTab(route: String) {
        haptics.tap()
        if (route == "home") {
            // Always return to the real home destination. Relying only on
            // restoreState could leave the Subjects tab visually selected.
            if (!navController.popBackStack("home", inclusive = false)) {
                navController.navigate("home") {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun message(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    fun trashNotes(ids: List<String>) {
        val notesToTrash = ids.distinct().filter { viewModel.note(it) != null }
        if (notesToTrash.isEmpty()) return
        notesToTrash.forEach(viewModel::deleteNote)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (notesToTrash.size == 1) "Конспект в корзине"
                else "Конспектов в корзине: ${notesToTrash.size}",
                actionLabel = "Вернуть",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                notesToTrash.forEach(viewModel::restoreNote)
            }
        }
    }

    fun trashNote(id: String) = trashNotes(listOf(id))

    fun shareNotePdf(id: String) {
        if (id in exportingNoteIds) return
        val note = viewModel.note(id) ?: return
        val subject = viewModel.subject(note.subjectId) ?: return
        exportingNoteIds = exportingNoteIds + id
        PdfExporter.export(context, note, subject) { _, text ->
            exportingNoteIds = exportingNoteIds - id
            message(text)
        }
    }

    fun exportNotesPdf(ids: List<String>) {
        if (batchExportRunning) return
        val items = ids.distinct().mapNotNull { id ->
            val note = viewModel.note(id) ?: return@mapNotNull null
            val subject = viewModel.subject(note.subjectId) ?: return@mapNotNull null
            note to subject
        }
        if (items.isEmpty()) return
        batchExportRunning = true
        message("Экспортируем PDF: ${items.size}…")
        var index = 0
        var exported = 0
        fun exportNext() {
            if (index >= items.size) {
                batchExportRunning = false
                message(
                    if (exported == 0) "Не удалось экспортировать PDF"
                    else "PDF сохранены в Загрузки: $exported"
                )
                return
            }
            val (note, subject) = items[index]
            index += 1
            PdfExporter.export(context, note, subject, shareAfter = false) { file, _ ->
                if (file != null) exported += 1
                exportNext()
            }
        }
        exportNext()
    }

    val completedImport = audioImportState as? AudioImportState.Completed
    val failedImport = audioImportState as? AudioImportState.Failed

    LaunchedEffect(completedImport?.noteId, importScreenVisible) {
        val completed = completedImport ?: return@LaunchedEffect
        if (importScreenVisible) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Конспект готов",
            actionLabel = "Открыть",
            duration = SnackbarDuration.Long
        )
        viewModel.consumeAudioImport()
        completed.warning?.let { message(it) }
        if (result == SnackbarResult.ActionPerformed) {
            navController.navigate("note/${completed.noteId}") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(failedImport?.message, importScreenVisible) {
        val failed = failedImport ?: return@LaunchedEffect
        if (importScreenVisible) return@LaunchedEffect
        message(failed.message)
        viewModel.consumeAudioImport()
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                val runningImport = audioImportState as? AudioImportState.Running
                AnimatedVisibility(
                    visible = runningImport != null && !importScreenVisible,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { it },
                    exit = fadeOut(tween(140)) + slideOutVertically(tween(200)) { it }
                ) {
                    runningImport?.let { importState ->
                        ImportProgressBanner(
                            state = importState,
                            onClick = viewModel::showImportScreen
                        )
                    }
                }
                if (isRoot) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTab(destination.route) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (isRoot) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.tap()
                        if (viewModel.subjects.isEmpty()) {
                            navController.navigate("subjects")
                        } else {
                            openRecording(null)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = { Text("Запись") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300))
            }
        ) {
            tabComposable("home") {
                HomeScreen(
                    userName = viewModel.settings.userName,
                    subjects = viewModel.subjects,
                    notes = viewModel.notes,
                    notesCount = viewModel::notesCount,
                    onOpenNote = { navController.navigate("note/$it") },
                    onDeleteNote = ::trashNote,
                    onDeleteNotes = ::trashNotes,
                    onSharePdf = ::shareNotePdf,
                    onExportNotes = ::exportNotesPdf,
                    onOpenSubject = { navController.navigate("subject/$it") },
                    onAllSubjects = { navController.navigate("subjects") },
                    onAllNotes = { navController.navigate("library") },
                    onSearch = { navController.navigate("search") },
                    onProfile = { navController.navigate("profile") },
                    onStartRecording = { subjectId -> openRecording(subjectId) }
                )
            }

            tabComposable("subjects") {
                SubjectsScreen(
                    subjects = viewModel.subjects,
                    notesCount = viewModel::notesCount,
                    totalMinutes = viewModel::totalMinutesForSubject,
                    onOpenSubject = { navController.navigate("subject/$it") },
                    onAddSubject = { name, tone, icon ->
                        viewModel.addSubject(name, tone, icon)
                        message("Предмет добавлен")
                    },
                    onDeleteSubject = { id ->
                        viewModel.deleteSubject(id)
                        message("Предмет и его конспекты удалены")
                    },
                    onProfile = { navController.navigate("profile") }
                )
            }

            tabComposable("library") {
                LibraryScreen(
                    notes = viewModel.notes,
                    subjects = viewModel.subjects,
                    onOpenNote = { navController.navigate("note/$it") },
                    onDeleteNote = ::trashNote,
                    onDeleteNotes = ::trashNotes,
                    onSharePdf = ::shareNotePdf,
                    onExportNotes = ::exportNotesPdf,
                    onSearch = { navController.navigate("search") },
                    onTrash = { navController.navigate("trash") },
                    trashCount = viewModel.trash.size,
                    onProfile = { navController.navigate("profile") }
                )
            }

            tabComposable("search") {
                SearchScreen(
                    notes = viewModel.notes,
                    subjects = viewModel.subjects,
                    onOpenNote = { navController.navigate("note/$it") },
                    onDeleteNote = ::trashNote,
                    onDeleteNotes = ::trashNotes,
                    onSharePdf = ::shareNotePdf,
                    onExportNotes = ::exportNotesPdf,
                    onOpenSubject = { navController.navigate("subject/$it") }
                )
            }

            composable("trash") {
                TrashScreen(
                    notes = viewModel.trash,
                    subjects = viewModel.subjects,
                    onBack = { navController.popBackStack() },
                    onRestore = { id ->
                        viewModel.restoreNote(id)
                        message("Конспект восстановлен")
                    },
                    onDeleteForever = viewModel::deleteNotePermanently,
                    onEmptyTrash = {
                        viewModel.emptyTrash()
                        message("Корзина очищена")
                    }
                )
            }

            composable("about") {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("profile") {
                ProfileScreen(
                    settings = viewModel.settings,
                    notesCount = viewModel.notes.size,
                    subjectsCount = viewModel.subjects.size,
                    totalMinutes = viewModel.notes.sumOf { it.durationMinutes },
                    hasDeepSeekKey = viewModel.hasDeepSeekKey,
                    onSaveDeepSeekKey = viewModel::saveDeepSeekKey,
                    onClearDeepSeekKey = viewModel::clearDeepSeekKey,
                    onExportBackup = viewModel::exportBackupJson,
                    onImportBackup = viewModel::importBackupJson,
                    onMessage = ::message,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate("about") },
                    onSettingsChange = viewModel::updateSettings
                )
            }

            composable(
                route = "subject/{subjectId}",
                arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId")
                val subject = subjectId?.let(viewModel::subject)
                if (subject != null) {
                    SubjectDetailScreen(
                        subject = subject,
                        notes = viewModel.notesForSubject(subject.id),
                        totalMinutes = viewModel.totalMinutesForSubject(subject.id),
                        onBack = { navController.popBackStack() },
                        onOpenNote = { navController.navigate("note/$it") },
                        onDeleteNote = ::trashNote,
                        onDeleteNotes = ::trashNotes,
                        onSharePdf = ::shareNotePdf,
                        onExportNotes = ::exportNotesPdf,
                        onRecord = { openRecording(subject.id) },
                        onUpdateSubject = { name, tone, icon ->
                            viewModel.updateSubject(subject.id, name, tone, icon)
                            message("Предмет обновлён")
                        }
                    )
                }
            }

            composable(
                route = "note/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { entry ->
                val noteId = entry.arguments?.getString("noteId")
                val note = noteId?.let(viewModel::note)
                if (note != null) {
                    val subject = viewModel.subject(note.subjectId)
                    if (subject != null) {
                        NoteScreen(
                            note = note,
                            subject = subject,
                            onBack = { navController.popBackStack() },
                            onMessage = ::message
                        )
                    }
                }
            }

            composable(
                route = "record/{subjectId}?topic={topic}",
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("topic") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                ),
                enterTransition = {
                    slideInVertically(tween(340)) { it } + fadeIn(tween(340))
                },
                exitTransition = {
                    slideOutVertically(tween(280)) { it } + fadeOut(tween(280))
                },
                popEnterTransition = {
                    fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutVertically(tween(280)) { it } + fadeOut(tween(280))
                }
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId").orEmpty()
                val topic = entry.arguments?.getString("topic").orEmpty()
                val subject = viewModel.subject(subjectId)
                if (subject != null) {
                    RecordScreen(
                        subject = subject,
                        topic = topic,
                        demoMode = BuildConfig.DEBUG,
                        cloudMode = viewModel.settings.aiMode == ru.konspekt.app.data.AiMode.DEEPSEEK && viewModel.hasDeepSeekKey,
                        useSystemSpeech = viewModel.settings.speechEngine == SpeechEngine.SYSTEM,
                        onClose = { navController.popBackStack() },
                        onFinish = { transcript, durationSeconds, demo ->
                            viewModel.generateNote(
                                subjectId = subject.id,
                                topic = topic,
                                transcript = transcript,
                                durationMinutes = durationSeconds.coerceAtLeast(1),
                                demo = demo
                            )
                        },
                        onCompleted = { result ->
                            val currentId = navController.currentDestination?.id
                            navController.navigate("note/${result.noteId}") {
                                if (currentId != null) {
                                    popUpTo(currentId) { inclusive = true }
                                }
                            }
                            result.warning?.let(::message)
                        }
                    )
                }
            }
        }

    }

        AnimatedVisibility(
            visible = importOverlayVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 16 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 24 }
        ) {
            val state = overlayImportState
            if (state != null) {
                val subjectId = when (state) {
                    is AudioImportState.Running -> state.subjectId
                    is AudioImportState.Completed -> state.subjectId
                    is AudioImportState.Failed -> state.subjectId
                    AudioImportState.Idle -> ""
                }
                val topic = when (state) {
                    is AudioImportState.Running -> state.topic
                    is AudioImportState.Completed -> state.topic
                    is AudioImportState.Failed -> state.topic
                    AudioImportState.Idle -> ""
                }
                val importSubject = viewModel.subject(subjectId) ?: viewModel.subjects.firstOrNull()
                if (importSubject != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        ImportAudioScreen(
                            subject = importSubject,
                            topic = topic,
                            state = state,
                            onClose = viewModel::hideImportScreen,
                            onCompleted = { completed ->
                                viewModel.consumeAudioImport()
                                navController.navigate("note/${completed.noteId}") {
                                    launchSingleTop = true
                                }
                                completed.warning?.let(::message)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRecordingSheet) {
        NewRecordingSheet(
            subjects = viewModel.subjects,
            initialSubjectId = preselectedSubject,
            filterTangents = viewModel.settings.filterTangents,
            useSystemSpeech = viewModel.settings.speechEngine == SpeechEngine.SYSTEM,
            onFilterChange = { enabled ->
                viewModel.updateSettings { it.copy(filterTangents = enabled) }
            },
            onImportAudio = { subjectId, topic, uri ->
                showRecordingSheet = false
                viewModel.startAudioImport(subjectId, topic, uri)
            },
            onDismiss = { showRecordingSheet = false },
            onStart = { subjectId, topic ->
                showRecordingSheet = false
                navController.navigate(
                    "record/${Uri.encode(subjectId)}?topic=${Uri.encode(topic)}"
                )
            }
        )
    }
}


private fun NavGraphBuilder.tabComposable(
    route: String,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220))
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = {
            fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))
        },
        popExitTransition = { fadeOut(tween(160)) },
        content = { content() }
    )
}
