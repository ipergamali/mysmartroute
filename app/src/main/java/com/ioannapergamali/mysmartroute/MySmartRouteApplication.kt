package com.ioannapergamali.mysmartroute

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.ioannapergamali.mysmartroute.BuildConfig
import com.ioannapergamali.mysmartroute.utils.ShortcutUtils
import com.ioannapergamali.mysmartroute.utils.populatePoiTypes
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
import kotlinx.coroutines.runBlocking


class MySmartRouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val lang = runBlocking { LanguagePreferenceManager.getLanguage(this@MySmartRouteApplication) }
        LocaleUtils.updateLocale(this, lang)
        FirebaseApp.initializeApp(this)
        AuthenticationViewModel().ensureMenusInitialized(this)
        populatePoiTypes(this)
        // Η υπηρεσία Firebase App Check απενεργοποιήθηκε προσωρινά
        //val apiKey = BuildConfig.MAPS_API_KEY
//        Log.d("MySmartRoute Maps API key ", "Maps API key loaded: ${apiKey.isNotBlank()}")
//        if (apiKey.isBlank()) {
//            Log.w("MySmartRoute Maps API key " , "MAPS_API_KEY is blank. Ελέγξτε το local.properties")
//        }
        ShortcutUtils.addMainShortcut(this)
    }
}
