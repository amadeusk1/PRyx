package com.amadeusk.liftlog

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
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amadeusk.liftlog.data.PR
import kotlin.math.abs

private data class LeaderboardRow(
    val pr: PR,
    val weightKg: Double,
)

private fun bigThreeKey(exercise: String): String? {
    val s = exercise.trim().lowercase()
    if (s.contains("deadlift") || s.contains("dead lift")) return "deadlift"
    if (s.contains("squat")) return "squat"
    if (s.contains("bench")) return "bench"
    return null
}

@Composable
fun LeaderboardScreen(
    prs: List<PR>,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    // One best (heaviest) PR per exercise
    val rows = remember(prs) {
        prs
            .groupBy { it.exercise.trim() }
            .mapNotNull { (_, exercisePrs) ->
                val best = exercisePrs.maxWithOrNull(
                    compareBy<PR> { it.weight }
                        .thenBy { parsePrDateOrMin(it.date) }
                        .thenBy { it.id }
                )
                best?.let { pr -> LeaderboardRow(pr = pr, weightKg = pr.weight) }
            }
            .sortedWith(
                compareByDescending<LeaderboardRow> { it.weightKg }
                    .thenByDescending { parsePrDateOrMin(it.pr.date) }
                    .thenByDescending { it.pr.id }
            )
            .take(25)
    }

    // Best PR for each of Bench/Squat/Deadlift (missing lifts = 0)
    val bigThreeBestKg = remember(prs) {
        val best = mutableMapOf<String, PR>()

        for (pr in prs) {
            val key = bigThreeKey(pr.exercise) ?: continue
            val current = best[key]

            val better =
                current == null ||
                        pr.weight > current.weight ||
                        (abs(pr.weight - current.weight) < 1e-9 &&
                                parsePrDateOrMin(pr.date) > parsePrDateOrMin(current.date)) ||
                        (abs(pr.weight - current.weight) < 1e-9 &&
                                parsePrDateOrMin(pr.date) == parsePrDateOrMin(current.date) &&
                                pr.id > current.id)

            if (better) best[key] = pr
        }

        best
    }

    val benchKg = bigThreeBestKg["bench"]?.weight ?: 0.0
    val squatKg = bigThreeBestKg["squat"]?.weight ?: 0.0
    val deadliftKg = bigThreeBestKg["deadlift"]?.weight ?: 0.0
    val clubTotalKg = benchKg + squatKg + deadliftKg

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("All Time Stats", style = MaterialTheme.typography.titleLarge)
            Text(
                "Top lifts based on your heaviest recorded PRs.",
                style = MaterialTheme.typography.bodySmall
            )

            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            // Plain-text stats (no bubble)
            Text(
                text = "${formatWeight(clubTotalKg, useKg)} Club",
                style = MaterialTheme.typography.titleMedium
            )

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

        if (rows.isEmpty()) {
            item {
                Text(
                    "No PRs yet. Add a PR to see the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@LazyColumn
        }

        // Individual lifts in Cards ✅
        items(rows, key = { it.pr.id }) { row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
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

                    Text("Reps: ${row.pr.reps}", style = MaterialTheme.typography.bodySmall)
                    Text("Date: ${row.pr.date}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}
