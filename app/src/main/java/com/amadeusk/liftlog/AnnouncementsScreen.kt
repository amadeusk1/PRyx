package com.amadeusk.liftlog

// Layout imports
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

// Material UI components
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// Compose runtime + UI utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Screen that displays app announcements and updates
@Composable
fun AnnouncementsScreen(
    modifier: Modifier = Modifier // Allows parent composables to modify layout
) {
    // Main vertical layout for the screen
    Column(
        modifier = modifier
            .fillMaxSize()      // Take up the full screen
            .padding(16.dp),    // Add padding around the content
        verticalArrangement = Arrangement.spacedBy(12.dp) // Space between items
    ) {

        // Screen title
        Text(
            "Announcements",
            style = MaterialTheme.typography.titleLarge
        )

        // Subtitle / description
        Text(
            "App updates and notes will show up here.",
            style = MaterialTheme.typography.bodySmall
        )

        // Horizontal divider
        Divider()

        // Placeholder card showing current version updates
        Card {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Version title
                Text(
                    "v1.0.0",
                    style = MaterialTheme.typography.titleMedium
                )

                // List of features in this version
                Text(
                    "• PR + bodyweight tracking\n• Graph improvements\n• Leaderboard screen",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Placeholder card showing upcoming features
        Card {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Section title
                Text(
                    "Coming soon",
                    style = MaterialTheme.typography.titleMedium
                )

                // List of planned features
                Text(
                    "• Cloud sync\n• Real leaderboards\n• More tools",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
