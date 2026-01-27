package com.amadeusk.liftlog

// Android app context for file I/O
import android.app.Application

// ViewModel base class with access to Application
import androidx.lifecycle.AndroidViewModel

// Data model + file helpers
import com.amadeusk.liftlog.data.PR
import com.amadeusk.liftlog.data.loadPrsFromFile
import com.amadeusk.liftlog.data.savePrsToFile

// StateFlow for reactive UI updates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// UI state container (what the UI reads)
data class PRUiState(
    val prs: List<PR> = emptyList() // All saved PRs
)

// ViewModel that manages PR list + persistence
class PRViewModel(application: Application) : AndroidViewModel(application) {

    // Internal mutable state
    private val _uiState = MutableStateFlow(
        PRUiState(
            // Load PRs from file once at startup
            prs = loadPrsFromFile(application)
        )
    )

    // Public read-only state for the UI
    val uiState: StateFlow<PRUiState> = _uiState

    // Saves current PR list to file
    private fun persist() {
        savePrsToFile(getApplication(), _uiState.value.prs)
    }

    // Add a new PR entry
    fun addPr(exercise: String, weight: Double, reps: Int, date: String) {
        val pr = PR(
            exercise = exercise.trim(),
            weight = weight,
            reps = reps,
            date = date.trim()
        )

        // Update state (adds new PR to list)
        _uiState.update { it.copy(prs = it.prs + pr) }

        // Persist changes to disk
        persist()
    }

    // Delete an existing PR entry
    fun deletePr(pr: PR) {
        _uiState.update { it.copy(prs = it.prs - pr) }
        persist()
    }

    // Update an existing PR entry (matched by ID)
    fun updatePr(updated: PR) {
        _uiState.update { state ->
            state.copy(
                prs = state.prs.map { pr ->
                    if (pr.id == updated.id) updated else pr
                }
            )
        }
        persist()
    }
}
