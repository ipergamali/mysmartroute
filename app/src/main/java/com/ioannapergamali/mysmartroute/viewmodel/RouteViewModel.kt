package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.RouteBusStationEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.model.Walk
import com.ioannapergamali.mysmartroute.repository.AdminWalkRepository
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toRouteEntity
import com.ioannapergamali.mysmartroute.utils.toRouteWithStations
import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.ioannapergamali.mysmartroute.utils.MapsUtils
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.abs

/**
 * ViewModel για διαχείριση διαδρομών στο Firestore και στη Room DB.
 * ViewModel for managing routes in Firestore and the Room DB.
 */
class RouteViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val adminWalkRepo = AdminWalkRepository()

    private val _routes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val routes: StateFlow<List<RouteEntity>> = _routes

    // Όλες οι πεζές μετακινήσεις για τον διαχειριστή
    // All walking movements for the administrator
    private val _walks = MutableStateFlow<List<Walk>>(emptyList())
    val walks: StateFlow<List<Walk>> = _walks

    // Διατηρούμε προσωρινά τα επιλεγμένα σημεία μιας διαδρομής
    // Temporarily store the selected points of a route
    private val _currentRoute = MutableStateFlow<List<PoIEntity>>(emptyList())
    val currentRoute: StateFlow<List<PoIEntity>> = _currentRoute

    /**
     * Προσθέτει ένα σημείο στην τρέχουσα διαδρομή αν δεν υπάρχει ήδη σημείο
     * με τις ίδιες συντεταγμένες.
     * Adds a point to the current route if no point with the same coordinates
     * already exists.
     * @return true αν το σημείο προστέθηκε, false διαφορετικά
     */
    fun addPoiToCurrentRoute(poi: PoIEntity): Boolean {
        val exists = _currentRoute.value.any {
            abs(it.lat - poi.lat) < 0.00001 &&
                abs(it.lng - poi.lng) < 0.00001
        }
        return if (!exists) {
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

    /**
     * Φορτώνει όλες τις πεζές μετακινήσεις από όλα τα `walks` υποσυλλογές.
     * Loads all walking movements from every `walks` subcollection.
     */
    fun loadAllWalksForAdmin() {
        viewModelScope.launch {
            _walks.value = runCatching { adminWalkRepo.fetchAllWalks() }.getOrElse { emptyList() }
        }
    }

    /**
     * Φορτώνει διαδρομές από το Firestore ή την τοπική βάση.
     * Loads routes from Firestore or the local database.
     */
    fun loadRoutes(context: Context, includeAll: Boolean = false) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val routeDao = db.routeDao()
            val pointDao = db.routePointDao()
            val busDao = db.routeBusStationDao()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            val query = if (includeAll) {
                firestore.collection("routes")
            } else {
                firestore.collection("routes").whereEqualTo("userId", userId)
            }

            val snapshot = runCatching { query.get().await() }.getOrNull()
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toRouteWithStations() }
                _routes.value = list.map { it.first }
                list.forEach { (route, points, busStations) ->
                    routeDao.insert(route)
                    points.forEach { pointDao.insert(it) }
                    busStations.forEach { busDao.insert(it) }
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

    /**
     * Φορτώνει διαδρομές που δεν έχουν αποθηκευμένη διάρκεια περπατήματος.
     * Loads routes lacking stored walking duration.
     */
    fun loadRoutesWithoutDuration() {
        viewModelScope.launch {
            val snapshot = runCatching {
                firestore.collection("routes").get().await()
            }.getOrNull()

            val result = snapshot?.documents?.mapNotNull { doc ->
                val hasWalks = runCatching {
                    doc.reference.collection("walks").limit(1).get().await().isEmpty.not()
                }.getOrDefault(false)
                if (!hasWalks) doc.toRouteEntity() else null
            } ?: emptyList()

            _routes.value = result
        }
    }

    /**
     * Φορτώνει τα routes που εμφανίζονται στις υποσυλλογές `walks`.
     * Loads routes that appear in `walks` subcollections using the `routeId` reference.
     */
    fun loadRoutesFromWalks() {
        viewModelScope.launch {
            val snapshot = runCatching {
                firestore.collectionGroup("walks").get().await()
            }.getOrNull()

            val routeEntities = snapshot?.documents
                ?.mapNotNull { doc ->
                    val routeRef = doc.reference.parent.parent
                    if (routeRef?.parent?.id == "routes") {
                        runCatching { routeRef.get().await().toRouteEntity() }.getOrNull()
                    } else {
                        null
                    }
                }
                ?.distinctBy { it.id }
                ?: emptyList()

            _routes.value = routeEntities

        }
    }

    /**
     * Φορτώνει τις διαδρομές που υπάρχουν στο subcollection `walks` του τρέχοντος χρήστη.
     * Loads routes from the current user's `walks` subcollection by reading each `routeId`.
     */
    fun loadUserWalkingRoutes() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val snapshot = runCatching {
                firestore.collection("users")
                    .document(userId)
                    .collection("walks")
                    .get()
                    .await()
            }.getOrNull()

            val routeEntities = snapshot?.documents
                ?.mapNotNull { it.getDocumentReference("routeId") }
                ?.distinct()
                ?.mapNotNull { ref ->
                    runCatching { ref.get().await().toRouteEntity() }.getOrNull()
                }
                ?: emptyList()

            _routes.value = routeEntities
        }
    }

    /**
     * Επιστρέφει τον αριθμό σημείων μιας διαδρομής.
     * Returns the number of points for a route.
     */
    suspend fun getPointsCount(context: Context, routeId: String): Int {
        val dao = MySmartRouteDatabase.getInstance(context).routePointDao()
        return dao.getPointsForRoute(routeId).first().size
    }

    /**
     * Ανακτά τα σημεία ενδιαφέροντος μιας διαδρομής.
     * Retrieves the points of interest of a route.
     */
    suspend fun getRoutePois(context: Context, routeId: String): List<PoIEntity> {
        val db = MySmartRouteDatabase.getInstance(context)
        val pointDao = db.routePointDao()
        val poiDao = db.poIDao()
        val points = pointDao.getPointsForRoute(routeId).first()
        return points.mapNotNull { poiDao.findById(it.poiId) }
    }

    /**
     * Επιστρέφει μόνο τις στάσεις λεωφορείου μιας διαδρομής.
     * Returns only the bus stations of a route.
     */
    suspend fun getRouteBusStations(context: Context, routeId: String): List<PoIEntity> {
        val db = MySmartRouteDatabase.getInstance(context)
        val busDao = db.routeBusStationDao()
        val poiDao = db.poIDao()
        val stations = busDao.getStationsForRoute(routeId).first()
        return stations.mapNotNull { poiDao.findById(it.poiId) }
    }

    /**
     * Ελέγχει αν μια διαδρομή περιέχει καθόλου στάσεις λεωφορείου.
     * Checks whether a route contains any bus stations.
     */
    suspend fun hasBusStations(context: Context, routeId: String): Boolean {
        val db = MySmartRouteDatabase.getInstance(context)
        val busDao = db.routeBusStationDao()
        return busDao.getStationsForRoute(routeId).first().isNotEmpty()
    }

    /**
     * Προσθέτει νέα διαδρομή με τα δοθέντα σημεία και όνομα.
     * Adds a new route with the provided points and name.
     */
    suspend fun addRoute(
        context: Context,
        poiIds: List<String>,
        name: String,
        busPoiIds: List<String> = emptyList()
    ): String? {
        if (poiIds.size < 2 || name.isBlank()) return null
        val db = MySmartRouteDatabase.getInstance(context)
        val routeDao = db.routeDao()
        val pointDao = db.routePointDao()
        val busDao = db.routeBusStationDao()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val id = UUID.randomUUID().toString()
        val entity = RouteEntity(id, userId, name, poiIds.first(), poiIds.last())
        val points = poiIds.mapIndexed { index, p -> RoutePointEntity(id, index, p) }

        val busIds = if (busPoiIds.isNotEmpty()) {
            busPoiIds
        } else {
            poiIds.filter { pid ->
                db.poIDao().findById(pid)?.type == Place.Type.BUS_STATION
            }
        }
        val busStations = busIds.mapIndexed { index, p -> RouteBusStationEntity(id, index, p) }


        runCatching {
            val routeRef = firestore.collection("routes").document(id)
            routeRef.set(entity.toFirestoreMap(points)).await()
            if (busStations.isNotEmpty()) {
                val busCol = routeRef.collection("bus_stations")
                busStations.forEach { station ->
                    busCol.document(station.position.toString())
                        .set(
                            mapOf(
                                "poi" to firestore.collection("pois").document(station.poiId),
                                "position" to station.position
                            )
                        )
                        .await()
                }
            }
        }.onFailure { e ->
            Log.e("RouteViewModel", "Αποτυχία αποθήκευσης στο Firestore", e)
        }

        routeDao.insert(entity)
        points.forEach { pointDao.insert(it) }
        busStations.forEach { busDao.insert(it) }

        return id
    }

    /**
     * Συγχρονίζει τα σημεία μιας διαδρομής από τη Room στη συλλογή `route_points` του Firestore.
     */
    suspend fun syncRoutePoints(context: Context, routeId: String) {
        val db = MySmartRouteDatabase.getInstance(context)
        val points = db.routePointDao().getPointsForRoute(routeId).first()
        FirebaseFirestore.getInstance()
            .collection("route_points")
            .document(routeId)
            .set(points)
            .await()
    }

    /**
     * Ενημερώνει υπάρχουσα διαδρομή με νέα σημεία ή όνομα.
     * Updates an existing route with new points or name.
     */
    suspend fun updateRoute(
        context: Context,
        routeId: String,
        poiIds: List<String>,
        newName: String? = null,
        busPoiIds: List<String> = emptyList()
    ) {
        if (routeId.isBlank() || poiIds.size < 2) return
        val db = MySmartRouteDatabase.getInstance(context)
        val routeDao = db.routeDao()
        val pointDao = db.routePointDao()
        val busDao = db.routeBusStationDao()

        val existing = routeDao.findById(routeId) ?: return
        val updated = existing.copy(
            name = newName.takeUnless { it.isNullOrBlank() } ?: existing.name,
            startPoiId = poiIds.first(),
            endPoiId = poiIds.last()
        )
        val points = poiIds.mapIndexed { index, p -> RoutePointEntity(routeId, index, p) }

        val busIds = if (busPoiIds.isNotEmpty()) {
            busPoiIds
        } else {
            poiIds.filter { pid ->
                db.poIDao().findById(pid)?.type == Place.Type.BUS_STATION
            }
        }
        val busStations = busIds.mapIndexed { index, p -> RouteBusStationEntity(routeId, index, p) }


        if (NetworkUtils.isInternetAvailable(context)) {
            val routeRef = firestore.collection("routes").document(routeId)
            routeRef.set(updated.toFirestoreMap(points)).await()
            val busCol = routeRef.collection("bus_stations")
            val existingBus = busCol.get().await()
            existingBus.documents.forEach { it.reference.delete().await() }
            busStations.forEach { station ->
                busCol.document(station.position.toString())
                    .set(
                        mapOf(
                            "poi" to firestore.collection("pois").document(station.poiId),
                            "position" to station.position
                        )
                    )
                    .await()
            }
        }

        routeDao.insert(updated)
        pointDao.deletePointsForRoute(routeId)
        points.forEach { pointDao.insert(it) }
        busDao.deleteStationsForRoute(routeId)
        busStations.forEach { busDao.insert(it) }
    }

    /**
     * Αποθηκεύει διάρκεια πεζοπορίας για συγκεκριμένη διαδρομή.
     * Saves walking duration for a specific route.
     */
    fun updateWalkDuration(context: Context, routeId: String, minutes: Int) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val route = db.routeDao().findById(routeId)
            if (NetworkUtils.isInternetAvailable(context) && route != null) {
                val walkEntry = mapOf(
                    "durationMinutes" to minutes
                )
                firestore.collection("routes").document(routeId)
                    .collection("walks")
                    .add(walkEntry)
                    .await()
            }
            _routes.value = _routes.value.filterNot { it.id == routeId }
        }
    }

    /**
     * Υπολογίζει τη συνολική απόσταση της διαδρομής χρησιμοποιώντας το Maps API.
     * Calculates total route distance using the Maps API.
     */
    suspend fun getRouteDistance(context: Context, routeId: String): Int {
        val pois = getRoutePois(context, routeId)
        if (pois.size < 2) return 0
        val origin = LatLng(pois.first().lat, pois.first().lng)
        val destination = LatLng(pois.last().lat, pois.last().lng)
        val waypoints = pois.drop(1).dropLast(1).map { LatLng(it.lat, it.lng) }
        val apiKey = MapsUtils.getApiKey(context)
        return MapsUtils.fetchWalkingDistance(origin, destination, apiKey, waypoints)
    }

    /**
     * Υπολογίζει τη διάρκεια διαδρομής με βάση τα αποθηκευμένα σημεία και το επιλεγμένο όχημα.
     * Calculates route duration from stored points and the chosen vehicle using the Directions API.
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
     * Returns route duration and path using the Directions API.
     */
    suspend fun getRouteDirections(
        context: Context,
        routeId: String,
        vehicleType: VehicleType
    ): Pair<Int, List<LatLng>> {
        val pois = getRoutePois(context, routeId)
        if (pois.size < 2) return 0 to emptyList()

        val apiKey = MapsUtils.getApiKey(context)
        var totalDuration = 0
        val allPoints = mutableListOf<LatLng>()

        for (i in 0 until pois.lastIndex) {
            val origin = LatLng(pois[i].lat, pois[i].lng)
            val destination = LatLng(pois[i + 1].lat, pois[i + 1].lng)
            val segment = MapsUtils.fetchDurationAndPath(origin, destination, apiKey, vehicleType)
            totalDuration += segment.duration
            if (allPoints.isEmpty()) {
                allPoints.addAll(segment.points)
            } else {
                allPoints.addAll(segment.points.drop(1))
            }
        }

        return totalDuration to allPoints
    }
}
