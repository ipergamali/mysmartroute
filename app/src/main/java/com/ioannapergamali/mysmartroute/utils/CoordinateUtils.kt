package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng

/** Utility helpers for coordinate validation */
object CoordinateUtils {
    /**
     * Returns true if [latLng] is not null and represents a valid geographic coordinate.
     */
    fun isValid(latLng: LatLng?): Boolean {
        return latLng != null &&
            latLng.latitude in -90.0..90.0 &&
            latLng.longitude in -180.0..180.0
    }
}

/** Extension to quickly validate a [LatLng] */
fun LatLng.isValid(): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0
