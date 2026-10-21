package ru.konspekt.app.ui

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.konspekt.app.data.AppViewModel
import ru.konspekt.app.ui.components.NewRecordingSheet
import ru.konspekt.app.ui.screens.home.HomeScreen
import ru.konspekt.app.ui.screens.library.LibraryScreen
import ru.konspekt.app.ui.screens.note.NoteScreen
import ru.konspekt.app.ui.screens.profile.ProfileScreen
import ru.konspekt.app.ui.screens.record.RecordScreen
import ru.konspekt.app.ui.screens.search.SearchScreen
import ru.konspekt.app.ui.screens.subjects.SubjectDetailScreen
import ru.konspekt.app.ui.screens.subjects.SubjectsScreen

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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isRoot = currentRoute == null || currentRoute in bottomDestinations.map { it.route }

    var showRecordingSheet by remember { mutableStateOf(false) }
    var preselectedSubject by remember { mutableStateOf<String?>(null) }

    fun openRecording(subjectId: String? = null) {
        preselectedSubject = subjectId
        showRecordingSheet = true
    }

    fun message(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(120)) }
        ) {
            composable("home") {
                HomeScreen(
                    userName = viewModel.settings.userName,
                    subjects = viewModel.subjects,
                    notes = viewModel.notes,
                    schedule = ru.konspekt.app.data.SeedData.schedule,
                    notesCount = viewModel::notesCount,
                    onOpenNote = { navController.navigate("note/$it") },
                    onOpenSubject = { navController.navigate("subject/$it") },
                    onAllSubjects = { navController.navigate("subjects") },
                    onAllNotes = { navController.navigate("library") },
                    onSearch = { navController.navigate("search") },
                    onProfile = { navController.navigate("profile") },
                    onStartRecording = { subjectId -> openRecording(subjectId) }
                )
            }

            composable("subjects") {
                SubjectsScreen(
                    subjects = viewModel.subjects,
                    notesCount = viewModel::notesCount,
                    totalMinutes = viewModel::totalMinutesForSubject,
                    onOpenSubject = { navController.navigate("subject/$it") },
                    onAddSubject = { name, tone, icon ->
                        viewModel.addSubject(name, tone, icon)
                        message("Предмет добавлен")
                    },
                    onProfile = { navController.navigate("profile") }
                )
            }

            composable("library") {
                LibraryScreen(
                    notes = viewModel.notes,
                    subjects = viewModel.subjects,
                    onOpenNote = { navController.navigate("note/$it") },
                    onSearch = { navController.navigate("search") },
                    onProfile = { navController.navigate("profile") }
                )
            }

            composable("search") {
                SearchScreen(
                    notes = viewModel.notes,
                    subjects = viewModel.subjects,
                    onOpenNote = { navController.navigate("note/$it") },
                    onOpenSubject = { navController.navigate("subject/$it") }
                )
            }

            composable("profile") {
                ProfileScreen(
                    settings = viewModel.settings,
                    notesCount = viewModel.notes.size,
                    subjectsCount = viewModel.subjects.size,
                    totalMinutes = viewModel.notes.sumOf { it.durationMinutes },
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
                        onRecord = { openRecording(subject.id) }
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
                )
            ) { entry ->
                val subjectId = entry.arguments?.getString("subjectId").orEmpty()
                val topic = entry.arguments?.getString("topic").orEmpty()
                val subject = viewModel.subject(subjectId)
                if (subject != null) {
                    RecordScreen(
                        subject = subject,
                        topic = topic,
                        demoMode = viewModel.settings.demoMode,
                        onClose = { navController.popBackStack() },
                        onFinish = { transcript, durationSeconds, demo ->
                            val note = viewModel.createNote(
                                subjectId = subject.id,
                                topic = topic,
                                transcript = transcript,
                                durationMinutes = durationSeconds.coerceAtLeast(1),
                                demo = demo
                            )
                            val currentId = navController.currentDestination?.id
                            navController.navigate("note/${note.id}") {
                                if (currentId != null) {
                                    popUpTo(currentId) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }

        if (isRoot) {
            KonspektBottomBar(
                currentRoute = currentRoute ?: "home",
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onRecord = { openRecording(null) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (isRoot) 96.dp else 16.dp
                )
        )
    }

    if (showRecordingSheet) {
        NewRecordingSheet(
            subjects = viewModel.subjects,
            initialSubjectId = preselectedSubject,
            filterTangents = viewModel.settings.filterTangents,
            demoMode = viewModel.settings.demoMode,
            onFilterChange = { enabled ->
                viewModel.updateSettings { it.copy(filterTangents = enabled) }
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

@Composable
private fun KonspektBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomDestinations.take(2).forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onRecord),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    onClick = onRecord,
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Начать запись",
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Запись",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            bottomDestinations.drop(2).forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: BottomDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .size(width = 52.dp, height = 30.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent,
                    shape = RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
