package com.ioannapergamali.mysmartroute.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.android.gms.maps.model.LatLng

/**
 * A simple [Saver] for [LatLng] objects used with Compose's [rememberSaveable].
 */
fun latLngSaver(): Saver<LatLng, List<Double>> = listSaver(
    save = { listOf(it.latitude, it.longitude) },
    restore = { LatLng(it[0], it[1]) }
)
