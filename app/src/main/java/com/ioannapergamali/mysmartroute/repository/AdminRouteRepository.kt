package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class AdminRouteRepository(private val db: MySmartRouteDatabase) {
    private val routeDao = db.routeDao()
    private val pointDao = db.routePointDao()
    private val busDao = db.routeBusStationDao()

    fun getRoutesWithSameName(): Flow<List<List<RouteEntity>>> =
        routeDao.getAll().map { routes ->
            routes.groupBy { it.name.trim() }
                .values
                .filter { it.size > 1 }
        }

    suspend fun updateRoute(route: RouteEntity) {
        val points = pointDao.getPointsForRoute(route.id).first()
        FirebaseFirestore.getInstance()
            .collection("routes")
            .document(route.id)
            .set(route.toFirestoreMap(points))
            .await()
        routeDao.insert(route)
    }

    suspend fun mergeRoutes(keepId: String, removeId: String) {
        FirebaseFirestore.getInstance().collection("routes").document(removeId).delete().await()
        pointDao.deletePointsForRoute(removeId)
        busDao.deleteStationsForRoute(removeId)
        routeDao.deleteById(removeId)
    }
}
