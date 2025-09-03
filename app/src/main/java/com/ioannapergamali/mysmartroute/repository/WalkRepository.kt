package com.ioannapergamali.mysmartroute.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Date


/**
 * Repository για διαχείριση των πεζών μετακινήσεων του χρήστη.
 * Αποθηκεύει την ώρα έναρξης και μετατρέπει παλιές εγγραφές
 * αφαιρώντας τα πεδία fromPoiId/toPoiId.
 */
class WalkRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /**
     * Καθαρίζει τυχόν παλιές εγγραφές `walks` του χρήστη αφαιρώντας τα
     * πεδία `fromPoiId`/`toPoiId` και συμπληρώνοντας το `startTime` με την
     * ώρα που επέλεξε ο χρήστης αν λείπει.
     */
    private suspend fun cleanupUserWalks(uid: String, startTimeMillis: Long) {
        val snapshot = db.collection("users")
            .document(uid)
            .collection("walks")
            .get()
            .await()
        snapshot.documents.forEach { walk ->
            val updates = mutableMapOf<String, Any>(
                "fromPoiId" to FieldValue.delete(),
                "toPoiId" to FieldValue.delete()
            )
            if (!walk.contains("startTime")) {
                updates["startTime"] = Timestamp(Date(startTimeMillis))
            }
            walk.reference.update(updates).await()
        }
    }
    /**

     * Ξεκινά μια πεζή μετακίνηση καταγράφοντας την ώρα που επέλεξε ο χρήστης.
     */
    suspend fun startWalk(startTimeMillis: Long) {
        val uid = auth.currentUser?.uid ?: return
        cleanupUserWalks(uid, startTimeMillis)
        val data = mapOf(
            "startTime" to Timestamp(Date(startTimeMillis))
        )
        db.collection("users")
            .document(uid)
            .collection("walks")
            .add(data)
            .await()
    }

    /**
     * Μετατρέπει παλιές εγγραφές στο `walks` αφαιρώντας τα fromPoiId/toPoiId
     * και προσθέτει startTime αν λείπει.
     */
    suspend fun migrateOldWalks() {
        val users = db.collection("users").get().await()
        users.forEach { user ->
            val walks = user.reference.collection("walks").get().await()
            walks.forEach { walk ->
                val updates = hashMapOf<String, Any>(
                    "fromPoiId" to FieldValue.delete(),
                    "toPoiId" to FieldValue.delete()
                )
                if (walk.getTimestamp("startTime") == null) {
                    updates["startTime"] = FieldValue.serverTimestamp()
                }
                walk.reference.update(updates).await()
            }
        }
    }
}

