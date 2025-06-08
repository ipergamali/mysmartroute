package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object MapsUtils {
    private val client = OkHttpClient()

    private fun buildDirectionsUrl(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        mode: String?
    ): String {
        val originParam = "${origin.latitude},${origin.longitude}"
        val destParam = "${destination.latitude},${destination.longitude}"
        val modeParam = mode?.let { "&mode=$it" } ?: ""
        return "https://maps.googleapis.com/maps/api/directions/json?origin=$originParam&destination=$destParam$modeParam&key=$apiKey"
    }

    private fun buildGeocodeUrl(address: String, apiKey: String): String {
        val encoded = URLEncoder.encode(address, "UTF-8")
        return "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&key=$apiKey"
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

    suspend fun fetchDuration(
        origin: LatLng,
        destination: LatLng,
        apiKey: String,
        mode: String? = null
    ): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildDirectionsUrl(origin, destination, apiKey, mode)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext 0
            val body = response.body?.string() ?: return@withContext 0
            return@withContext parseDuration(body)
        }
    }

    suspend fun geocode(address: String, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildGeocodeUrl(address, apiKey)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val jsonObj = JSONObject(body)
            val results = jsonObj.getJSONArray("results")
            if (results.length() == 0) return@withContext null
            val location = results.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")
            val lat = location.getDouble("lat")
            val lng = location.getDouble("lng")
            return@withContext LatLng(lat, lng)
        }
    }

    fun vehicleTypeToMode(type: VehicleType): String = when (type) {
        VehicleType.BICYCLE -> "bicycling"
        else -> "driving"
    }
}
