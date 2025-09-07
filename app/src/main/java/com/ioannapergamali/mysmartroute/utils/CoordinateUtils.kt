package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng

/**
 * Βοηθητικές συναρτήσεις για την επικύρωση συντεταγμένων.
 * Utility helpers for coordinate validation.
 */
object CoordinateUtils {
    /**
     * Επιστρέφει true αν το [latLng] δεν είναι null και αποτελεί έγκυρη γεωγραφική συντεταγμένη.
     * Returns true if [latLng] is not null and represents a valid geographic coordinate.
     */
    fun isValid(latLng: LatLng?): Boolean {
        return latLng != null &&
            latLng.latitude in -90.0..90.0 &&
            latLng.longitude in -180.0..180.0
    }
}

/**
 * Επέκταση για γρήγορη επικύρωση ενός [LatLng].
 * Extension to quickly validate a [LatLng].
 */
fun LatLng.isValid(): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0
