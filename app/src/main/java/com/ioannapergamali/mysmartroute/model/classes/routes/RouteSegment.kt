package com.ioannapergamali.mysmartroute.model.classes.routes

import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.model.interfaces.User
import com.ioannapergamali.mysmartroute.model.interfaces.Vehicle

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
 * Περιλαμβάνεται ο χρήστης με ρόλο οδηγού και το λεωφορείο που έχει δηλώσει.
 */
data class DriverInfo(
    val user: User,
    val bus: Vehicle
) {
    init {
        require(user.getRole() == UserRole.DRIVER) { "Ο χρήστης πρέπει να έχει ρόλο οδηγού" }
        require(
            bus.getType() == VehicleType.BIGBUS ||
                bus.getType() == VehicleType.SMALLBUS
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
        .any { it.driver.user.id == driverId }
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
                availableDriverIds.contains(segment.driver.user.id)
            )
        }
}
