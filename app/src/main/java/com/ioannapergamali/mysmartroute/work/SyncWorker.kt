package com.ioannapergamali.mysmartroute.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel

/**
 * Worker που συγχρονίζει την τοπική και την απομακρυσμένη βάση.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result = try {
        Log.d(TAG, "Ξεκινά ο background συγχρονισμός")
        DatabaseViewModel().syncDatabasesSuspend(applicationContext)
        Log.d(TAG, "Ο background συγχρονισμός ολοκληρώθηκε επιτυχώς")
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "Αποτυχία background συγχρονισμού", e)
        Result.retry()
    }
}

