package com.ioannapergamali.mysmartroute.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.ioannapergamali.mysmartroute.model.classes.vehicles.RemoteVehicle

/**
 * Βοηθητικό αντικείμενο για άντληση διαθέσιμων οχημάτων από το Google Places API
 * για τον νομό Ηρακλείου.
 */
object VehiclePlacesUtils {
    private val client = OkHttpClient()
    private const val HERAKLION_LAT = 35.3387
    private const val HERAKLION_LNG = 25.1442

    suspend fun fetchVehicles(apiKey: String): List<RemoteVehicle> = withContext(Dispatchers.IO) {
        val url = buildUrl(apiKey)
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: return@withContext emptyList()
            if (!response.isSuccessful) return@withContext emptyList()
            val json = JSONObject(body)
            if (json.optString("status") != "OK") return@withContext emptyList()
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<RemoteVehicle>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val name = item.optString("name")
                val address = item.optString("vicinity")
                list.add(RemoteVehicle(name, address))
            }
            list
        }
    }

    private fun buildUrl(apiKey: String): String {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
            "location=$HERAKLION_LAT,$HERAKLION_LNG" +
            "&radius=50000&type=car_rental&key=$apiKey"
    }
}
