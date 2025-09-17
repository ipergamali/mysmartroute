package com.ioannapergamali.mysmartroute.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ioannapergamali.mysmartroute.data.local.TransferRequestDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

object RequestNumberProvider {
    private const val TAG = "RequestNumberProvider"

    suspend fun nextRequestNumber(
        transferDao: TransferRequestDao,
        firestore: FirebaseFirestore
    ): Int = withContext(Dispatchers.IO) {
        val localLast = try {
            transferDao.getMaxRequestNumber() ?: 0
        } catch (error: Exception) {
            Log.w(TAG, "Αποτυχία ανάκτησης τοπικού request number", error)
            0
        }

        val remoteLast = try {
            firestore.collection("transfer_requests")
                .orderBy("requestNumber", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.getLong("requestNumber")
                ?.toInt()
                ?: 0
        } catch (error: Exception) {
            Log.w(TAG, "Αποτυχία ανάκτησης απομακρυσμένου request number", error)
            0
        }

        max(localLast, remoteLast) + 1
    }
}
