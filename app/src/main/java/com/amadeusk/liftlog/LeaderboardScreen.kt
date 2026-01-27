package com.amadeusk.liftlog

// Compose layout + list imports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Material UI
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// Compose runtime helpers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Data model
import com.amadeusk.liftlog.data.PR

// Math helper for double comparisons
import kotlin.math.abs

// Small holder object for leaderboard rows
private data class LeaderboardRow(
    val pr: PR,          // The PR record
    val weightKg: Double // Stored in KG internally
)

// Detects if an exercise name matches bench/squat/deadlift
private fun bigThreeKey(exercise: String): String? {
    val s = exercise.trim().lowercase()
    if (s.contains("deadlift") || s.contains("dead lift")) return "deadlift"
    if (s.contains("squat")) return "squat"
    if (s.contains("bench")) return "bench"
    return null
}

// Screen that shows top PRs and "big 3" totals
@Composable
fun LeaderboardScreen(
    prs: List<PR>,              // All PR records
    useKg: Boolean,             // Unit toggle for display
    modifier: Modifier = Modifier
) {
    // Build leaderboard rows: one best (heaviest) PR per exercise name
    val rows = remember(prs) {
        prs
            .groupBy { it.exercise.trim() } // Group PRs by exercise
            .mapNotNull { (_, exercisePrs) ->
                // Pick the best PR for that exercise
                val best = exercisePrs.maxWithOrNull(
                    compareBy<PR> { it.weight }                // heavier first
                        .thenBy { parsePrDateOrMin(it.date) }  // tie-break by date
                        .thenBy { it.id }                      // final tie-break by id
                )
                // Convert to row object
                best?.let { pr -> LeaderboardRow(pr = pr, weightKg = pr.weight) }
            }
            // Sort all exercises by best weight
            .sortedWith(
                compareByDescending<LeaderboardRow> { it.weightKg }
                    .thenByDescending { parsePrDateOrMin(it.pr.date) }
                    .thenByDescending { it.pr.id }
            )
            .take(25) // Only show top 25
    }

    // Find best bench/squat/deadlift PRs for the "club total"
    val bigThreeBestKg = remember(prs) {
        val best = mutableMapOf<String, PR>()

        for (pr in prs) {
            val key = bigThreeKey(pr.exercise) ?: continue
            val current = best[key]

            // Decide if this PR is better than the current best
            val better =
                current == null ||
                        pr.weight > current.weight ||
                        // If same weight, prefer the more recent date
                        (abs(pr.weight - current.weight) < 1e-9 &&
                                parsePrDateOrMin(pr.date) > parsePrDateOrMin(current.date)) ||
                        // If same weight + same date, prefer larger id (newer entry)
                        (abs(pr.weight - current.weight) < 1e-9 &&
                                parsePrDateOrMin(pr.date) == parsePrDateOrMin(current.date) &&
                                pr.id > current.id)

            if (better) best[key] = pr
        }

        best
    }

    // Read out each big 3 lift (default to 0 if missing)
    val benchKg = bigThreeBestKg["bench"]?.weight ?: 0.0
    val squatKg = bigThreeBestKg["squat"]?.weight ?: 0.0
    val deadliftKg = bigThreeBestKg["deadlift"]?.weight ?: 0.0

    // Total for the "club" number (bench + squat + deadlift)
    val clubTotalKg = benchKg + squatKg + deadliftKg

    // Main scrollable list
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Header / stats section
        item {
            Text("All Time Stats", style = MaterialTheme.typography.titleLarge)
            Text(
                "Top lifts based on your heaviest recorded PRs.",
                style = MaterialTheme.typography.bodySmall
            )

            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            // Total club number displayed (converted by formatWeight)
            Text(
                text = "${formatWeight(clubTotalKg, useKg)} Club",
                style = MaterialTheme.typography.titleMedium
            )

            // Bench stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bench", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (benchKg == 0.0) "—" else formatWeight(benchKg, useKg),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Squat stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Squat", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (squatKg == 0.0) "—" else formatWeight(squatKg, useKg),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Deadlift stat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Deadlift", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (deadliftKg == 0.0) "—" else formatWeight(deadliftKg, useKg),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
        }

        // Empty state (no PRs)
        if (rows.isEmpty()) {
            item {
                Text(
                    "No PRs yet. Add a PR to see the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@LazyColumn
        }

        // Leaderboard list items (one card per exercise best PR)
        items(rows, key = { it.pr.id }) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Top row: exercise name + best weight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row.pr.exercise, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatWeight(row.weightKg, useKg),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Extra info under the main row
                    Text("Reps: ${row.pr.reps}", style = MaterialTheme.typography.bodySmall)
                    Text("Date: ${row.pr.date}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Bottom padding
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
