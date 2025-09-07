package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Έλεγχοι διαθεσιμότητας διαδικτύου.
 * Utilities for checking internet availability.
 */
object NetworkUtils {
    /**
     * Επιστρέφει true αν υπάρχει σύνδεση με το διαδίκτυο.
     * Returns true if the device has internet connectivity.
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
