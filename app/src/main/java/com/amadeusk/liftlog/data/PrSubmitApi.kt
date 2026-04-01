package com.amadeusk.liftlog.data

import com.google.gson.GsonBuilder
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

private const val BASE_URL = "https://www.amadeusk.dev/pryx/"

interface PrSubmitApiService {
    @POST("pr_submit.php")
    suspend fun submitPr(@Body body: PrSubmitRequest): Response<PrSubmitResponse>

    @GET("pr_status.php")
    suspend fun getStatus(@Query("submission_id") submissionId: String): Response<PrStatusResponse>

    /** Accepted submissions (leaderboard). */
    @GET("pr_accepted.php")
    suspend fun getAcceptedList(): Response<PrListResponse>
}

object PrSubmitApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()

    val service: PrSubmitApiService = retrofit.create(PrSubmitApiService::class.java)
}
