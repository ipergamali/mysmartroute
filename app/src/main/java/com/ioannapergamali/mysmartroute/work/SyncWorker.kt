package com.ioannapergamali.mysmartroute.work

import android.content.Context
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

    override suspend fun doWork(): Result = try {
        DatabaseViewModel().syncDatabasesSuspend(applicationContext)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}

