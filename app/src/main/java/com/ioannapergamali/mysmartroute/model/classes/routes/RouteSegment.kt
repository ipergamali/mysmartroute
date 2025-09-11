package com.ioannapergamali.mysmartroute.model.classes.routes

import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.PoI

/**
 * Αντιπροσώπευση τμημάτων διαδρομής.
 * Κάθε τμήμα μπορεί να είναι πεζό ή λεωφορείο.
 */
sealed class RouteSegment {
    data class Walk(
        val start: PoI,
        val end: PoI
    ) : RouteSegment()

    data class Bus(
        val start: PoI,
        val end: PoI,
        val driver: DriverInfo
    ) : RouteSegment()
}

/**
 * Βασικές πληροφορίες οδηγού για τα τμήματα λεωφορείου.
 * Περιλαμβάνεται το ID του οδηγού και τα στοιχεία του λεωφορείου.
 */
data class DriverInfo(
    val driverId: String,
    val driverName: String,
    val busId: String,
    val busType: VehicleType
) {
    init {
        require(
            busType == VehicleType.BIGBUS ||
                busType == VehicleType.SMALLBUS
        ) { "Το όχημα πρέπει να είναι λεωφορείο" }
    }
}

/**
 * Διαδρομή αποτελούμενη από πολλαπλά τμήματα.
 */
data class ComplexRoute(
    val segments: List<RouteSegment>
)

/**
 * Έλεγχος αν ο οδηγός είναι διαθέσιμος για κάποιο τμήμα λεωφορείου.
 */
fun isDriverAvailable(driverId: String, route: ComplexRoute): Boolean {
    return route.segments
        .filterIsInstance<RouteSegment.Bus>()
        .any { it.driver.driverId == driverId }
}

/**
 * Δήλωση διαθεσιμότητας οδηγού για κάθε τμήμα λεωφορείου.
 */
data class DriverAvailability(
    val segment: RouteSegment.Bus,
    val isAvailable: Boolean
)

fun declareDriverAvailability(
    availableDriverIds: Set<String>,
    route: ComplexRoute
): List<DriverAvailability> {
    return route.segments
        .filterIsInstance<RouteSegment.Bus>()
        .map { segment ->
            DriverAvailability(
                segment,
                availableDriverIds.contains(segment.driver.driverId)
            )
        }
}
