package com.amadeusk.liftlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnnouncementsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Announcements", style = MaterialTheme.typography.titleLarge)
        Text(
            "App updates and notes will show up here.",
            style = MaterialTheme.typography.bodySmall
        )

        Divider()

        // Example placeholder announcements (replace with real content later)
        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("v1.0.0", style = MaterialTheme.typography.titleMedium)
                Text("• PR + bodyweight tracking\n• Graph improvements\n• Leaderboard screen",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Coming soon", style = MaterialTheme.typography.titleMedium)
                Text("• Cloud sync\n• Real leaderboards\n• More tools",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
