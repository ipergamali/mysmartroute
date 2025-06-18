package com.ioannapergamali.mysmartroute

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.ioannapergamali.mysmartroute.BuildConfig


class MySmartRouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Η υπηρεσία Firebase App Check απενεργοποιήθηκε προσωρινά
        val apiKey = BuildConfig.MAPS_API_KEY
        Log.d("MySmartRoute", "API key loaded? ${apiKey.isNotEmpty()}")
    }
}
