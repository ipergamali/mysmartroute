package com.ioannapergamali.mysmartroute.repository

/**
 * Απλό repository για διαχείριση σημείων και διαδρομών.
 */
data class Route(
    val id: String,
    val pointIds: MutableList<String>
)

data class Point(
    val id: String,
    var name: String,
    var details: String
)

class PointRepository {
    private val points = mutableMapOf<String, Point>()
    private val routes = mutableMapOf<String, Route>()

    /** Επιστροφή όλων των ονομάτων σημείων */
    fun getAllPointNames(): List<String> = points.values.map { it.name }

    /** Επιστροφή όλων των σημείων */
    fun getAllPoints(): List<Point> = points.values.toList()

    /** Ενημέρωση στοιχείων ενός σημείου */
    fun updatePoint(pointId: String, newName: String, newDetails: String) {
        points[pointId]?.apply {
            name = newName
            details = newDetails
        }
    }

    /** Ομαδοποίηση δύο σημείων */
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

    /** Προσθήκη νέου σημείου */
    fun addPoint(point: Point) {
        points[point.id] = point
    }

    /** Προσθήκη νέας διαδρομής */
    fun addRoute(route: Route) {
        routes[route.id] = route
    }

    /** Επιστροφή σημείου */
    fun getPoint(pointId: String): Point? = points[pointId]

    /** Επιστροφή διαδρομής */
    fun getRoute(routeId: String): Route? = routes[routeId]
}

