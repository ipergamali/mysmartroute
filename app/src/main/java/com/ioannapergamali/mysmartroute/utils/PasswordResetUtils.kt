package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Βοηθητικές συναρτήσεις για χειρισμό του συνδέσμου επαναφοράς κωδικού.
 */
object PasswordResetUtils {
    private val client = OkHttpClient()

    /**
     * Ελέγχει αν ο σύνδεσμος είναι ακόμη έγκυρος κι αν ναι τον ανοίγει.
     * Αν έχει λήξει ή υπάρξει σφάλμα, εμφανίζεται μήνυμα στον χρήστη.
     */
    fun openResetLink(context: Context, resetUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val isValid = isLinkValid(resetUrl)
            withContext(Dispatchers.Main) {
                if (isValid) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resetUrl))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(
                        context,
                        "Ο σύνδεσμος έχει λήξει ή έχει ήδη χρησιμοποιηθεί",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun isLinkValid(resetUrl: String): Boolean {
        val request = Request.Builder()
            .url(resetUrl)
            .head()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            false
        }
    }
}
