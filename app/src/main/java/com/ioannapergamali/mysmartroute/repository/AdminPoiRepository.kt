package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.duplicatePoisByName
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository για τον διαχειριστή ώστε να ελέγχει ονόματα PoI,
 * να ενημερώνει στοιχεία και να ομαδοποιεί σημεία.
 *
 * Repository for administrators to review PoI names,
 * update their data and group related points.
 */
class AdminPoiRepository(private val db: MySmartRouteDatabase) {
    private val poiDao = db.poIDao()
    private val routePointDao = db.routePointDao()
    private val routeDao = db.routeDao()

    /**
     * Επιστροφή όλων των ονομάτων σημείων που έχουν καταχωριστεί.
     *
     * Returns all stored point names.
     */
    fun getPoiNames(): Flow<List<String>> = poiDao.getAll().map { list -> list.map { it.name } }

    /**
     * Επιστροφή όλων των σημείων.
     *
     * Returns all points.
     */
    fun getAllPois(): Flow<List<PoIEntity>> = poiDao.getAll()

    /**
     * Επιστρέφει ομάδες σημείων που μοιράζονται ίδιες συντεταγμένες
     * αλλά έχουν διαφορετικό όνομα. Χρήσιμο για εντοπισμό διπλών
     * καταχωρίσεων ώστε να συγχωνευθούν.
     *
     * Returns groups of points sharing coordinates but with different names,
     * useful to detect duplicates for merging.
     */
    fun getPoisWithSameCoordinatesDifferentName(): Flow<List<List<PoIEntity>>> =
        poiDao.getAll().map { pois ->
            pois.groupBy { it.lat to it.lng }
                .values
                .filter { group ->
                    group.size > 1 && group.map { it.name }.toSet().size > 1
                }
                .map { it }
        }

    /**
     * Επιστρέφει ομάδες σημείων με ακριβώς το ίδιο όνομα.
     * Χρήσιμο για εντοπισμό διπλών καταχωρίσεων βάσει ονομασίας.
     *
     * Returns groups of points sharing the same name, useful to detect
     * duplicate entries by name.
     */
    fun getPoisWithSameName(): Flow<List<List<PoIEntity>>> =
        poiDao.getAll().map { pois -> duplicatePoisByName(pois) }

    /**
     * Ενημέρωση στοιχείων σημείου.
     *
     * Updates a point's data.
     */
    suspend fun updatePoi(poi: PoIEntity) {
        FirebaseFirestore.getInstance()
            .collection("pois")
            .document(poi.id)
            .set(poi.toFirestoreMap())
            .await()
        poiDao.insert(poi)
    }

    /**
     * Διαγραφή σημείου.
     *
     * Deletes a point.
     */
    suspend fun deletePoi(id: String) {
        FirebaseFirestore.getInstance()
            .collection("pois")
            .document(id)
            .delete()
            .await()
        poiDao.deleteById(id)
    }

    /**
     * Συγχώνευση δύο σημείων. Το removeId διαγράφεται και όλες οι
     * διαδρομές/σημεία που το αναφέρουν ενημερώνονται να δείχνουν στο keepId.
     *
     * Merges two points: `removeId` is deleted and all references are
     * updated to point to `keepId`.
     */
    suspend fun mergePois(keepId: String, removeId: String) {
        val db = FirebaseFirestore.getInstance()
        val removeRef = db.collection("pois").document(removeId)
        val keepRef = db.collection("pois").document(keepId)

        try {
            val pointsRoutes = db.collection("routes")
                .whereArrayContains("points", removeRef)
                .get().await()
            val startRoutes = db.collection("routes")
                .whereEqualTo("start", removeRef)
                .get().await()
            val endRoutes = db.collection("routes")
                .whereEqualTo("end", removeRef)
                .get().await()
            val allDocs = (pointsRoutes.documents + startRoutes.documents + endRoutes.documents)
                .distinctBy { it.id }

            db.runTransaction { tx ->
                allDocs.forEach { doc ->
                    val points = doc.get("points") as? MutableList<Any?> ?: mutableListOf()
                    var updatedPoints = false
                    for (i in points.indices) {
                        val ref = points[i]
                        if (ref is DocumentReference && ref.id == removeId) {
                            points[i] = keepRef
                            updatedPoints = true
                        }
                    }
                    val startRef = doc.get("start") as? DocumentReference
                    val endRef = doc.get("end") as? DocumentReference
                    if (startRef?.id == removeId) tx.update(doc.reference, "start", keepRef)
                    if (endRef?.id == removeId) tx.update(doc.reference, "end", keepRef)
                    if (updatedPoints) tx.update(doc.reference, "points", points)
                }
                tx.delete(removeRef)
            }.await()
        } catch (_: Exception) {
        }

        routePointDao.updatePoiReferences(removeId, keepId)
        routeDao.updatePoiReferences(removeId, keepId)
        poiDao.deleteById(removeId)
    }
}

