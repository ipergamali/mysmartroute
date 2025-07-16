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
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toRouteEntity
import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
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

    fun addPoiToCurrentRoute(poi: PoIEntity) {
        _currentRoute.value = _currentRoute.value + poi
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

    fun loadRoutes(context: Context) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val dao = MySmartRouteDatabase.getInstance(context).routeDao()
            _routes.value = dao.getRoutesForUser(userId).first()
            firestore.collection("routes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { it.toRouteEntity() }
                    _routes.value = list
                    viewModelScope.launch { list.forEach { dao.insert(it) } }
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

    suspend fun addRoute(context: Context, poiIds: List<String>, name: String): Boolean {
        if (poiIds.size < 2) return false
        val db = MySmartRouteDatabase.getInstance(context)
        val routeDao = db.routeDao()
        val pointDao = db.routePointDao()

        if (routeDao.findByName(name) != null) return false

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val id = UUID.randomUUID().toString()
        val entity = RouteEntity(id, userId, name, poiIds.first(), poiIds.last())
        val points = poiIds.mapIndexed { index, p -> RoutePointEntity(id, index, p) }

        if (NetworkUtils.isInternetAvailable(context)) {
            firestore.collection("routes").document(id).set(entity.toFirestoreMap(points)).await()
        }

        routeDao.insert(entity)
        points.forEach { pointDao.insert(it) }

        return true
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
