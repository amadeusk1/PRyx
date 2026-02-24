package com.amadeusk.liftlog

// Compose layouts + lists
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star

// Material UI
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.amadeusk.liftlog.currentActivityStreak
import com.amadeusk.liftlog.getDailyQuote

// App theme
import com.amadeusk.liftlog.ui.theme.LiftLogTheme

// Top-level pages (separate from the tab row)
private enum class TopPage { DASHBOARD, TRAINING, INFO, LEADERBOARD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftLogRoot(viewModel: PRViewModel) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    var useDarkTheme by remember(context, systemDark) {
        mutableStateOf(loadDarkTheme(context, systemDark))
    }

    LiftLogTheme(darkTheme = useDarkTheme) {
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

    // Main app layout structure (top bar + FAB + content)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PRyx") },

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

                // Right side: Leaderboard + Settings
                actions = {
                    // Open leaderboard page (disable button if already there)
                    IconButton(
                        onClick = { topPage = TopPage.LEADERBOARD },
                        enabled = topPage != TopPage.LEADERBOARD
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Leaderboard")
                    }

                    // Open settings dialog
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },

        floatingActionButton = {
            // Only show FAB on TRAINING and not on Tools tab
            if (topPage == TopPage.TRAINING && currentTab != LiftLogTab.TOOLS) {
                FloatingActionButton(
                    onClick = {
                        // Open the correct "add" dialog depending on tab
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
                        onOpenInfo = { topPage = TopPage.INFO }
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
private fun DashboardScreen(
    prs: List<PR>,
    bodyWeights: List<BodyWeightEntry>,
    useKg: Boolean,
    onOpenExercise: (String) -> Unit,
    onOpenBodyweight: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenInfo: () -> Unit
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------- DAILY QUOTE --------
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "\"${quote.text}\"",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = quote.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // -------- STREAK --------
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Daily streak", style = MaterialTheme.typography.titleSmall)
                if (streak > 0) {
                    Text(
                        text = "$streak day${if (streak == 1) "" else "s"} logged in a row",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Log a PR or bodyweight today to start your streak.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // -------- LIFTS SECTION --------
        Text(
            text = "Lifts",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                coreLifts.forEachIndexed { index, lift ->
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
                            Text(
                                text = lift,
                                style = MaterialTheme.typography.titleSmall
                            )

                            val latest = liftPrs.lastOrNull()
                            if (latest != null) {
                                Text(
                                    text = formatWeight(latest.weight, useKg),
                                    style = MaterialTheme.typography.labelSmall
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
                                    .height(140.dp)
                                    .padding(top = 6.dp),
                                showAxisLabels = false,
                                showTitle = false,
                                showGrid = false
                            )
                        } else {
                            Text(
                                text = "No PRs logged yet. Tap to add and view more.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (index != coreLifts.lastIndex) {
                        Divider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }

        // -------- BODYWEIGHT SECTION --------
        Text(
            text = "Bodyweight",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenBodyweight() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Text(
                        text = "Bodyweight",
                        style = MaterialTheme.typography.titleSmall
                    )

                    val latest = bwPreview.lastOrNull()
                    if (latest != null) {
                        Text(
                            text = formatWeight(latest.weight, useKg),
                            style = MaterialTheme.typography.labelSmall
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
                            .height(160.dp)
                            .padding(top = 6.dp),
                        showAxisLabels = false,
                        showTitle = false,
                        showGrid = false
                    )
                } else {
                    Text(
                        text = "No bodyweight entries yet. Tap to add your first entry.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Other feature previews
        Text(
            text = "More from PRyx",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTools() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Tools & Calculators", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "TDEE, 1RM, protein needs, and body fat calculators.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenLeaderboard() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Leaderboard", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "See how your PRs stack up.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenInfo() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("App Info", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Tips, how PRyx works, and more.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
