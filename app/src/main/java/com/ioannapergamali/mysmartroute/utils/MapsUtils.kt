package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.json.JSONArray
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

object MapsUtils {
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
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

        var durationSec = 0
        for (i in 0 until legs.length()) {
            durationSec += legs.getJSONObject(i)
                .getJSONObject("duration")
                .getInt("value")
        }
        return (durationSec + 59) / 60
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

        var durationSec = 0
        for (i in 0 until legs.length()) {
            durationSec += legs.getJSONObject(i)
                .getJSONObject("duration")
                .getInt("value")
        }
        val encoded = route.getJSONObject("overview_polyline").getString("points")
        val durationMin = (durationSec + 59) / 60
        return DirectionsData(durationMin, decodePolyline(encoded), status)
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
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext 0
                val body = response.body?.string() ?: return@withContext 0
                return@withContext parseDuration(body)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Duration request failed", e)
            return@withContext 0
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
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext DirectionsData(0, emptyList(), "NO_RESPONSE")
                if (!response.isSuccessful) return@withContext DirectionsData(0, emptyList(), "HTTP_${response.code}")
                return@withContext parseDirections(body)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Directions request failed", e)
            return@withContext DirectionsData(0, emptyList(), "EXCEPTION")
        }
    }

    suspend fun fetchNearbyPlaceName(
        location: LatLng,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        val nearbyUrl =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&rankby=distance&type=establishment&key=$apiKey"
        val request = Request.Builder().url(nearbyUrl).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (response.isSuccessful) {
                val actualBody = body ?: return@withContext null
                val jsonObj = JSONObject(actualBody)
                val status = jsonObj.optString("status")
                if (status == "OK") {
                    val results = jsonObj.optJSONArray("results") ?: return@withContext null
                    if (results.length() > 0) {
                        return@withContext results.getJSONObject(0).optString("name")
                    }
                }
            }

            // Αν δεν βρέθηκε κάποιο κοντινό "establishment", χρησιμοποιούμε geocoding
            val geocodeUrl =
                "https://maps.googleapis.com/maps/api/geocode/json?" +
                    "latlng=${location.latitude},${location.longitude}&key=$apiKey"
            val geocodeRequest = Request.Builder().url(geocodeUrl).build()
            client.newCall(geocodeRequest).execute().use { geoResp ->
                val geoBody = geoResp.body?.string()
                if (!geoResp.isSuccessful) {
                    Log.e(TAG, "geocode name failed: ${geoResp.code} - ${geoResp.message}")
                    if (geoBody != null) Log.e(TAG, geoBody)
                    return@withContext null
                }
                val geocodeObj = JSONObject(geoBody ?: return@withContext null)
                val geoStatus = geocodeObj.optString("status")
                if (geoStatus != "OK") {
                    Log.e(TAG, "Geocode name request failed: $geoStatus")
                    geocodeObj.optString("error_message")?.let { Log.e(TAG, it) }
                    return@withContext null
                }
                val geoResults = geocodeObj.optJSONArray("results") ?: return@withContext null
                if (geoResults.length() == 0) return@withContext null
                return@withContext geoResults.getJSONObject(0).optString("formatted_address")
            }
        }
    }

    suspend fun fetchNearbyPlaceType(
        location: LatLng,
        apiKey: String
    ): Place.Type? = withContext(Dispatchers.IO) {
        val nearbyUrl =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${location.latitude},${location.longitude}" +
                "&rankby=distance&type=establishment&key=$apiKey"
        val request = Request.Builder().url(nearbyUrl).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            var typesArray: org.json.JSONArray? = null

            if (response.isSuccessful) {
                val jsonObj = JSONObject(body ?: return@withContext null)
                if (jsonObj.optString("status") == "OK") {
                    val results = jsonObj.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        typesArray = results.getJSONObject(0).optJSONArray("types")
                    }
                }
            }

            if (typesArray == null || typesArray!!.length() == 0) {
                // Χρήση geocoding αν δεν υπάρχουν αποτελέσματα establishment
                val geocodeUrl =
                    "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "latlng=${location.latitude},${location.longitude}&key=$apiKey"
                val geocodeRequest = Request.Builder().url(geocodeUrl).build()
                client.newCall(geocodeRequest).execute().use { geoResp ->
                    val geoBody = geoResp.body?.string()
                    if (!geoResp.isSuccessful) {
                        Log.e(TAG, "geocode type failed: ${geoResp.code} - ${geoResp.message}")
                        if (geoBody != null) Log.e(TAG, geoBody)
                        return@withContext null
                    }
                    val geoObj = JSONObject(geoBody ?: return@withContext null)
                    if (geoObj.optString("status") != "OK") {
                        Log.e(TAG, "Geocode type request failed: ${geoObj.optString("status")}")
                        geoObj.optString("error_message")?.let { Log.e(TAG, it) }
                        return@withContext null
                    }
                    val geoResults = geoObj.optJSONArray("results") ?: return@withContext null
                    if (geoResults.length() == 0) return@withContext null
                    typesArray = geoResults.getJSONObject(0).optJSONArray("types")
                }
            }

            val exclude = setOf("POINT_OF_INTEREST", "ESTABLISHMENT", "LOCALITY", "POLITICAL")
            if (typesArray != null) {
                for (i in 0 until typesArray!!.length()) {
                    val typeStr = typesArray!!.getString(i).uppercase().replace('-', '_')
                    if (typeStr in exclude) continue
                    val type = runCatching { enumValueOf<Place.Type>(typeStr) }.getOrNull()
                    if (type != null) return@withContext type
                }
            }

            return@withContext null
        }
    }

    suspend fun autocompleteHeraklion(query: String, apiKey: String): List<String> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val bias = "rectangle:35.28,25.05|35.40,25.20"
            val url =
                "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
                    "input=$encoded&types=address&locationbias=$bias&key=$apiKey"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                if (!response.isSuccessful) return@withContext emptyList()
                val obj = JSONObject(body)
                if (obj.optString("status") != "OK") return@withContext emptyList()
                val preds = obj.optJSONArray("predictions") ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until preds.length()) {
                    val desc = preds.getJSONObject(i).optString("description")
                    if (desc.isNotBlank()) list.add(desc)
                }
                return@withContext list
            }
        }
}
