package com.amadeusk.liftlog.data

import com.google.gson.annotations.SerializedName

/** Request body for POST pr_submit.php. Required: name, weight, reps, exercise (bench|deadlift|squat). Optional: notes, image, video. */
data class PrSubmitRequest(
    @SerializedName("name") val name: String,
    @SerializedName("weight") val weight: String,     // e.g. "100 kg" or "225"
    @SerializedName("reps") val reps: String,
    @SerializedName("exercise") val exercise: String, // exactly: "bench", "deadlift", or "squat"
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("video") val video: String? = null
)

/** Response from pr_submit.php. 201: ok + submission_id. 400: ok=false + error. */
data class PrSubmitResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("submission_id") val submissionId: String? = null,
    @SerializedName("error") val error: String? = null
)

/** Response from GET pr_status.php?submission_id=xxx */
data class PrStatusResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("status") val status: String?,  // "pending" | "accepted" | "rejected"
    @SerializedName("reviewed_at") val reviewedAt: String? = null
)

/** One accepted submission for display. exercise is "bench"|"squat"|"deadlift" when provided by API. */
data class AcceptedSubmission(
    @SerializedName("name") val name: String,
    @SerializedName("weight") val weight: String,
    @SerializedName("exercise") val exercise: String? = null
)

/** Response from GET pr_accepted.php */
data class PrListResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("submissions") val submissions: List<AcceptedSubmission>?
)
