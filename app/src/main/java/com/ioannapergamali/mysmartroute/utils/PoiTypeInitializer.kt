package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoiTypeEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * Δημιουργεί τα έγγραφα της συλλογής `poi_types` στο Firestore αν δεν υπάρχουν.
 */
fun populatePoiTypes(context: Context) {
    val firestore = FirebaseFirestore.getInstance()
    val types = Place.Type.values()
    runBlocking {
        MySmartRouteDatabase.getInstance(context).poiTypeDao()
            .insertAll(types.map { PoiTypeEntity(it.name, it.name) })
        for (type in types) {
            val data = mapOf("id" to type.name, "name" to type.name)
            try {
                firestore.collection("poi_types")
                    .document(type.name)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                Log.e("PoiTypeInitializer", "Αποτυχία εγγραφής τύπου ${type.name}", e)
            }
        }
    }
}
