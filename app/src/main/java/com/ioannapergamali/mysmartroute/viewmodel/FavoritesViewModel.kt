package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.insertFavoriteSafely
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FavoritesViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private fun userVehicles(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("favorites")
        .document("vehicles")
        .collection("items")

    private fun userId() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun preferredFlow(context: Context): Flow<Set<VehicleType>> {
        val dao = MySmartRouteDatabase.getInstance(context).favoriteDao()
        return dao.getPreferred(userId()).map { list ->
            list.mapNotNull { runCatching { VehicleType.valueOf(it.vehicleType) }.getOrNull() }.toSet()
        }
    }

    fun nonPreferredFlow(context: Context): Flow<Set<VehicleType>> {
        val dao = MySmartRouteDatabase.getInstance(context).favoriteDao()
        return dao.getNonPreferred(userId()).map { list ->
            list.mapNotNull { runCatching { VehicleType.valueOf(it.vehicleType) }.getOrNull() }.toSet()
        }
    }

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
