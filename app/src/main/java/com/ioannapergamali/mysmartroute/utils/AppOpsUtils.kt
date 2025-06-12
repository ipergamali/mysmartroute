package com.ioannapergamali.mysmartroute.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

/**
 * Βοηθητικές συναρτήσεις για ελέγχους AppOps.
 */
object AppOpsUtils {
    /**
     * Ελέγχει αν επιτρέπεται η λειτουργία READ_PHONE_STATE για την εφαρμογή.
     */
    fun isReadPhoneStateAllowed(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = if (Build.VERSION.SDK_INT >= 24) {
            context.packageManager.getPackageUid(context.packageName, 0)
        } else {
            context.applicationInfo.uid
        }
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_READ_PHONE_STATE, uid, context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Καταγράφει την ενέργεια READ_PHONE_STATE και επιστρέφει true αν έγινε δεκτή.
     */
    fun noteReadPhoneState(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = if (Build.VERSION.SDK_INT >= 24) {
            context.packageManager.getPackageUid(context.packageName, 0)
        } else {
            context.applicationInfo.uid
        }
        val result = appOps.noteOpNoThrow(AppOpsManager.OPSTR_READ_PHONE_STATE, uid, context.packageName)
        return result == AppOpsManager.MODE_ALLOWED
    }
}
