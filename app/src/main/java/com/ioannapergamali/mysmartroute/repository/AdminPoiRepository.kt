package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository για τον διαχειριστή ώστε να ελέγχει ονόματα PoI,
 * να ενημερώνει στοιχεία και να ομαδοποιεί σημεία.
 */
class AdminPoiRepository(private val db: MySmartRouteDatabase) {
    private val poiDao = db.poIDao()
    private val routePointDao = db.routePointDao()
    private val routeDao = db.routeDao()

    /** Επιστροφή όλων των ονομάτων σημείων που έχουν καταχωριστεί. */
    fun getPoiNames(): Flow<List<String>> = poiDao.getAll().map { list -> list.map { it.name } }

    /** Επιστροφή όλων των σημείων. */
    fun getAllPois(): Flow<List<PoIEntity>> = poiDao.getAll()

    /** Ενημέρωση στοιχείων σημείου. */
    suspend fun updatePoi(poi: PoIEntity) {
        poiDao.insert(poi)
    }

    /** Διαγραφή σημείου. */
    suspend fun deletePoi(id: String) {
        poiDao.deleteById(id)
    }

    /**
     * Συγχώνευση δύο σημείων. Το removeId διαγράφεται και όλες οι
     * διαδρομές/σημεία που το αναφέρουν ενημερώνονται να δείχνουν στο keepId.
     */
    suspend fun mergePois(keepId: String, removeId: String) {
        routePointDao.updatePoiReferences(removeId, keepId)
        routeDao.updatePoiReferences(removeId, keepId)
        poiDao.deleteById(removeId)
    }
}

