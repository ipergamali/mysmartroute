package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
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

    fun addRoute(context: Context, startPoiId: String, endPoiId: String, cost: Double) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).routeDao()
            val id = UUID.randomUUID().toString()
            val entity = RouteEntity(id, startPoiId, endPoiId, cost)
            if (NetworkUtils.isInternetAvailable(context)) {
                firestore.collection("routes").document(id).set(entity.toFirestoreMap())
                    .addOnSuccessListener { viewModelScope.launch { dao.insert(entity) } }
            } else {
                dao.insert(entity)
            }
        }
    }
}
