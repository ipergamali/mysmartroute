package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import android.util.Log

object MapsUtils {
    private val client = OkHttpClient()

    data class DirectionsResult(
        val durationMinutes: Int,
        val points: List<LatLng>,
        val errorMessage: String? = null
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
        vehicleType: VehicleType
    ): String {
        val originParam = "${origin.latitude},${origin.longitude}"
        val destParam = "${destination.latitude},${destination.longitude}"
        val modeParam = vehicleToMode(vehicleType)
        return "https://maps.googleapis.com/maps/api/directions/json?origin=$originParam&destination=$destParam&mode=$modeParam&key=$apiKey"
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

    private fun parseDurationAndPolyline(json: String): DirectionsResult {
        val jsonObj = JSONObject(json)
        val status = jsonObj.optString("status")
        if (status != "OK") {
            val msg = jsonObj.optString("error_message")
            Log.w("MapsUtils", "Directions API error: $status $msg")
            return DirectionsResult(0, emptyList(), if (msg.isNotBlank()) msg else status)
        }

        val routes = jsonObj.getJSONArray("routes")
        if (routes.length() == 0) return DirectionsResult(0, emptyList(), "ZERO_RESULTS")

        val route = routes.getJSONObject(0)
        val legs = route.getJSONArray("legs")
        if (legs.length() == 0) return DirectionsResult(0, emptyList(), "ZERO_LEGS")

        val durationSec = legs.getJSONObject(0).getJSONObject("duration").getInt("value")
        val encoded = route.getJSONObject("overview_polyline").getString("points")
        return DirectionsResult(durationSec / 60, decodePolyline(encoded))
    }

    suspend fun fetchDuration(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        vehicleType: VehicleType
    ): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildDirectionsUrl(origin, destination, apiKey, vehicleType)).build()
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
        vehicleType: VehicleType
    ): DirectionsResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildDirectionsUrl(origin, destination, apiKey, vehicleType))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext DirectionsResult(0, emptyList(), "No response")
            if (!response.isSuccessful) {
                val json = JSONObject(body)
                val status = json.optString("status", response.code.toString())
                val msg = json.optString("error_message")
                Log.w("MapsUtils", "Directions call failed: $status $msg")
                return@withContext DirectionsResult(0, emptyList(), if (msg.isNotBlank()) msg else status)
            }
            return@withContext parseDurationAndPolyline(body)
        }
    }
}
