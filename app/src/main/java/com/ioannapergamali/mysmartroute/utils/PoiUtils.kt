package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.data.local.PoIEntity

/**
 * Επιστρέφει λίστα από POI μαζί με μετατοπισμένες θέσεις ώστε
 * markers με ίδιες συντεταγμένες να μην αλληλοκαλύπτονται.
 */
fun offsetPois(
    pois: List<PoIEntity>,
    step: Double = 0.00005
): List<Pair<PoIEntity, LatLng>> {
    val counters = mutableMapOf<Pair<Double, Double>, Int>()
    return pois.map { poi ->
        val key = poi.lat to poi.lng
        val count = counters.getOrDefault(key, 0)
        counters[key] = count + 1
        val position = LatLng(poi.lat + count * step, poi.lng)
        poi to position
    }
}

