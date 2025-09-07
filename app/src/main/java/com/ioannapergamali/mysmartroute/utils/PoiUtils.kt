package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlin.math.abs

/**
 * Επιστρέφει λίστα από POI μαζί με μετατοπισμένες θέσεις ώστε
 * markers με ίδιες συντεταγμένες να μην αλληλοκαλύπτονται.
 * Returns POIs with slightly shifted positions so markers at the same
 * coordinates do not overlap.
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

/**
 * Βρίσκει όλα τα POI που έχουν ακριβώς τις ίδιες συντεταγμένες με το
 * δοθέν σημείο [point]. Ένα μικρό [tolerance] χρησιμοποιείται για την
 * σύγκριση των δεκαδικών.
 * Finds all POIs sharing the same coordinates as [point]; a small [tolerance]
 * is used for decimal comparison.
 */
fun poisAtLocation(
    pois: List<PoIEntity>,
    point: LatLng,
    tolerance: Double = 1e-5
): List<PoIEntity> =
    pois.filter {
        abs(it.lat - point.latitude) < tolerance &&
        abs(it.lng - point.longitude) < tolerance
    }

/**
 * Επιστρέφει ομάδες από POI που μοιράζονται τις ίδιες
 * συντεταγμένες αλλά έχουν διαφορετικό όνομα. Χρησιμοποιείται
 * ώστε ο διαχειριστής να μπορεί να τα συγχωνεύσει σε ένα.
 * Returns groups of POIs sharing the same coordinates but different names,
 * allowing an administrator to merge them.
 */
fun duplicatePois(
    pois: List<PoIEntity>
): List<List<PoIEntity>> =
    pois.groupBy { it.lat to it.lng }
        .values
        .filter { group ->
            group.size > 1 && group.map { it.name }.toSet().size > 1
        }

