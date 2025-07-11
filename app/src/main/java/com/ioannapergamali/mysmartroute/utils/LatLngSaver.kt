package com.ioannapergamali.mysmartroute.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.android.gms.maps.model.LatLng

/**
 * A simple [Saver] for [LatLng] objects used with Compose's [rememberSaveable].
 */
fun latLngSaver(): Saver<LatLng, List<Double>> = Saver(
    save = { listOf(it.latitude, it.longitude) },
    restore = { LatLng(it[0], it[1]) }
)

/**
 * A [Saver] that also supports `null` values for [LatLng].
 */
fun nullableLatLngSaver(): Saver<LatLng?, List<Double>?> = Saver(
    save = { it?.let { listOf(it.latitude, it.longitude) } },
    restore = { it?.let { LatLng(it[0], it[1]) } }
)
