package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * Βοηθητικό αντικείμενο για παρόχους ειδικούς του MIUI.
 * Helper for MIUI specific providers.
 */
object MiuiUtils {
    private const val TAG = "MiuiUtils"
    private const val SERVICE_DELIVERY_AUTHORITY =
        "com.miui.personalassistant.servicedeliver.system.provider"

    /**
     * Ασφαλής κλήση μεθόδου στον πάροχο MIUI Service Delivery.
     * Safely calls a method on the MIUI Service Delivery provider.
     *
     * @param context Έγκυρο [Context].
     * @param methodName Η μέθοδος που θα καλεστεί στον πάροχο.
     * @param extras Προαιρετικά extras για την κλήση.
     * @return Το αποτέλεσμα [Bundle] ή `null` αν ο πάροχος δεν υπάρχει.
     *         The result [Bundle] from the provider, or null if it doesn't exist.
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
