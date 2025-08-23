package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.Intent
import android.net.Uri


/**
 * Βοηθητικές συναρτήσεις για χειρισμό του συνδέσμου επαναφοράς κωδικού.
 */
object PasswordResetUtils {
    /**
     * Ανοίγει τον σύνδεσμο επαναφοράς κωδικού στον προεπιλεγμένο περιηγητή.
     *
     * @param context Το context από όπου γίνεται η κλήση.
     * @param url Ο σύνδεσμος επαναφοράς που θα ανοιχτεί.
     */
    fun openResetLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
