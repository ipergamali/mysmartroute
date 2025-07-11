package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.utils.toRouteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            val dao = MySmartRouteDatabase.getInstance(context).routeDao()
            _routes.value = dao.getAll().first()
            firestore.collection("routes").get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { it.toRouteEntity() }
                    _routes.value = list
                    viewModelScope.launch { list.forEach { dao.insert(it) } }
                }
        }
    }

    fun addRoute(context: Context, poiIds: List<String>, name: String) {
        if (poiIds.size < 2) return
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val routeDao = db.routeDao()
            val pointDao = db.routePointDao()
            val id = UUID.randomUUID().toString()
            val entity = RouteEntity(id, name, poiIds.first(), poiIds.last())
            val points = poiIds.mapIndexed { index, p -> RoutePointEntity(id, index, p) }
            if (NetworkUtils.isInternetAvailable(context)) {
                firestore.collection("routes").document(id).set(entity.toFirestoreMap(points))
                    .addOnSuccessListener {
                        viewModelScope.launch {
                            routeDao.insert(entity)
                            points.forEach { pointDao.insert(it) }
                        }
                    }
            } else {
                routeDao.insert(entity)
                points.forEach { pointDao.insert(it) }
            }
        }
    }
}
