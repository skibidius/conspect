package ru.konspekt.app.ui

import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import ru.konspekt.app.data.SpeechEngine
import ru.konspekt.app.ui.components.NewRecordingSheet
import ru.konspekt.app.ui.components.rememberHaptics
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

private data class ImportRequest(
    val subjectId: String,
    val topic: String,
    val uri: Uri
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isRoot = currentRoute == null || currentRoute in bottomDestinations.map { it.route }

    var showRecordingSheet by remember { mutableStateOf(false) }
    var importRequest by remember { mutableStateOf<ImportRequest?>(null) }
    var preselectedSubject by remember { mutableStateOf<String?>(null) }

    fun openRecording(subjectId: String? = null) {
        preselectedSubject = subjectId
        showRecordingSheet = true
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

    fun trashNote(id: String) {
        viewModel.deleteNote(id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Конспект в корзине",
                actionLabel = "Вернуть",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreNote(id)
            }
        }
    }

    val activeImport = importRequest
    if (activeImport != null) {
        val importSubject = viewModel.subject(activeImport.subjectId)
        if (importSubject != null) {
            ImportAudioScreen(
                subject = importSubject,
                topic = activeImport.topic,
                uri = activeImport.uri,
                onClose = { importRequest = null },
                onFinish = { lines, duration ->
                    viewModel.generateNote(
                        subjectId = importSubject.id,
                        topic = activeImport.topic,
                        transcript = lines,
                        // AudioFileTranscriber returns seconds, while notes store minutes.
                        durationMinutes = ((duration + 59) / 60).coerceAtLeast(1),
                        demo = false
                    )
                },
                onCompleted = { result ->
                    importRequest = null
                    navController.navigate("note/${result.noteId}") {
                        launchSingleTop = true
                    }
                    result.warning?.let(::message)
                }
            )
            return
        } else {
            importRequest = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                importRequest = ImportRequest(subjectId, topic, uri)
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
