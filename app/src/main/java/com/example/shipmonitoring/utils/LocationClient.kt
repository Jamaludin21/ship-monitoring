package com.example.shipmonitoring.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationClient(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Kita akan tangani permission di UI
    fun getLocationUpdates(intervalMs: Long): Flow<android.location.Location> {
        return callbackFlow {
            // Konfigurasi pengambilan GPS (Akurat, update setiap X detik)
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs)
                .build()

            // Listener setiap kali satelit GPS menemukan koordinat baru
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.lastOrNull()?.let { location ->
                        trySend(location) // Kirim data ke ViewModel
                    }
                }
            }

            // Mulai melacak
            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            // Saat Coroutine dibatalkan (misal Nahkoda mematikan fitur), matikan GPS
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}