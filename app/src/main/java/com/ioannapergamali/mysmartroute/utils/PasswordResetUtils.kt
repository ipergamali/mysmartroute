package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R


/**
 * Βοηθητικές συναρτήσεις για χειρισμό του συνδέσμου επαναφοράς κωδικού.
 */
object PasswordResetUtils {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Στέλνει email επαναφοράς κωδικού μέσω Firebase Authentication.
     */
    fun sendPasswordResetEmail(email: String, context: Context) {
        if (email.isBlank()) {
            Toast.makeText(context, context.getString(R.string.email), Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    context.getString(R.string.reset_email_sent),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Αποτυχία",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
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
