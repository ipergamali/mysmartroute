package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Βοηθητικές συναρτήσεις για χειρισμό του συνδέσμου επαναφοράς κωδικού.
 */
object PasswordResetUtils {
    /**
     * Ανοίγει τον σύνδεσμο επαναφοράς κωδικού σε browser ή συμβατή εφαρμογή.
     */
    fun openResetLink(context: Context, resetUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resetUrl))
        context.startActivity(intent)
    }
}
