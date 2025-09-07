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
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra


/**
 * Κεντρική κλάση `Application` που αρχικοποιεί υπηρεσίες και ρυθμίσεις κατά την εκκίνηση.
 * Main `Application` class that initializes services and settings on startup.
 */
class MySmartRouteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. ACRA για αναφορές σφαλμάτων
        // 1. ACRA crash reporting
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            mailSender {
                mailTo = "ioannapergamali@gmail.com"
                reportAsFile = false
            }
        }

        // 2. Ρύθμιση γλώσσας
        // 2. Language setup
        val lang = runBlocking {
            LanguagePreferenceManager.getLanguage(this@MySmartRouteApplication)
        }
        LocaleUtils.updateLocale(this, lang)

        // 3. Firebase
        // 3. Firebase initialization
        FirebaseApp.initializeApp(this)

        // 4. Μενού χρήστη
        // 4. User menus
        AuthenticationViewModel().ensureMenusInitialized(this)

        // 5. Δομές δεδομένων και κύριο shortcut
        // 5. Data structures and main shortcut
        populatePoiTypes(this)
        ShortcutUtils.addMainShortcut(this)

        // 6. Συγχρονισμός βάσεων δεδομένων
        // 6. Database synchronization
        runBlocking {
            try {
                DatabaseViewModel().syncDatabasesSuspend(this@MySmartRouteApplication)
            } catch (_: Exception) {
                // Αγνόηση τυχόν σφαλμάτων ώστε να μην εμποδιστεί η εκκίνηση
                // Ignore any sync errors so startup isn't blocked
            }
        }
    }
}
