package com.amadeusk.liftlog

import androidx.activity.compose.BackHandler
// Compose layouts + lists
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star

// Material UI
import androidx.compose.material3.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Data models + file storage for bodyweights
import com.amadeusk.liftlog.data.BodyWeightEntry
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.loadBodyWeightsFromFile
import com.amadeusk.liftlog.data.saveBodyWeightsToFile
import com.amadeusk.liftlog.data.loadUseKg
import com.amadeusk.liftlog.data.saveUseKg
import com.amadeusk.liftlog.data.loadDarkTheme
import com.amadeusk.liftlog.data.saveDarkTheme
import com.amadeusk.liftlog.ui.charts.BodyWeightGraph
import com.amadeusk.liftlog.ui.charts.ExerciseGraph
import com.amadeusk.liftlog.ui.components.BodyWeightDialog
import com.amadeusk.liftlog.ui.components.BodyWeightItem
import com.amadeusk.liftlog.ui.components.ExerciseSelector
import com.amadeusk.liftlog.ui.components.GraphRangeSelector
import com.amadeusk.liftlog.ui.components.PRItem
import com.amadeusk.liftlog.ui.components.LiveLeaderboardSubmitDialog
import com.amadeusk.liftlog.ui.components.PrDialog
import com.amadeusk.liftlog.ui.components.RepRangeSelector
import com.amadeusk.liftlog.ui.screens.InfoScreen
import com.amadeusk.liftlog.ui.screens.LeaderboardScreen
import com.amadeusk.liftlog.ui.screens.LiveLeaderboardScreen
import com.amadeusk.liftlog.ui.screens.SplashScreen
import com.amadeusk.liftlog.ui.screens.ToolsScreen
import com.amadeusk.liftlog.util.GraphRange
import com.amadeusk.liftlog.util.LiftLogTab
import com.amadeusk.liftlog.util.RepRange
import com.amadeusk.liftlog.util.StrengthTrend
import com.amadeusk.liftlog.util.ThisWeekSnapshot
import com.amadeusk.liftlog.util.computeThisWeekSnapshot
import com.amadeusk.liftlog.util.KG_TO_LB
import com.amadeusk.liftlog.util.currentActivityStreak
import com.amadeusk.liftlog.util.filterBodyWeightsByRange
import com.amadeusk.liftlog.util.filterPrsByRange
import com.amadeusk.liftlog.util.filterPrsByRepRange
import com.amadeusk.liftlog.util.formatWeight
import com.amadeusk.liftlog.util.fromDisplayWeight
import com.amadeusk.liftlog.util.getDailyQuote
import com.amadeusk.liftlog.util.parsePrDateOrMin
import com.amadeusk.liftlog.util.toDisplayWeight
import java.time.LocalDate

// App theme
import com.amadeusk.liftlog.ui.theme.LiftLogTheme

