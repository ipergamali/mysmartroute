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


        val batch = db.batch()
        snapshot.documents.forEach { walk ->
            val updates = mutableMapOf<String, Any>()
            // Διαγράφουμε πάντα τα πεδία fromPoiId/toPoiId αν υπάρχουν
            updates["fromPoiId"] = FieldValue.delete()
            updates["toPoiId"] = FieldValue.delete()
            // Συμπληρώνουμε το startTime μόνο όταν λείπει
            if (walk.get("startTime") == null) {
                updates["startTime"] = Timestamp(Date(startTimeMillis))
            }
            batch.update(walk.reference, updates)
        }
        batch.commit().await()

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

}

