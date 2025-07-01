package com.ioannapergamali.mysmartroute.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.model.enumerations.PoIType

/**
 * Δημιουργεί τα έγγραφα της συλλογής `poi_types` στο Firestore αν δεν υπάρχουν.
 */
fun populatePoiTypes() {
    val firestore = FirebaseFirestore.getInstance()
    for (type in PoIType.values()) {
        val data = mapOf("id" to type.name, "name" to type.name)
        firestore.collection("poi_types")
            .document(type.name)
            .set(data)
    }
}
