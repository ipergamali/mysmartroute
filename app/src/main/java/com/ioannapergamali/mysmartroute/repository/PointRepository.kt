package com.ioannapergamali.mysmartroute.repository

/**
 * Απλό repository για διαχείριση σημείων και διαδρομών.
 * Simple repository for managing points and routes.
 */
data class Route(
    val id: String,
    val pointIds: MutableList<String>
)

/**
 * Αναπαριστά ένα σημείο με όνομα και λεπτομέρειες.
 * Represents a point with a name and details.
 */
data class Point(
    val id: String,
    var name: String,
    var details: String
)

/**
 * Repository σε μνήμη για απλές λειτουργίες CRUD πάνω σε σημεία και διαδρομές.
 * In-memory repository providing simple CRUD operations on points and routes.
 */
class PointRepository {
    private val points = mutableMapOf<String, Point>()
    private val routes = mutableMapOf<String, Route>()

    /**
     * Επιστροφή όλων των ονομάτων σημείων.
     * Returns all point names.
     */
    fun getAllPointNames(): List<String> = points.values.map { it.name }

    /**
     * Επιστροφή όλων των σημείων.
     * Returns all points.
     */
    fun getAllPoints(): List<Point> = points.values.toList()

    /**
     * Ενημέρωση στοιχείων ενός σημείου.
     * Updates a point's information.
     */
    fun updatePoint(pointId: String, newName: String, newDetails: String) {
        points[pointId]?.apply {
            name = newName
            details = newDetails
        }
    }

    /**
     * Ομαδοποίηση δύο σημείων.
     * Merges two points into one.
     */
    fun mergePoints(pointAId: String, pointBId: String) {
        val pointA = points[pointAId] ?: return
        val pointB = points.remove(pointBId) ?: return

        if (pointB.name.isNotEmpty()) {
            pointA.name = "${'$'}{pointA.name} / ${'$'}{pointB.name}"
        }
        if (pointB.details.isNotEmpty()) {
            pointA.details = listOf(pointA.details, pointB.details)
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }

        routes.values.forEach { route ->
            route.pointIds.replaceAll { id -> if (id == pointBId) pointAId else id }
        }
    }

    /**
     * Διαγραφή σημείου και ενημέρωση διαδρομών.
     * Deletes a point and updates routes.
     */
    fun deletePoint(pointId: String) {
        if (points.remove(pointId) != null) {
            routes.values.forEach { route ->
                route.pointIds.removeIf { it == pointId }
            }
        }
    }

    /**
     * Προσθήκη νέου σημείου.
     * Adds a new point.
     */
    fun addPoint(point: Point) {
        points[point.id] = point
    }

    /**
     * Προσθήκη νέας διαδρομής.
     * Adds a new route.
     */
    fun addRoute(route: Route) {
        routes[route.id] = route
    }

    /**
     * Επιστροφή σημείου.
     * Returns a point.
     */
    fun getPoint(pointId: String): Point? = points[pointId]

    /**
     * Επιστροφή διαδρομής.
     * Returns a route.
     */
    fun getRoute(routeId: String): Route? = routes[routeId]
}

