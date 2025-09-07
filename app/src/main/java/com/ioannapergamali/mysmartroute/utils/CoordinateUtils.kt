package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng

/**
 * Utility helpers for coordinate validation.
 * Βοηθητικές συναρτήσεις για την επικύρωση συντεταγμένων.
 */
object CoordinateUtils {
    /**
     * Returns true if [latLng] is not null and represents a valid geographic coordinate.
     * Επιστρέφει true αν το [latLng] δεν είναι null και αποτελεί έγκυρη γεωγραφική συντεταγμένη.
     */
    fun isValid(latLng: LatLng?): Boolean {
        return latLng != null &&
            latLng.latitude in -90.0..90.0 &&
            latLng.longitude in -180.0..180.0
    }
}

/**
 * Extension to quickly validate a [LatLng].
 * Επέκταση για γρήγορη επικύρωση ενός [LatLng].
 */
fun LatLng.isValid(): Boolean =
    latitude in -90.0..90.0 && longitude in -180.0..180.0
