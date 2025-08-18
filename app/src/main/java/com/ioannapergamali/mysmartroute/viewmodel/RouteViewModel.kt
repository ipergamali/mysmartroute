package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toRouteEntity
import com.ioannapergamali.mysmartroute.utils.toRouteWithPoints
import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ViewModel για διαχείριση διαδρομών στο Firestore και στη Room DB.
 */
class RouteViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _routes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val routes: StateFlow<List<RouteEntity>> = _routes

    // Διατηρούμε προσωρινά τα επιλεγμένα σημεία μιας διαδρομής
    private val _currentRoute = MutableStateFlow<List<PoIEntity>>(emptyList())
    val currentRoute: StateFlow<List<PoIEntity>> = _currentRoute

    /**
     * Προσθέτει ένα σημείο στη τρέχουσα διαδρομή εφόσον δεν είναι ίδιο με το
     * τελευταίο καταχωρημένο.
     * @return true αν το σημείο προστέθηκε, false αν ήδη υπάρχει τελευταίο
     */
    fun addPoiToCurrentRoute(poi: PoIEntity): Boolean {
        val last = _currentRoute.value.lastOrNull()
        return if (last?.id != poi.id) {
            _currentRoute.value = _currentRoute.value + poi
            true
        } else {
            false
        }
    }

    fun removePoiAt(index: Int) {
        val list = _currentRoute.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _currentRoute.value = list
        }
    }

    fun clearCurrentRoute() {
        _currentRoute.value = emptyList()
    }

    fun loadRoutes(context: Context, includeAll: Boolean = false) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val routeDao = db.routeDao()
            val pointDao = db.routePointDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            val query = if (includeAll) {
                firestore.collection("routes")
            } else {
                firestore.collection("routes").whereEqualTo("userId", userId)
            }

            val snapshot = runCatching { query.get().await() }.getOrNull()
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toRouteWithPoints() }
                _routes.value = list.map { it.first }
                list.forEach { (route, points) ->
                    routeDao.insert(route)
                    points.forEach { pointDao.insert(it) }
                }
            } else {
                _routes.value = if (includeAll) {
                    routeDao.getAll().first()
                } else if (userId != null) {
                    routeDao.getRoutesForUser(userId).first()
                } else {
                    emptyList()
                }
            }
        }
    }

    suspend fun getPointsCount(context: Context, routeId: String): Int {
        val dao = MySmartRouteDatabase.getInstance(context).routePointDao()
        return dao.getPointsForRoute(routeId).first().size
    }

    suspend fun getRoutePois(context: Context, routeId: String): List<PoIEntity> {
        val db = MySmartRouteDatabase.getInstance(context)
        val pointDao = db.routePointDao()
        val poiDao = db.poIDao()
        val points = pointDao.getPointsForRoute(routeId).first()
        return points.mapNotNull { poiDao.findById(it.poiId) }
    }

    suspend fun addRoute(context: Context, poiIds: List<String>, name: String): String? {
        if (poiIds.size < 2 || name.isBlank()) return null
        val db = MySmartRouteDatabase.getInstance(context)
        val routeDao = db.routeDao()
        val pointDao = db.routePointDao()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val id = UUID.randomUUID().toString()
        val entity = RouteEntity(id, userId, name, poiIds.first(), poiIds.last())
        val points = poiIds.mapIndexed { index, p -> RoutePointEntity(id, index, p) }

        runCatching {
            firestore.collection("routes").document(id).set(entity.toFirestoreMap(points)).await()
        }

        routeDao.insert(entity)
        points.forEach { pointDao.insert(it) }

        return id
    }

    suspend fun updateRoute(context: Context, routeId: String, poiIds: List<String>) {
        if (routeId.isBlank() || poiIds.size < 2) return
        val db = MySmartRouteDatabase.getInstance(context)
        val routeDao = db.routeDao()
        val pointDao = db.routePointDao()

        val existing = routeDao.findById(routeId) ?: return
        val updated = existing.copy(startPoiId = poiIds.first(), endPoiId = poiIds.last())
        val points = poiIds.mapIndexed { index, p -> RoutePointEntity(routeId, index, p) }

        if (NetworkUtils.isInternetAvailable(context)) {
            firestore.collection("routes").document(routeId).set(updated.toFirestoreMap(points)).await()
        }

        routeDao.insert(updated)
        pointDao.deletePointsForRoute(routeId)
        points.forEach { pointDao.insert(it) }
    }

    /**
     * Υπολογίζει τη διάρκεια διαδρομής με βάση τα αποθηκευμένα σημεία και το επιλεγμένο όχημα.
     * Χρησιμοποιεί το Google Maps Directions API για να επιστρέψει τη διάρκεια σε λεπτά.
     */
    suspend fun getRouteDuration(
        context: Context,
        routeId: String,
        vehicleType: VehicleType
    ): Int {
        val pois = getRoutePois(context, routeId)
        if (pois.size < 2) return 0
        val origin = LatLng(pois.first().lat, pois.first().lng)
        val destination = LatLng(pois.last().lat, pois.last().lng)
        val waypoints = pois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
        val apiKey = MapsUtils.getApiKey(context)
        return MapsUtils.fetchDuration(origin, destination, apiKey, vehicleType, waypoints)
    }

    /**
     * Επιστρέφει τη διάρκεια και τα σημεία της διαδρομής μέσω του Directions API.
     */
    suspend fun getRouteDirections(
        context: Context,
        routeId: String,
        vehicleType: VehicleType
    ): Pair<Int, List<LatLng>> {
        val pois = getRoutePois(context, routeId)
        if (pois.size < 2) return 0 to emptyList()
        val origin = LatLng(pois.first().lat, pois.first().lng)
        val destination = LatLng(pois.last().lat, pois.last().lng)
        val waypoints = pois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
        val apiKey = MapsUtils.getApiKey(context)
        val data = MapsUtils.fetchDurationAndPath(origin, destination, apiKey, vehicleType, waypoints)
        return data.duration to data.points
    }
}
