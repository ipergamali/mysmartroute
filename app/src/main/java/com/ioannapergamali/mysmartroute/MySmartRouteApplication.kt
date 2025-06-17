package com.ioannapergamali.mysmartroute

import android.app.Application
import com.google.firebase.FirebaseApp


class MySmartRouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Η υπηρεσία Firebase App Check απενεργοποιήθηκε προσωρινά
    }
}
