package com.amadeusk.liftlog

// Android activity base class
import android.os.Bundle

// Jetpack Compose activity setup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// ViewModel support (creates and survives config changes)
import androidx.activity.viewModels

// Material UI
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

// Compose layout
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

// App theme
import com.amadeusk.liftlog.ui.theme.LiftLogTheme

// Main entry point activity for the app
class MainActivity : ComponentActivity() {

    // ViewModel that stores PR data + app state
    private val prViewModel: PRViewModel by viewModels()

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the UI content using Jetpack Compose
        setContent {
            // Apply the app theme
            LiftLogTheme {
                // Surface gives a consistent background + theming
                Surface(
                    modifier = Modifier.fillMaxSize(), // Fill full screen
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Root composable that controls navigation + main screens
                    LiftLogRoot(prViewModel)
                }
            }
        }
    }
}
