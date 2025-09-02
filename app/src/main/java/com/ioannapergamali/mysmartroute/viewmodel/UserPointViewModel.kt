package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.ioannapergamali.mysmartroute.repository.Point
import com.ioannapergamali.mysmartroute.repository.PointRepository
import com.ioannapergamali.mysmartroute.repository.Route
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
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
        refreshPoints()
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

    /**
     * Επιστρέφει τα POI που δεν έχουν ίδιες συντεταγμένες με τα ήδη
     * αποθηκευμένα σημεία χρηστών. Χρησιμοποιείται για να αποφεύγονται
     * διπλότυπα κατά την προσθήκη νέου σημείου.
     */
    fun availablePois(allPois: List<PoIEntity>): List<PoIEntity> {
        val occupied = points.value.mapNotNull { point ->
            allPois.find { it.id == point.id }?.let { it.lat to it.lng }
        }.toSet()
        return allPois.filter { (it.lat to it.lng) !in occupied }
    }
}