// Top-level pages (separate from the tab row)
private enum class TopPage { DASHBOARD, TRAINING, INFO, LEADERBOARD, LIVE_LEADERBOARD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogRoot(viewModel: PRViewModel) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    var useDarkTheme by remember(context, systemDark) {
        mutableStateOf(loadDarkTheme(context, systemDark))
    }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2200)
        showSplash = false
    }

    LiftLogTheme(darkTheme = useDarkTheme) {
        if (showSplash) {
            SplashScreen()
        } else {
            LiftLogRootContent(
                viewModel = viewModel,
                useDarkTheme = useDarkTheme,
                onDarkThemeChange = { enabled ->
                    useDarkTheme = enabled
                    saveDarkTheme(context, enabled)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiftLogRootContent(
    viewModel: PRViewModel,
    useDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    // Android context (used for file I/O + dialogs)
    val context = LocalContext.current

    // UI state coming from ViewModel (PR list, etc.)
    val uiState by viewModel.uiState.collectAsState()

    // Current selected tab (PRS / BODYWEIGHT / TOOLS)
    var currentTab by remember { mutableStateOf(LiftLogTab.PRS) }

    // PR dialogs + editing state
    var showAddPrDialog by remember { mutableStateOf(false) }
    var prBeingEdited by remember { mutableStateOf<PR?>(null) }

    // Extract list of distinct exercise names from PRs
    val exercises = uiState.prs.map { it.exercise }.distinct()

    // Exercise suggestions shown in the PR dialog
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")
    val exerciseSuggestions = (coreLifts + exercises).distinct()

    // Selected exercise for the graph + list filter
    var selectedExercise by remember(exercises) {
        mutableStateOf(exercises.firstOrNull())
    }

    // Currently selected point on PR graph
    var selectedGraphPr by remember { mutableStateOf<PR?>(null) }

    // Shared range selector for both graphs (month/year/all)
    var selectedRange by remember { mutableStateOf(GraphRange.MONTH) }

    // Rep filter selector for PRs (1/3/6/8/all)
    var selectedRepRange by remember { mutableStateOf(RepRange.ALL) }

    // Bodyweight entries (loaded from file, stored locally in this screen)
    var bodyWeights by remember { mutableStateOf(loadBodyWeightsFromFile(context)) }

    // Bodyweight dialogs + editing state
    var showAddBwDialog by remember { mutableStateOf(false) }
    var bwBeingEdited by remember { mutableStateOf<BodyWeightEntry?>(null) }

    // Currently selected point on bodyweight graph
    var selectedBwEntry by remember { mutableStateOf<BodyWeightEntry?>(null) }

    // Unit toggle (true = kg, false = lb)
    var useKg by remember(context) { mutableStateOf(loadUseKg(context, default = true)) }

    // Settings dialog flag
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Which top-level page we are currently showing
    var topPage by remember { mutableStateOf(TopPage.DASHBOARD) }

    // Live leaderboard: submit dialog + ViewModel (for FAB and dialog)
    var showLiveSubmitDialog by remember { mutableStateOf(false) }
    val liveLeaderboardViewModel: LiveLeaderboardViewModel = viewModel()

    // Main app layout structure (top bar + FAB + content)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PR", color = MaterialTheme.colorScheme.onSurface)
                        Text("yx", color = MaterialTheme.colorScheme.primary)
                    }
                },

                // Left side: Info / Back + Announcements icon
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle between dashboard and info screen
                        TextButton(
                            onClick = {
                                topPage = if (topPage == TopPage.INFO) {
                                    TopPage.DASHBOARD
                                } else {
                                    TopPage.INFO
                                }
                            }
                        ) {
                            Text(if (topPage == TopPage.INFO) "Back" else "Info")
                        }

                        // Home button (go back to dashboard)
                        IconButton(
                            onClick = { topPage = TopPage.DASHBOARD },
                            enabled = topPage != TopPage.DASHBOARD
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = "Home")
                        }
                    }
                },

                // Right side: Live leaderboard (trophy) + Personal leaderboard (star) + Settings
                actions = {
                    // Live leaderboard — trophy icon (left of star)
                    IconButton(
                        onClick = { topPage = TopPage.LIVE_LEADERBOARD },
                        enabled = topPage != TopPage.LIVE_LEADERBOARD
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Live leaderboard")
                    }

                    // Personal leaderboard — star icon
                    IconButton(
                        onClick = { topPage = TopPage.LEADERBOARD },
                        enabled = topPage != TopPage.LEADERBOARD
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Personal leaderboard")
                    }

                    // Open settings dialog
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },

        floatingActionButton = {
            when {
                topPage == TopPage.LIVE_LEADERBOARD -> {
                    FloatingActionButton(onClick = { showLiveSubmitDialog = true }) {
                        Text("+")
                    }
                }
                topPage == TopPage.TRAINING && currentTab != LiftLogTab.TOOLS -> {
                    FloatingActionButton(
                        onClick = {
                            when (currentTab) {
                                LiftLogTab.PRS -> showAddPrDialog = true
                                LiftLogTab.BODYWEIGHT -> showAddBwDialog = true
                                else -> {}
                            }
                        }
                    ) {
                        Text("+")
                    }
                }
            }
        }
    ) { innerPadding ->

        // Main content container
        Column(
            modifier = Modifier
                .padding(innerPadding) // Padding provided by Scaffold
                .fillMaxSize()
        ) {

            // If user is on a top-level page, show it and exit early
            when (topPage) {
                TopPage.DASHBOARD -> {
                    DashboardScreen(
                        prs = uiState.prs,
                        bodyWeights = bodyWeights,
                        useKg = useKg,
                        onOpenExercise = { exercise ->
                            selectedExercise = exercise
                            selectedGraphPr = null
                            selectedRange = GraphRange.MONTH
                            selectedRepRange = RepRange.ALL
                            currentTab = LiftLogTab.PRS
                            topPage = TopPage.TRAINING
                        },
                        onOpenBodyweight = {
                            selectedBwEntry = null
                            selectedRange = GraphRange.MONTH
                            currentTab = LiftLogTab.BODYWEIGHT
                            topPage = TopPage.TRAINING
                        },
                        onOpenTools = {
                            currentTab = LiftLogTab.TOOLS
                            topPage = TopPage.TRAINING
                        },
                        onOpenLeaderboard = { topPage = TopPage.LEADERBOARD },
                        onOpenLogDifferentExercise = {
                            currentTab = LiftLogTab.PRS
                            topPage = TopPage.TRAINING
                            showAddPrDialog = true
                        }
                    )
                    return@Column
                }
                TopPage.INFO -> {
                    InfoScreen()
                    return@Column
                }
                TopPage.LEADERBOARD -> {
                    LeaderboardScreen(
                        prs = uiState.prs,
                        useKg = useKg,
                        modifier = Modifier.fillMaxSize()
                    )
                    return@Column
                }
                TopPage.LIVE_LEADERBOARD -> {
                    LiveLeaderboardScreen(
                        viewModel = liveLeaderboardViewModel,
                        useKg = useKg,
                        modifier = Modifier.fillMaxSize()
                    )
                    return@Column
                }
                else -> {}
            }

            // ------------------ TAB BAR (MAIN PAGE) ------------------
            if (topPage == TopPage.TRAINING) {
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = currentTab == LiftLogTab.PRS,
                        onClick = { currentTab = LiftLogTab.PRS },
                        text = { Text("PRs") }
                    )
                    Tab(
                        selected = currentTab == LiftLogTab.BODYWEIGHT,
                        onClick = { currentTab = LiftLogTab.BODYWEIGHT },
                        text = { Text("Bodyweight") }
                    )
                    Tab(
                        selected = currentTab == LiftLogTab.TOOLS,
                        onClick = { currentTab = LiftLogTab.TOOLS },
                        text = { Text("Tools") }
                    )
                }
            }

            // ------------------ TAB CONTENT ------------------
            if (topPage == TopPage.TRAINING) {
                when (currentTab) {

                    // ---------- PR TAB ----------
                    LiftLogTab.PRS -> {
                    // Only show selector + graph if we have exercises
                    if (exercises.isNotEmpty()) {
                        // Exercise dropdown
                        ExerciseSelector(
                            exercises = exercises,
                            selectedExercise = selectedExercise,
                            onExerciseSelected = { exercise ->
                                selectedExercise = exercise
                                selectedGraphPr = null // clear selected point
                            }
                        )

                        RepRangeSelector(
                            selectedRange = selectedRepRange,
                            onRangeSelected = { rr ->
                                selectedRepRange = rr
                                selectedGraphPr = null // clear selected point
                            }
                        )

                        // Range selector (month/year/all)
                        GraphRangeSelector(
                            selectedRange = selectedRange,
                            onRangeSelected = { range ->
                                selectedRange = range
                                selectedGraphPr = null // clear selected point
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter PRs to selected exercise + selected range
                        val prsForSelected = filterPrsByRange(
                            filterPrsByRepRange(
                                uiState.prs.filter { it.exercise == selectedExercise },
                                selectedRepRange
                            ),
                            selectedRange
                        ).sortedBy { parsePrDateOrMin(it.date) }

                        // PR graph
                        ExerciseGraph(
                            prs = prsForSelected,
                            selectedPr = selectedGraphPr,
                            onPointSelected = { pr -> selectedGraphPr = pr },
                            useKg = useKg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                        )

                        // If a graph point is selected, show a details card + edit/delete
                        selectedGraphPr?.let { pr ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(pr.exercise, style = MaterialTheme.typography.titleSmall)
                                    Text("Weight: ${formatWeight(pr.weight, useKg)}")
                                    Text("Reps: ${pr.reps}")
                                    Text("Date: ${pr.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { prBeingEdited = pr }) { Text("Edit") }
                                        TextButton(onClick = {
                                            viewModel.deletePr(pr)
                                            selectedGraphPr = null
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Global empty state (no PRs at all)
                    if (uiState.prs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("No PRs yet. Tap + to add your first one.") }
                    } else {
                        // PR history list for selected exercise + selected range
                        val history = if (selectedExercise != null) {
                            filterPrsByRange(
                                filterPrsByRepRange(
                                    uiState.prs.filter { it.exercise == selectedExercise },
                                    selectedRepRange
                                ),
                                selectedRange
                            ).sortedByDescending { parsePrDateOrMin(it.date) }
                        } else emptyList()

                        // Empty state for this exercise/range
                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("No PRs yet for this exercise in this range.") }
                        } else {
                            // List of PR cards
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it.id }) { pr ->
                                    PRItem(
                                        pr = pr,
                                        useKg = useKg,
                                        onDelete = {
                                            viewModel.deletePr(pr)
                                            // If we deleted the selected graph point, clear it
                                            if (selectedGraphPr?.id == pr.id) {
                                                selectedGraphPr = null
                                            }
                                        },
                                        onEdit = { prBeingEdited = pr }
                                    )
                                }
                            }
                        }
                    }
                    }

                    // ---------- BODYWEIGHT TAB ----------
                    LiftLogTab.BODYWEIGHT -> {
                    // Empty state (no entries)
                    if (bodyWeights.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("No bodyweight entries yet. Tap + to add one.") }
                    } else {
                        // Range selector for bodyweight graph
                        GraphRangeSelector(
                            selectedRange = selectedRange,
                            onRangeSelected = { range ->
                                selectedRange = range
                                selectedBwEntry = null // clear selected point
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter bodyweights to selected range
                        val bwForRange = filterBodyWeightsByRange(bodyWeights, selectedRange)
                            .sortedBy { parsePrDateOrMin(it.date) }

                        // Bodyweight graph
                        BodyWeightGraph(
                            entries = bwForRange,
                            selectedEntry = selectedBwEntry,
                            onPointSelected = { selectedBwEntry = it },
                            useKg = useKg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp)
                        )

                        // If graph point selected, show details + edit/delete
                        selectedBwEntry?.let { entry ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Bodyweight", style = MaterialTheme.typography.titleSmall)
                                    Text("Weight: ${formatWeight(entry.weight, useKg)}")
                                    Text("Date: ${entry.date}")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { bwBeingEdited = entry }) { Text("Edit") }
                                        TextButton(onClick = {
                                            // Remove entry and save updated list to file
                                            bodyWeights = bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)
                                            selectedBwEntry = null
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // History list in this range (newest first)
                        val history = bwForRange.sortedByDescending { parsePrDateOrMin(it.date) }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("No bodyweight entries in this range.") }
                        } else {
                            // List of bodyweight cards
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history, key = { it.id }) { entry ->
                                    BodyWeightItem(
                                        entry = entry,
                                        useKg = useKg,
                                        onEdit = { bwBeingEdited = entry },
                                        onDelete = {
                                            // Remove entry and save updated list to file
                                            bodyWeights = bodyWeights.filterNot { it.id == entry.id }
                                            saveBodyWeightsToFile(context, bodyWeights)

                                            // Clear selected point if it was deleted
                                            if (selectedBwEntry?.id == entry.id) selectedBwEntry = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }

                    // ---------- TOOLS TAB ----------
                    LiftLogTab.TOOLS -> ToolsScreen()
                }
            }
        }
    }

    // ------------------ SETTINGS DIALOG ------------------
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Close") }
            },
            title = { Text("Units") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = useKg,
                            onClick = {
                                useKg = true
                                saveUseKg(context, true)
                            }
                        )
                        Text("Kilograms (kg)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !useKg,
                            onClick = {
                                useKg = false
                                saveUseKg(context, false)
                            }
                        )
                        Text("Pounds (lb)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Appearance", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark mode", modifier = Modifier.weight(1f))
                        Switch(
                            checked = useDarkTheme,
                            onCheckedChange = { onDarkThemeChange(it) }
                        )
                    }
                }
            }
        )
    }

    // ------------------ ADD PR DIALOG ------------------
    if (showAddPrDialog) {
        PrDialog(
            title = "Add PR",
            confirmButtonText = "Save",
            initialExercise = "",
            initialWeight = "",
            initialReps = "",
            initialDate = "",
            useKg = useKg,
            exerciseSuggestions = exerciseSuggestions, // suggestions for the exercise field
            onDismiss = { showAddPrDialog = false },
            onConfirm = { exercise, weightStr, repsStr, date ->
                // Convert from display units (kg/lb) into stored kg value
                val raw = weightStr.toDoubleOrNull() ?: 0.0
                val weightKg = raw.fromDisplayWeight(useKg)

                // Add PR to ViewModel/store
                viewModel.addPr(
                    exercise = exercise,
                    weight = weightKg,
                    reps = repsStr.toIntOrNull() ?: 1,
                    date = date
                )
                showAddPrDialog = false
            }
        )
    }

    // ------------------ EDIT PR DIALOG ------------------
    prBeingEdited?.let { pr ->
        PrDialog(
            title = "Edit PR",
            confirmButtonText = "Update",
            initialExercise = pr.exercise,
            initialWeight = formatWeight(pr.weight, useKg),
            initialReps = pr.reps.toString(),
            initialDate = pr.date,
            useKg = useKg,
            exerciseSuggestions = exerciseSuggestions,
            onDismiss = { prBeingEdited = null },
            onConfirm = { exercise, weightStr, repsStr, date ->
                // Convert from display units (kg/lb) back into stored kg
                val newWeightKg = weightStr.toDoubleOrNull()?.fromDisplayWeight(useKg) ?: pr.weight

                // Update the PR in ViewModel/store
                viewModel.updatePr(
                    pr.copy(
                        exercise = exercise,
                        weight = newWeightKg,
                        reps = repsStr.toIntOrNull() ?: pr.reps,
                        date = date
                    )
                )
                prBeingEdited = null
            }
        )
    }

    // ------------------ LIVE LEADERBOARD SUBMIT DIALOG ------------------
    if (showLiveSubmitDialog) {
        LiveLeaderboardSubmitDialog(
            viewModel = liveLeaderboardViewModel,
            useKg = useKg,
            onDismiss = { showLiveSubmitDialog = false }
        )
    }

    // ------------------ ADD BODYWEIGHT DIALOG ------------------
    if (showAddBwDialog) {
        BodyWeightDialog(
            title = "Add bodyweight",
            confirmButtonText = "Save",
            initialWeight = "",
            initialDate = "",
            useKg = useKg,
            onDismiss = { showAddBwDialog = false },
            onConfirm = { weightStr, date ->
                // Convert from display units (kg/lb) into stored kg
                val raw = weightStr.toDoubleOrNull() ?: 0.0
                val weightKg = raw.fromDisplayWeight(useKg)

                // Create new entry
                val newEntry = BodyWeightEntry(
                    id = System.currentTimeMillis(),
                    date = date,
                    weight = weightKg
                )

                // Add + persist to file
                bodyWeights = bodyWeights + newEntry
                saveBodyWeightsToFile(context, bodyWeights)
                showAddBwDialog = false
            }
        )
    }

    // ------------------ EDIT BODYWEIGHT DIALOG ------------------
    bwBeingEdited?.let { entry ->
        BodyWeightDialog(
            title = "Edit bodyweight",
            confirmButtonText = "Update",
            initialWeight = formatWeight(entry.weight, useKg),
            initialDate = entry.date,
            useKg = useKg,
            onDismiss = { bwBeingEdited = null },
            onConfirm = { weightStr, date ->
                // Convert from display units (kg/lb) back into stored kg
                val newWeightKg = weightStr.toDoubleOrNull()?.fromDisplayWeight(useKg) ?: entry.weight

                // Update entry in list
                val updated = entry.copy(weight = newWeightKg, date = date)
                bodyWeights = bodyWeights.map { if (it.id == entry.id) updated else it }

                // Persist updated list to file
                saveBodyWeightsToFile(context, bodyWeights)
                bwBeingEdited = null
            }
        )
    }
}

