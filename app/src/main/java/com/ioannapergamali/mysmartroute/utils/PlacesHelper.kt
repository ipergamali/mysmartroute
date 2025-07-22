package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.ioannapergamali.mysmartroute.BuildConfig

object PlacesHelper {
    /** Επιστρέφει όλους τους διαθέσιμους τύπους από το Google Places SDK. */
    fun allPlaceTypes(): List<Place.Type> = Place.Type.values().toList()

    /**
     * Αρχικοποιεί το Places SDK εάν δεν είναι έτοιμο και επιστρέφει το [PlacesClient].
     */
    private fun getClient(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        return Places.createClient(context)
    }

}
