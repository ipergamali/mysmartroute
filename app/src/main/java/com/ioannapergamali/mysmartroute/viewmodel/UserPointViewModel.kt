package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import com.ioannapergamali.mysmartroute.repository.Point
import com.ioannapergamali.mysmartroute.repository.PointRepository
import com.ioannapergamali.mysmartroute.repository.Route
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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

    private val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val _points = MutableStateFlow<List<Point>>(emptyList())
    val points: StateFlow<List<Point>> = _points

    init {
        firestore?.collection("user_points")?.get()?.addOnSuccessListener { snapshot ->
            val remote = snapshot.documents.mapNotNull { it.toPoint() }
            remote.forEach { repository.addPoint(it) }
            _points.value = repository.getAllPoints()
        }
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
        firestore?.collection("user_points")?.document(point.id)?.set(point)
        refreshPoints()
    }

    /**
     * Ενημέρωση στοιχείων υπάρχοντος σημείου.
     * Updates details of an existing point.
     */
    fun updatePoint(id: String, name: String, details: String) {
        repository.updatePoint(id, name, details)
        repository.getPoint(id)?.let { updated ->
            firestore?.collection("user_points")?.document(id)?.set(updated)
        }
        refreshPoints()
    }

    /**
     * Ομαδοποίηση δύο σημείων με διατήρηση του πρώτου.
     * Merges two points keeping the first one.
     */
    fun mergePoints(keepId: String, removeId: String) {
        repository.mergePoints(keepId, removeId)
        repository.getPoint(keepId)?.let { keep ->
            firestore?.collection("user_points")?.document(keepId)?.set(keep)
        }
        firestore?.collection("user_points")?.document(removeId)?.delete()
        refreshPoints()
    }

    /**
     * Διαγραφή σημείου
     * Deletes a point
     */
    fun deletePoint(id: String) {
        repository.deletePoint(id)
        firestore?.collection("user_points")?.document(id)?.delete()
        refreshPoints()
    }

    /**
     * Προσθήκη διαδρομής για τις δοκιμές.
     * Adds a route for testing purposes.
     */
    fun addRoute(route: Route) {
        repository.addRoute(route)
    }

    private fun DocumentSnapshot.toPoint(): Point? = runCatching {
        val pid = getString("id") ?: id
        val name = getString("name") ?: ""
        val details = getString("details") ?: ""
        Point(pid, name, details)
    }.getOrNull()
}
