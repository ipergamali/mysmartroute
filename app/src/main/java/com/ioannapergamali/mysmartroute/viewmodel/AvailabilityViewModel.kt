package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.AvailabilityEntity
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.UUID

/** ViewModel για αποθήκευση διαθεσιμότητας οδηγού. */
class AvailabilityViewModel : ViewModel() {
    fun declareAvailability(
        context: Context,
        date: Long,
        fromTime: Int,
        toTime: Int
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).availabilityDao()
            val id = UUID.randomUUID().toString()
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val entity = AvailabilityEntity(id, userId, date, fromTime, toTime)
            dao.insert(entity)
            try {
                FirebaseFirestore.getInstance()
                    .collection("availabilities")
                    .document(id)
                    .set(entity.toFirestoreMap())
                    .await()
            } catch (_: Exception) {
                // ignore failures; will sync later
            }
        }
    }
}
