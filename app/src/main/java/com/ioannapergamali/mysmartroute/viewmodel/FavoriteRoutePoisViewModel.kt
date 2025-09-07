package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase

import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.UserPoiEntity

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel για την αποθήκευση των επιλεγμένων POI μιας διαδρομής
 * τόσο τοπικά όσο και στο Firebase.
 */
class FavoriteRoutePoisViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    /**
     * Αποθηκεύει τα επιλεγμένα σημεία μιας διαδρομής.
     * Τα δεδομένα αποθηκεύονται στο `users/{uid}/favorites/data/pois/{routeId_poiId}`
     * με πεδία `routeRef` και `poiRef`.
     */
    fun saveFavorites(
        context: Context,
        routeId: String,
        poiIds: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val uid = userId()
        if (uid.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val routeRef = firestore.collection("routes").document(routeId)
            val col = userDoc(uid).collection("favorites").document("data").collection("pois")

            val batch = firestore.batch()
            val existing = col.whereEqualTo("routeRef", routeRef).get().await()
            existing.documents.forEach { batch.delete(it.reference) }
            poiIds.forEach { poiId ->
                val poiRef = firestore.collection("pois").document(poiId)
                val docId = "${routeId}_$poiId"
                batch.set(col.document(docId), mapOf("routeRef" to routeRef, "poiRef" to poiRef))
            }
            val remoteResult = runCatching { batch.commit().await() }.isSuccess

            val db = MySmartRouteDatabase.getInstance(context)

            val userPoiDao = db.userPoiDao()
            val userDao = db.userDao()
            val poiDao = db.poIDao()

            // Εισαγωγή χρήστη αν δεν υπάρχει
            userDao.insert(UserEntity(id = uid))

            // Εισαγωγή κάθε POI και σύνδεσή του με τον χρήστη
            poiIds.forEach { poiId ->
                poiDao.insert(PoIEntity(id = poiId))

            poiIds.forEach { poiId ->

                val entity = UserPoiEntity(
                    id = "$uid-$poiId",
                    userId = uid,
                    poiId = poiId
                )

                userPoiDao.insert(entity)

            }
            onComplete(remoteResult)
        }
    }
}