@Composable
private fun AnimatedDashboardSection(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        visible = true
    }
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        animationSpec = tween(280),
        label = "alpha"
    )
    val offsetY by animateFloatAsState(
        if (visible) 0f else 20f,
        animationSpec = tween(280),
        label = "offset"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        }
    ) {
        content()
    }
}

@Composable
private fun DashboardScreen(
    prs: List<PR>,
    bodyWeights: List<BodyWeightEntry>,
    useKg: Boolean,
    onOpenExercise: (String) -> Unit,
    onOpenBodyweight: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenLogDifferentExercise: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coreLifts = listOf("Bench Press", "Squat", "Deadlift")
    val quote = remember { getDailyQuote() }
    val streak = remember(prs, bodyWeights) {
        currentActivityStreak(prs = prs, bodyWeights = bodyWeights)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // -------- DAILY QUOTE --------
        AnimatedDashboardSection(0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daily quote",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "\u201C${quote.text}\u201D",
                            style = MaterialTheme.typography.titleMedium,
                            lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "\u2014 ${quote.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // -------- STREAK --------
        AnimatedDashboardSection(1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (streak > 0)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (streak > 0)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Daily streak",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (streak > 0) {
                            Text(
                                text = "$streak day${if (streak == 1) "" else "s"}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "logged in a row",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Log a PR or bodyweight today to start.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // -------- LIFTS SECTION --------
        AnimatedDashboardSection(2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                val liftColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary
                )
                for ((index, lift) in coreLifts.withIndex()) {
                    val liftPrs = remember(prs, lift) {
                        prs
                            .filter { it.exercise == lift }
                            .sortedByDescending { parsePrDateOrMin(it.date) }
                            .take(5)
                            .sortedBy { parsePrDateOrMin(it.date) }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenExercise(lift) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .widthIn(min = 0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(4.dp, 24.dp),
                                    shape = RoundedCornerShape(2.dp),
                                    color = liftColors.getOrNull(index) ?: MaterialTheme.colorScheme.primary
                                ) {}
                                Text(
                                    text = lift,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val latest = liftPrs.lastOrNull()
                            if (latest != null) {
                                Text(
                                    text = formatWeight(latest.weight, useKg),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        if (liftPrs.isNotEmpty()) {
                            ExerciseGraph(
                                prs = liftPrs,
                                selectedPr = null,
                                onPointSelected = {},
                                useKg = useKg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(152.dp)
                                    .padding(top = 8.dp),
                                showAxisLabels = false,
                                showTitle = false,
                                showGrid = false,
                                lineColor = liftColors.getOrNull(index),
                                interactive = false
                            )
                        } else {
                            Text(
                                text = "No PRs logged yet. Tap to add and view more.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (index != coreLifts.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLogDifferentExercise() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log different exercise",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        }

        // -------- BODYWEIGHT SECTION --------
        AnimatedDashboardSection(3) {
            Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBodyweight() },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bwPreview = remember(bodyWeights) {
                        bodyWeights
                            .sortedByDescending { parsePrDateOrMin(it.date) }
                            .take(5)
                            .sortedBy { parsePrDateOrMin(it.date) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val latest = bwPreview.lastOrNull()
                        if (latest != null) {
                            Text(
                                text = formatWeight(latest.weight, useKg),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                if (bwPreview.isNotEmpty()) {
                    BodyWeightGraph(
                        entries = bwPreview,
                        selectedEntry = null,
                        onPointSelected = {},
                        useKg = useKg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .padding(top = 8.dp),
                        showAxisLabels = false,
                        showTitle = false,
                        showGrid = false,
                        lineColor = MaterialTheme.colorScheme.tertiary,
                        interactive = false
                    )
                } else {
                    Text(
                        text = "No bodyweight entries yet. Tap to add your first entry.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // -------- TOOLS SHORTCUT --------
        AnimatedDashboardSection(4) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTools() },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tools",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "TDEE, 1RM, protein, body fat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // -------- THIS WEEK SNAPSHOT --------
        val weekSnapshot = remember(prs, bodyWeights, useKg) {
            computeThisWeekSnapshot(prs, bodyWeights, useKg)
        }
        AnimatedDashboardSection(5) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${weekSnapshot.weekRangeLabel} \u00b7 vs previous 7 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sessions", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${weekSnapshot.sessionsThisWeek} day${if (weekSnapshot.sessionsThisWeek == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sets logged", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${weekSnapshot.totalSetsThisWeek}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Exercises", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${weekSnapshot.exercisesTracked}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = buildString {
                                val vol = weekSnapshot.volumeThisWeekKg
                                append(if (vol >= 1000) "%.1fk".format(vol / 1000) else "%.0f".format(vol))
                                append(" kg")
                                weekSnapshot.volumeVsLastWeekPercent?.let { pct ->
                                    append(" ")
                                    append(if (pct >= 0) "+" else "")
                                    append("%.0f%%".format(pct))
                                    append(" vs last")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Avg reps \u00b7 intensity", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (weekSnapshot.totalSetsThisWeek > 0)
                                "~${"%.0f".format(weekSnapshot.avgReps)} \u00b7 ${weekSnapshot.avgIntensity}"
                            else "\u2014",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("BW change", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = weekSnapshot.bwChangeKg?.let { delta ->
                                val sign = if (delta >= 0) "+" else "\u2013"
                                val abs = kotlin.math.abs(delta)
                                val displayVal = if (useKg) abs else abs * KG_TO_LB
                                "$sign${"%.1f".format(displayVal)} ${if (useKg) "kg" else "lb"}"
                            } ?: "\u2014",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Strength trend", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = when (weekSnapshot.strengthTrend) {
                                StrengthTrend.UP -> "\u2191 Up"
                                StrengthTrend.DOWN -> "\u2193 Down"
                                StrengthTrend.STABLE -> "\u2192 Stable"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (weekSnapshot.strengthTrend) {
                                StrengthTrend.UP -> MaterialTheme.colorScheme.primary
                                StrengthTrend.DOWN -> MaterialTheme.colorScheme.error
                                StrengthTrend.STABLE -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fatigue estimate", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = weekSnapshot.fatigueEstimate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            }
        }

        // Other feature previews
        AnimatedDashboardSection(6) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "More from PRyx",
                    style = MaterialTheme.typography.titleLarge
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTools() },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tools & Calculators", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "TDEE, 1RM, protein needs, and body fat calculators.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLeaderboard() },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "See how your PRs stack up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
