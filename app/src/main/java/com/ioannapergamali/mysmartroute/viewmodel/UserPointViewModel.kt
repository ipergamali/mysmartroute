package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.ioannapergamali.mysmartroute.repository.Point
import com.ioannapergamali.mysmartroute.repository.PointRepository
import com.ioannapergamali.mysmartroute.repository.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Απλό ViewModel για παρουσίαση και διαχείριση των σημείων που
 * έχουν προσθέσει οι χρήστες. Παρέχει λειτουργίες ενημέρωσης και
 * ομαδοποίησης ονομάτων.
 * Simple ViewModel for presenting and managing user-added points, offering update and grouping features.
 */
class UserPointViewModel(
    private val repository: PointRepository = PointRepository()
) : ViewModel() {

    private val _points = MutableStateFlow<List<Point>>(emptyList())
    val points: StateFlow<List<Point>> = _points

    init {
        refreshPoints()
    }

    /**
     * Φόρτωση όλων των σημείων για προβολή στον χρήστη.
     * Loads all points for user display.
     */
    fun refreshPoints() {
        _points.value = repository.getAllPoints()
    }

    /**
     * Προσθήκη νέου σημείου.
     * Adds a new point.
     */
    fun addPoint(point: Point) {
        repository.addPoint(point)
        refreshPoints()
    }

    /**
     * Ενημέρωση στοιχείων υπάρχοντος σημείου.
     * Updates details of an existing point.
     */
    fun updatePoint(id: String, name: String, details: String) {
        repository.updatePoint(id, name, details)
        refreshPoints()
    }

    /**
     * Ομαδοποίηση δύο σημείων με διατήρηση του πρώτου.
     * Merges two points keeping the first one.
     */
    fun mergePoints(keepId: String, removeId: String) {
        repository.mergePoints(keepId, removeId)
        refreshPoints()
    }

    /**
     * Διαγραφή σημείου
     * Deletes a point
     */
    fun deletePoint(id: String) {
        repository.deletePoint(id)
        refreshPoints()
    }

    /**
     * Προσθήκη διαδρομής για τις δοκιμές.
     * Adds a route for testing purposes.
     */
    fun addRoute(route: Route) {
        repository.addRoute(route)
    }
}
