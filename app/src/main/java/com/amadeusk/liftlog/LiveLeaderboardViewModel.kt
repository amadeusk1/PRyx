package com.amadeusk.liftlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amadeusk.liftlog.data.AcceptedSubmission
import com.amadeusk.liftlog.data.PrSubmitApi
import com.amadeusk.liftlog.data.PrSubmitRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ioDispatcher = kotlinx.coroutines.Dispatchers.IO

/** UI state for the Live Leaderboard (submit + poll status + accepted list). */
data class LiveLeaderboardUiState(
    val submissionId: String? = null,
    val status: String? = null,  // "pending" | "accepted" | "rejected"
    val reviewedAt: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val submitSuccess: Boolean = false,
    val acceptedSubmissions: List<com.amadeusk.liftlog.data.AcceptedSubmission> = emptyList(),
    val isLoadingList: Boolean = false
)

class LiveLeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LiveLeaderboardUiState())
    val uiState: StateFlow<LiveLeaderboardUiState> = _uiState

    private var pollJob: Job? = null

    init {
        fetchAcceptedList()
    }

    /** Load accepted submissions from the server. */
    fun fetchAcceptedList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true) }
            try {
                val response = withContext(ioDispatcher) { PrSubmitApi.service.getAcceptedList() }
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok) {
                    _uiState.update {
                        it.copy(
                            acceptedSubmissions = body.submissions ?: emptyList(),
                            isLoadingList = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingList = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingList = false) }
            }
        }
    }

    /** Submit a PR. exercise must be "bench", "deadlift", or "squat". Optional: notes, image, video. */
    fun submit(
        name: String,
        exercise: String,
        weightDisplay: Double,
        useKg: Boolean,
        notes: String,
        reps: Int,
        imageBase64: String? = null,
        videoBase64: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val weightStr = if (useKg) "%.1f kg".format(weightDisplay) else "%.1f lb".format(weightDisplay)
                val req = PrSubmitRequest(
                    name = name.trim(),
                    weight = weightStr,
                    reps = reps.toString(),
                    exercise = exercise,
                    notes = notes.trim().takeIf { it.isNotBlank() },
                    image = imageBase64?.takeIf { it.isNotBlank() },
                    video = videoBase64?.takeIf { it.isNotBlank() }
                )
                val response = withContext(ioDispatcher) { PrSubmitApi.service.submitPr(req) }
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok && !body.submissionId.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submissionId = body.submissionId,
                            status = "pending",
                            submitSuccess = true,
                            errorMessage = null
                        )
                    }
                    startPolling(body.submissionId)
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = body?.error ?: "Submit failed (${response.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Network error"
                    )
                }
            }
        }
    }

    private fun startPolling(submissionId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                try {
                    val response = withContext(ioDispatcher) {
                        PrSubmitApi.service.getStatus(submissionId)
                    }
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.ok && body.status != null) {
                        _uiState.update {
                            it.copy(
                                status = body.status,
                                reviewedAt = body.reviewedAt
                            )
                        }
                        when (body.status) {
                            "accepted" -> {
                                fetchAcceptedList()
                                return@launch
                            }
                            "rejected" -> return@launch
                            else -> { /* keep polling */ }
                        }
                    }
                } catch (_: Exception) { /* ignore and retry */ }
            }
        }
    }

    /** Clear submission state so user can submit again. */
    fun clearSubmission() {
        pollJob?.cancel()
        pollJob = null
        _uiState.update {
            it.copy(
                submissionId = null,
                status = null,
                reviewedAt = null,
                submitSuccess = false,
                errorMessage = null
            )
        }
    }
}
