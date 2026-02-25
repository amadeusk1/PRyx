package com.amadeusk.liftlog.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import com.amadeusk.liftlog.data.AcceptedSubmission
import com.amadeusk.liftlog.LiveLeaderboardViewModel
import com.amadeusk.liftlog.util.LB_TO_KG
import com.amadeusk.liftlog.util.formatWeight

private const val TOP_N = 5
private val EXERCISE_ORDER = listOf("bench" to "Bench", "squat" to "Squat", "deadlift" to "Deadlift")

/** Parse "100 kg" or "135 lb" (or "225") to weight in kg for correct sorting (100 kg > 135 lb). */
private fun parseWeightToKg(weightStr: String): Double {
    val num = weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return 0.0
    val lower = weightStr.lowercase()
    return if (lower.contains("lb")) num * LB_TO_KG else num // assume kg if no unit
}

private fun groupTop5ByExercise(submissions: List<AcceptedSubmission>): Map<String, List<AcceptedSubmission>> {
    val normalized = submissions.map { entry ->
        val ex = when (entry.exercise?.lowercase()) {
            "bench", "squat", "deadlift" -> entry.exercise.lowercase()
            else -> "bench"
        }
        entry.copy(exercise = ex)
    }
    return EXERCISE_ORDER.map { it.first }.associateWith { exerciseKey ->
        normalized
            .filter { it.exercise == exerciseKey }
            .sortedByDescending { parseWeightToKg(it.weight) }
            .take(TOP_N)
    }
}

@Composable
fun LiveLeaderboardScreen(
    viewModel: LiveLeaderboardViewModel,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showIntroAnimation by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2200)
        showIntroAnimation = false
    }

    if (showIntroAnimation) {
        LiveLeaderboardLoading(modifier = modifier.fillMaxSize())
        return
    }

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "contentAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .graphicsLayer { alpha = contentAlpha }
    ) {
        Text(
            text = "Live Leaderboard",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Top 5 per lift. Use the + button to submit a PR.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoadingList) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        } else if (uiState.acceptedSubmissions.isEmpty()) {
            Text(
                text = "No accepted submissions yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val byExercise = remember(uiState.acceptedSubmissions) {
                groupTop5ByExercise(uiState.acceptedSubmissions)
            }
            EXERCISE_ORDER.forEachIndexed { sectionIndex, (key, displayName) ->
                val list = byExercise[key].orEmpty()
                if (list.isNotEmpty()) {
                    var sectionVisible by remember(sectionIndex) { mutableStateOf(false) }
                    LaunchedEffect(sectionIndex) {
                        delay(sectionIndex * 80L)
                        sectionVisible = true
                    }
                    AnimatedVisibility(
                        visible = sectionVisible,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(300)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            list.forEachIndexed { cardIndex, entry ->
                                var cardVisible by remember(sectionIndex, cardIndex) { mutableStateOf(false) }
                                LaunchedEffect(sectionIndex, cardIndex) {
                                    delay((sectionIndex * 80L) + (cardIndex * 45L))
                                    cardVisible = true
                                }
                                AnimatedVisibility(
                                    visible = cardVisible,
                                    enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(280)
                                    )
                                ) {
                                    val rank = cardIndex + 1
                                    val medal = when (rank) {
                                        1 -> "🥇"
                                        2 -> "🥈"
                                        3 -> "🥉"
                                        else -> "$rank."
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = medal,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    text = entry.name,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                            Text(
                                                text = formatWeight(parseWeightToKg(entry.weight), useKg),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        when (uiState.status) {
            "accepted" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Accepted",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Your submission was accepted and appears on the live leaderboard.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.clearSubmission() }) {
                            Text("Submit another")
                        }
                    }
                }
            }
            "rejected" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Rejected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Your submission was not accepted. You can try again with different details.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.clearSubmission() }) {
                            Text("Try again")
                        }
                    }
                }
            }
            else -> {
                if (uiState.submissionId != null && uiState.status == "pending") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            Text(
                                text = "Pending review…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "ID: ${uiState.submissionId}",
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
