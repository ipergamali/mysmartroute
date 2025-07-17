package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**

 * να καλέσει την επίσημη συνάρτηση της βιβλιοθήκης Compose αν υπάρχει.
 * Αν δεν υπάρχει, η κλήση απλώς αγνοείται.
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
suspend fun BringIntoViewRequester.safeBringIntoView() {
    // Αναζητούμε την επίσημη μέθοδο μέσω reflection.
    val method = try {
        this::class.java.getMethod("bringIntoView", kotlin.coroutines.Continuation::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }

    if (method != null) {
        // Κλήση της suspend συνάρτησης χρησιμοποιώντας continuation
        return suspendCoroutine { cont ->
            try {
                method.invoke(this, cont)
            } catch (t: Throwable) {
                cont.resume(Unit)
            }
        }
    }
    // Αν δεν βρεθεί μέθοδος, δεν κάνουμε τίποτα
}

