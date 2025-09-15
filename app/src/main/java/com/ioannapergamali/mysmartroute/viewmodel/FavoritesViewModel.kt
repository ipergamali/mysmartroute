package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.insertFavoriteSafely
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ViewModel για τη διαχείριση προτιμήσεων οχημάτων του χρήστη.
 * ViewModel managing the user's vehicle preferences.
 */
class FavoritesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userVehicles(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("favorites")
        .document("vehicles")
        .collection("items")

    private fun userId() = SessionManager.currentUserId() ?: ""

    /**
     * Φορτώνει τα αγαπημένα οχήματα από το Firestore και τα συγχρονίζει με την τοπική βάση.
     * Loads favorite vehicles from Firestore and syncs them with the local database.
     */
    fun loadFavorites(context: Context) {
        viewModelScope.launch {
            val uid = userId()
            if (uid.isBlank()) return@launch
            val db = MySmartRouteDatabase.getInstance(context)
            val dao = db.favoriteDao()
            val snap = runCatching { userVehicles(uid).get().await() }.getOrNull()
            if (snap != null) {
                dao.deleteAllForUser(uid)
                snap.documents.mapNotNull { doc ->
                    val type = doc.getString("vehicleType")
                    val preferred = doc.getBoolean("preferred") ?: true
                    if (type != null) {
                        com.ioannapergamali.mysmartroute.data.local.FavoriteEntity(
                            doc.id,
                            uid,
                            type,
                            preferred
                        )
                    } else null
                }.forEach { fav ->
                    insertFavoriteSafely(dao, db.userDao(), fav)
                }
            }
        }
    }

    /**
     * Ροή με τα οχήματα που προτιμά ο χρήστης.
     * Flow emitting vehicles preferred by the user.
     */
    fun preferredFlow(context: Context): Flow<Set<VehicleType>> {
        val dao = MySmartRouteDatabase.getInstance(context).favoriteDao()
        return dao.getPreferred(userId()).map { list ->
            list.mapNotNull { runCatching { VehicleType.valueOf(it.vehicleType) }.getOrNull() }.toSet()
        }
    }

    /**
     * Ροή με τα οχήματα που ο χρήστης δεν προτιμά.
     * Flow emitting vehicles the user dislikes.
     */
    fun nonPreferredFlow(context: Context): Flow<Set<VehicleType>> {
        val dao = MySmartRouteDatabase.getInstance(context).favoriteDao()
        return dao.getNonPreferred(userId()).map { list ->
            list.mapNotNull { runCatching { VehicleType.valueOf(it.vehicleType) }.getOrNull() }.toSet()
        }
    }

    /**
     * Προσθέτει όχημα στις προτιμήσεις του χρήστη.
     * Adds a vehicle to the user's preferred list.
     */
    fun addPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch {
            val uid = userId()
            if (uid.isBlank()) return@launch
            val db = MySmartRouteDatabase.getInstance(context)
            val id = UUID.randomUUID().toString()
            val fav = com.ioannapergamali.mysmartroute.data.local.FavoriteEntity(id, uid, type.name, true)
            insertFavoriteSafely(db.favoriteDao(), db.userDao(), fav)
            try {
                userVehicles(uid).document(id).set(fav.toFirestoreMap()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Προσθέτει όχημα στη λίστα μη προτιμώμενων.
     * Adds a vehicle to the non-preferred list.
     */
    fun addNonPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch {
            val uid = userId()
            if (uid.isBlank()) return@launch
            val db = MySmartRouteDatabase.getInstance(context)
            val id = UUID.randomUUID().toString()
            val fav = com.ioannapergamali.mysmartroute.data.local.FavoriteEntity(id, uid, type.name, false)
            insertFavoriteSafely(db.favoriteDao(), db.userDao(), fav)
            try {
                userVehicles(uid).document(id).set(fav.toFirestoreMap()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Αφαιρεί όχημα από τις προτιμήσεις του χρήστη.
     * Removes a vehicle from the user's preferred list.
     */
    fun removePreferred(context: Context, type: VehicleType) {
        viewModelScope.launch {
            val uid = userId()
            if (uid.isBlank()) return@launch
            val db = MySmartRouteDatabase.getInstance(context)
            db.favoriteDao().delete(uid, type.name)
            try {
                userVehicles(uid)
                    .whereEqualTo("vehicleType", type.name)
                    .whereEqualTo("preferred", true)
                    .get()
                    .await()
                    .documents.forEach { it.reference.delete().await() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Αφαιρεί όχημα από τη λίστα μη προτιμώμενων.
     * Removes a vehicle from the non-preferred list.
     */
    fun removeNonPreferred(context: Context, type: VehicleType) {
        viewModelScope.launch {
            val uid = userId()
            if (uid.isBlank()) return@launch
            val db = MySmartRouteDatabase.getInstance(context)
            db.favoriteDao().delete(uid, type.name)
            try {
                userVehicles(uid)
                    .whereEqualTo("vehicleType", type.name)
                    .whereEqualTo("preferred", false)
                    .get()
                    .await()
                    .documents.forEach { it.reference.delete().await() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
