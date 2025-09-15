package com.ioannapergamali.mysmartroute.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Απλός διαχειριστής που κρατά τοπικά το αναγνωριστικό χρήστη όταν δεν υπάρχει σύνδεση.
 * Keeps track of the authenticated user id when FirebaseAuth is unavailable offline.
 */
object SessionManager {
    private val offlineUserId = MutableStateFlow<String?>(null)

    val userIdFlow: StateFlow<String?> = offlineUserId

    fun currentUserId(auth: FirebaseAuth): String? = auth.currentUser?.uid ?: offlineUserId.value

    fun currentUserId(): String? = currentUserId(FirebaseAuth.getInstance())

    fun setOfflineUser(userId: String?) {
        offlineUserId.value = userId
    }

    fun clear() {
        offlineUserId.value = null
    }
}
