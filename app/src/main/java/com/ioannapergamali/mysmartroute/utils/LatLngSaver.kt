package com.ioannapergamali.mysmartroute.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.android.gms.maps.model.LatLng

/**
 * Απλό [Saver] για αντικείμενα [LatLng] σε συνδυασμό με [rememberSaveable] του Compose.
 * A simple [Saver] for [LatLng] objects used with Compose's [rememberSaveable].

 */
fun latLngSaver(): Saver<LatLng, List<Double>> = Saver(
    save = { listOf(it.latitude, it.longitude) },
    restore = { LatLng(it[0], it[1]) }
)

/**


 * Δημιουργεί ένα [Saver] που μπορεί να χειριστεί και `null` τιμές.
 * A [Saver] that also supports `null` values for [LatLng].
 *

 * Το Compose δεν επιτρέπει nullable τύπο για το Saveable, οπότε όταν το [LatLng] είναι `null`
 * αποθηκεύουμε απλώς μια κενή [List].
 * Compose doesn't allow a nullable type for Saveable, so when [LatLng] is `null`
 * we store an empty [List].

 */
fun nullableLatLngSaver(): Saver<LatLng?, List<Double>> = Saver(
    save = { value ->
        value?.let { listOf(it.latitude, it.longitude) } ?: emptyList()
    },
    restore = { list ->
        if (list.isEmpty()) null else LatLng(list[0], list[1])
    }
)
