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
 */
class UserPointViewModel(
    private val repository: PointRepository = PointRepository()
) : ViewModel() {

    private val _points = MutableStateFlow<List<Point>>(emptyList())
    val points: StateFlow<List<Point>> = _points

    init {
        preloadPoints()
        refreshPoints()
    }

    /** Προσθήκη αρχικών ενδεικτικών σημείων αν η λίστα είναι άδεια. */
    private fun preloadPoints() {
        if (repository.getAllPoints().isNotEmpty()) return
        repository.addPoint(
            Point(
                id = "1",
                name = "Πλατεία Συντάγματος",
                details = "Κέντρο Αθήνας"
            )
        )
        repository.addPoint(
            Point(
                id = "2",
                name = "Ακρόπολη",
                details = "Αρχαιολογικός χώρος"
            )
        )
    }

    /** Φόρτωση όλων των σημείων για προβολή στον χρήστη. */
    fun refreshPoints() {
        _points.value = repository.getAllPoints()
    }

    /** Προσθήκη νέου σημείου. */
    fun addPoint(point: Point) {
        repository.addPoint(point)
        refreshPoints()
    }

    /** Ενημέρωση στοιχείων υπάρχοντος σημείου. */
    fun updatePoint(id: String, name: String, details: String) {
        repository.updatePoint(id, name, details)
        refreshPoints()
    }

    /** Ομαδοποίηση δύο σημείων με διατήρηση του πρώτου. */
    fun mergePoints(keepId: String, removeId: String) {
        repository.mergePoints(keepId, removeId)
        refreshPoints()
    }

    /** Διαγραφή σημείου */
    fun deletePoint(id: String) {
        repository.deletePoint(id)
        refreshPoints()
    }

    /** Προσθήκη διαδρομής για τις δοκιμές. */
    fun addRoute(route: Route) {
        repository.addRoute(route)
    }
}
