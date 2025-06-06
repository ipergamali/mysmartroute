package com.ioannapergamali.mysmartroute.utils

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object MapsUtils {
    private val client = OkHttpClient()

    private fun buildDirectionsUrl(origin: LatLng, destination: LatLng, apiKey: String): String {
        val originParam = "${origin.latitude},${origin.longitude}"
        val destParam = "${destination.latitude},${destination.longitude}"
        return "https://maps.googleapis.com/maps/api/directions/json?origin=$originParam&destination=$destParam&key=$apiKey"
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

    suspend fun fetchDuration(origin: LatLng, destination: LatLng, apiKey: String): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildDirectionsUrl(origin, destination, apiKey)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext 0
            val body = response.body?.string() ?: return@withContext 0
            return@withContext parseDuration(body)
        }
    }
}
