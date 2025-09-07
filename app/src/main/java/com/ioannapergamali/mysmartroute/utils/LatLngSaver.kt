package com.ioannapergamali.mysmartroute.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.android.gms.maps.model.LatLng

/**
 * A simple [Saver] for [LatLng] objects used with Compose's [rememberSaveable].
 * Απλό [Saver] για αντικείμενα [LatLng] σε συνδυασμό με [rememberSaveable] του Compose.
 */
fun latLngSaver(): Saver<LatLng, List<Double>> = Saver(
    save = { listOf(it.latitude, it.longitude) },
    restore = { LatLng(it[0], it[1]) }
)

/**
 * A [Saver] that also supports `null` values for [LatLng].
 * Δημιουργεί ένα [Saver] που μπορεί να χειριστεί και `null` τιμές.
 *
 * Compose doesn't allow a nullable type for Saveable, so when [LatLng] is `null`
 * we store an empty [List].
 * Το Compose δεν επιτρέπει nullable τύπο για το Saveable, οπότε όταν το [LatLng] είναι `null`
 * αποθηκεύουμε απλώς μια κενή [List].
 */
fun nullableLatLngSaver(): Saver<LatLng?, List<Double>> = Saver(
    save = { value ->
        value?.let { listOf(it.latitude, it.longitude) } ?: emptyList()
    },
    restore = { list ->
        if (list.isEmpty()) null else LatLng(list[0], list[1])
    }
)
