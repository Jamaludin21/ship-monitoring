package com.example.shipmonitoring

import android.app.Application
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class ShipMonitoringApplication : Application(), OnMapsSdkInitializedCallback {

    override fun onCreate() {
        super.onCreate()
        // Menginisialisasi Maps SDK dengan renderer LATEST untuk menghindari bug disk cache (Database lock unavailable)
        // yang sering terjadi pada renderer LEGACY di Android versi terbaru.
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d("MapsInit", "The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> Log.d("MapsInit", "The legacy version of the renderer is used.")
            else -> Log.d("MapsInit", "A custom renderer is used.")
        }
    }
}
