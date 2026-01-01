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

private data class LeaderboardRow(
    val pr: PR,
    val weightKg: Double,
)

@Composable
fun LeaderboardScreen(
    prs: List<PR>,
    useKg: Boolean,
    modifier: Modifier = Modifier
) {
    val rows = remember(prs) {
        prs
            .map { pr -> LeaderboardRow(pr = pr, weightKg = pr.weight) } // actual weight
            .sortedWith(
                compareByDescending<LeaderboardRow> { it.weightKg }
                    .thenByDescending { parsePrDateOrMin(it.pr.date) }
                    .thenByDescending { it.pr.id }
            )
            .take(25)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Leaderboard",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Top lifts based on your heaviest recorded PRs.",
            style = MaterialTheme.typography.bodySmall
        )

        Divider()

        if (rows.isEmpty()) {
            Text(
                text = "No PRs yet. Add a PR to see the leaderboard.",
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { it.pr.id }) { row ->
                val weightDisplay = row.weightKg.toDisplayWeight(useKg)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = row.pr.exercise,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = formatWeight(weightDisplay, useKg),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Reps: ${row.pr.reps}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Date: ${row.pr.date}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tip: this is local-only for now (no online ranking yet).",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
