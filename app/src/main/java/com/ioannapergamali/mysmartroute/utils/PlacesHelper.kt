package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.model.PlaceLikelihoodBufferResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.ioannapergamali.mysmartroute.BuildConfig

object PlacesHelper {
    /** Επιστρέφει μερικούς ενδεικτικούς τύπους σημείων ενδιαφέροντος. */
    fun samplePlaceTypes(): List<Place.Type> = listOf(
        Place.Type.RESTAURANT,
        Place.Type.HOTEL,
        Place.Type.PHARMACY,
        Place.Type.BUS_STATION
    )

    /**
     * Αρχικοποιεί το Places SDK εάν δεν είναι έτοιμο και επιστρέφει το [PlacesClient].
     */
    private fun getClient(context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        return Places.createClient(context)
    }

    /**
     * Παράδειγμα κλήσης που εμφανίζει τα κοντινά μέρη και τους τύπους τους στο log.
     */
    fun logCurrentPlaceTypes(context: Context) {
        val client = getClient(context)
        val request = FindCurrentPlaceRequest.newInstance(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.TYPES))
        client.findCurrentPlace(request).addOnSuccessListener { response ->
            for (likelihood in response.placeLikelihoods) {
                val place = likelihood.place
                Log.d("PlacesHelper", "Place: ${'$'}{place.name}, Types: ${'$'}{place.types}")
            }
        }.addOnFailureListener { e ->
            Log.e("PlacesHelper", "Σφάλμα εύρεσης τρεχουσών τοποθεσιών", e)
        }
    }
}
