package com.strathmore.predictivedispatch.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class GpsLocationTracker(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(): Location? {
        return try {
            val cancellationToken = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
            location ?: fusedClient.lastLocation.await()
        } catch (_: Exception) {
            try {
                fusedClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationResult: (Location) -> Unit): LocationCallback {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(1f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onLocationResult)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        return callback
    }

    fun stopLocationUpdates(callback: LocationCallback) {
        fusedClient.removeLocationUpdates(callback)
    }
}
