package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log

/** Helper for MIUI specific providers */
object MiuiUtils {
    private const val TAG = "MiuiUtils"
    private const val SERVICE_DELIVERY_AUTHORITY =
        "com.miui.personalassistant.servicedeliver.system.provider"

    /**
     * Safely calls a method on the MIUI Service Delivery provider.
     *
     * @param context A valid [Context].
     * @param methodName The method to invoke on the provider.
     * @param extras Optional extras for the call.
     * @return The result [Bundle] from the provider, or null if it doesn't exist.
     */
    fun callServiceDelivery(
        context: Context,
        methodName: String,
        extras: Bundle? = null,
    ): Bundle? {
        val uri = Uri.parse("content://$SERVICE_DELIVERY_AUTHORITY")
        return try {
            context.contentResolver.call(uri, methodName, null, extras)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Service Delivery provider not found on device")
            null
        }
    }
}
