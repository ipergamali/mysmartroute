package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.WalkingRouteDao
import com.ioannapergamali.mysmartroute.data.local.WalkingRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.WalkingRouteEntity
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WalkingRoutesViewModel(private val dao: WalkingRouteDao) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    val routes: StateFlow<List<WalkingRouteEntity>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRoute(name: String, origin: LatLng, destination: LatLng, apiKey: String) {
        viewModelScope.launch {
            val points = getWalkingRoute(origin, destination, apiKey)
            val encoded = PolyUtil.encode(points)
            val route = WalkingRouteEntity(
                name = name,
                startLat = origin.latitude,
                startLng = origin.longitude,
                endLat = destination.latitude,
                endLng = destination.longitude,
                polyline = encoded
            )
            val id = dao.insert(route)
            firestore.collection("walking_routes").document(id.toString()).set(route).await()
        }
    }

    private suspend fun getWalkingRoute(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): List<LatLng> = withContext(Dispatchers.IO) {
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&mode=walking&key=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val json = JSONObject(body)
            val encoded = json.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("overview_polyline")
                .getString("points")
            return@withContext PolyUtil.decode(encoded)
        }
    }

    companion object {
        fun Factory(context: Context) = viewModelFactory {
            initializer {
                val dao = WalkingRouteDatabase.getDatabase(context).dao()
                WalkingRoutesViewModel(dao)
            }
        }
    }
}

