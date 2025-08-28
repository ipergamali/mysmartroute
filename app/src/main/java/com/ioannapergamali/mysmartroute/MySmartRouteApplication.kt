package com.ioannapergamali.mysmartroute

import android.app.Application
import com.google.firebase.FirebaseApp
import com.ioannapergamali.mysmartroute.BuildConfig
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
import com.ioannapergamali.mysmartroute.utils.ShortcutUtils
import com.ioannapergamali.mysmartroute.utils.populatePoiTypes
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import kotlinx.coroutines.runBlocking
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.config.mailSender


class MySmartRouteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. ACRA για αναφορές σφαλμάτων
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            mailSender {
                mailTo = "ioannapergamali@gmail.com"
                reportAsFile = false
            }
        }

        // 2. Ρύθμιση γλώσσας
        val lang = runBlocking {
            LanguagePreferenceManager.getLanguage(this@MySmartRouteApplication)
        }
        LocaleUtils.updateLocale(this, lang)

        // 3. Firebase
        FirebaseApp.initializeApp(this)

        // 4. Μενού χρήστη
        AuthenticationViewModel().ensureMenusInitialized(this)

        // 5. Δομές δεδομένων και κύριο shortcut
        populatePoiTypes(this)
        ShortcutUtils.addMainShortcut(this)

        // 6. Συγχρονισμός βάσεων δεδομένων
        runBlocking {
            try {
                DatabaseViewModel().syncDatabasesSuspend(this@MySmartRouteApplication)
            } catch (_: Exception) {
                // Προαιρετική καταγραφή σφάλματος
            }
        }
    }
}
