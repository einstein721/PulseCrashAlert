package com.strathmore.predictivedispatch.network

import com.google.gson.annotations.SerializedName

data class CrashReportRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("gps_accuracy_m") val gpsAccuracyMeters: Float,
    @SerializedName("hour") val hour: Int,                    // 0..23
    @SerializedName("age") val age: Int,                      // 30 = Adult, 12 = Juvenile
    @SerializedName("cause_code") val causeCode: Int,         // e.g., 10, 26, 98
    @SerializedName("victim_code") val victimCode: Int,       // 0=Pedestrian, 1=Cyclist/Moto, 2=Passenger, 3=Driver
    @SerializedName("gender_code") val genderCode: Int,       // 0=Male, 1=Female, 2=Multiple/Unknown
    @SerializedName("county_code") val countyCode: Int,       // Auto-mapped from GPS (e.g., Nairobi = 47)
    @SerializedName("casualty_count") val casualtyCount: Int, // NO. (1..10)
    @SerializedName("image_base64") val imageBase64: String? = null
)

data class DispatchTriageResponse(
    @SerializedName("incident_id") val incidentId: String,
    @SerializedName("severity") val severity: Int,            // 1 = Priority Trauma, 0 = Moderate Incident
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("dispatch_status") val dispatchStatus: String
)
