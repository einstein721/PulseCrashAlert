package com.strathmore.predictivedispatch.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface PredictiveDispatchApi {
    @POST("api/v1/report-crash")
    suspend fun submitCrashReport(
        @Body payload: CrashReportRequest
    ): DispatchTriageResponse

    companion object {
        // Use 10.0.2.2 for Android Emulator -> Localhost Python API, or your hosted HTTPS URL
        private const val BASE_URL = "http://10.0.2.2:8000/"

        fun create(baseUrl: String = BASE_URL): PredictiveDispatchApi {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PredictiveDispatchApi::class.java)
        }
    }
}
