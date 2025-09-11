package com.ioannapergamali.mysmartroute.model.classes.routes

/**
 * Αντιπροσώπευση τμημάτων διαδρομής.
 * Κάθε τμήμα μπορεί να είναι πεζό ή λεωφορείο.
 */
sealed class RouteSegment {
    data class Walk(
        val start: String,
        val end: String
    ) : RouteSegment()

    data class Bus(
        val start: String,
        val end: String,
        val driver: DriverInfo
    ) : RouteSegment()
}

/**
 * Βασικές πληροφορίες οδηγού για τα τμήματα λεωφορείου.
 */
data class DriverInfo(
    val id: String,
    val name: String
)

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
        .any { it.driver.id == driverId }
}
