package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

object MapsUtils {
    private val client = OkHttpClient()
    private const val TAG = "MapsUtils"

    fun getApiKey(context: Context): String {
        return try {
            val info = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            info.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** Result from a Directions API request */
    data class DirectionsData(
        val duration: Int,
        val points: List<LatLng>,
        val status: String
    )

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }

    private fun vehicleToMode(vehicleType: VehicleType): String = when (vehicleType) {
        VehicleType.BICYCLE -> "bicycling"
        VehicleType.BIGBUS, VehicleType.SMALLBUS -> "transit"
        else -> "driving"
    }

    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        vehicleType: VehicleType,
        waypoints: List<LatLng> = emptyList()
    ): String {
        val originParam = "${origin.latitude},${origin.longitude}"
        val destParam = "${destination.latitude},${destination.longitude}"
        val modeParam = vehicleToMode(vehicleType)
        val waypointParam = if (waypoints.isNotEmpty()) {
            "&waypoints=" + waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        } else ""
        return "https://maps.googleapis.com/maps/api/directions/json?origin=$originParam&destination=$destParam&mode=$modeParam$waypointParam&key=$apiKey"
    }

    private fun parseDuration(json: String): Int {
        val jsonObj = JSONObject(json)
        val routes = jsonObj.getJSONArray("routes")
        if (routes.length() == 0) return 0
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        if (legs.length() == 0) return 0
        val durationSec = legs.getJSONObject(0).getJSONObject("duration").getInt("value")
        return durationSec / 60
    }

    private fun parseDirections(json: String): DirectionsData {
        val jsonObj = JSONObject(json)
        val status = jsonObj.optString("status")
        if (status != "OK") return DirectionsData(0, emptyList(), status)
        val routes = jsonObj.getJSONArray("routes")
        if (routes.length() == 0) return DirectionsData(0, emptyList(), status)
        val route = routes.getJSONObject(0)
        val legs = route.getJSONArray("legs")
        if (legs.length() == 0) return DirectionsData(0, emptyList(), status)
        val durationSec = legs.getJSONObject(0)
            .getJSONObject("duration")
            .getInt("value")
        val encoded = route.getJSONObject("overview_polyline").getString("points")
        return DirectionsData(durationSec / 60, decodePolyline(encoded), status)
    }

    suspend fun fetchDuration(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        vehicleType: VehicleType,
        waypoints: List<LatLng> = emptyList()
    ): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(
            buildDirectionsUrl(origin, destination, apiKey, vehicleType, waypoints)
        ).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext 0
            val body = response.body?.string() ?: return@withContext 0
            return@withContext parseDuration(body)
        }
    }

    suspend fun fetchDurationAndPath(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        vehicleType: VehicleType,
        waypoints: List<LatLng> = emptyList()
    ): DirectionsData = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(
            buildDirectionsUrl(origin, destination, apiKey, vehicleType, waypoints)
        ).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext DirectionsData(0, emptyList(), "NO_RESPONSE")
            if (!response.isSuccessful) return@withContext DirectionsData(0, emptyList(), "HTTP_${response.code}")
            return@withContext parseDirections(body)
        }
    }

    suspend fun fetchNearbyPlaceName(
        location: LatLng,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        val url =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&rankby=distance&type=establishment&key=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.e(TAG, "fetchNearbyPlaceName failed: ${response.code} - ${response.message}")
                if (body != null) Log.e(TAG, body)
                return@withContext null
            }
            val actualBody = body ?: return@withContext null
            val jsonObj = JSONObject(actualBody)
            val status = jsonObj.optString("status")
            if (status != "OK") {
                Log.e(TAG, "Nearby place name request failed: $status")
                jsonObj.optString("error_message")?.let { Log.e(TAG, it) }
                return@withContext null
            }
            val results = jsonObj.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            return@withContext results.getJSONObject(0).optString("name")
        }
    }

    suspend fun fetchNearbyPlaceType(
        location: LatLng,
        apiKey: String
    ): Place.Type? = withContext(Dispatchers.IO) {
        val url =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&rankby=distance&type=establishment&key=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.e(TAG, "fetchNearbyPlaceType failed: ${response.code} - ${response.message}")
                if (body != null) Log.e(TAG, body)
                return@withContext null
            }
            val actualBody = body ?: return@withContext null
            val jsonObj = JSONObject(actualBody)
            val status = jsonObj.optString("status")
            if (status != "OK") {
                Log.e(TAG, "Nearby place type request failed: $status")
                jsonObj.optString("error_message")?.let { Log.e(TAG, it) }
                return@withContext null
            }
            val results = jsonObj.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val exclude = setOf("POINT_OF_INTEREST", "ESTABLISHMENT", "LOCALITY", "POLITICAL")
            for (r in 0 until results.length()) {
                val types = results.getJSONObject(r).optJSONArray("types") ?: continue
                for (i in 0 until types.length()) {
                    val typeStr = types.getString(i).uppercase().replace('-', '_')
                    if (typeStr in exclude) continue
                    val type = runCatching { enumValueOf<Place.Type>(typeStr) }.getOrNull()
                    if (type != null) return@withContext type
                }
            }
            return@withContext null
        }
    }
}
