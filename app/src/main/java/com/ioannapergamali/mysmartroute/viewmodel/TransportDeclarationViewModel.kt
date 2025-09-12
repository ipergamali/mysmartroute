package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDetailEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel για αποθήκευση δηλώσεων μεταφοράς.
 * ViewModel for storing transport declarations.
 */
class TransportDeclarationViewModel : ViewModel() {
    private val _declarations = MutableStateFlow<List<TransportDeclarationEntity>>(emptyList())
    val declarations: StateFlow<List<TransportDeclarationEntity>> = _declarations

    private val _pendingDeclarations = MutableStateFlow<List<TransportDeclarationEntity>>(emptyList())
    val pendingDeclarations: StateFlow<List<TransportDeclarationEntity>> = _pendingDeclarations

    private val _completedDeclarations = MutableStateFlow<List<TransportDeclarationEntity>>(emptyList())
    val completedDeclarations: StateFlow<List<TransportDeclarationEntity>> = _completedDeclarations

    companion object {
        private const val TAG = "TransportDeclVM"
    }

    /**
     * Φορτώνει δηλώσεις μεταφοράς και τις χωρίζει σε εκκρεμείς και ολοκληρωμένες.
     * Loads transport declarations splitting them into pending and completed.
     */
    fun loadDeclarations(context: Context, driverId: String? = null) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val dao = db.transportDeclarationDao()
            val detailDao = db.transportDeclarationDetailDao()
            val movingDao = db.movingDao()
            val flow = if (driverId == null) dao.getAll() else dao.getForDriver(driverId)
            flow.collect { list ->
                withContext(Dispatchers.IO) {
                    list.forEach { decl ->
                        val details = detailDao.getForDeclaration(decl.id)
                        if (details.isNotEmpty()) {
                            val first = details.first()
                            decl.vehicleId = first.vehicleId
                            decl.vehicleType = first.vehicleType
                            decl.seats = first.seats
                        }
                    }
                }
                _declarations.value = list
                val pending = mutableListOf<TransportDeclarationEntity>()
                val completed = mutableListOf<TransportDeclarationEntity>()
                withContext(Dispatchers.IO) {
                    list.forEach { decl ->
                        val count = movingDao.countCompletedForRoute(decl.routeId, decl.date)
                        if (count > 0) {
                            completed += decl
                        } else {
                            pending += decl
                        }
                    }
                }
                _pendingDeclarations.value = pending
                _completedDeclarations.value = completed
            }
        }
    }
    /**
     * Καταχωρεί νέα δήλωση μεταφοράς τοπικά και απομακρυσμένα.
     * Registers a new transport declaration locally and remotely.
     */
    suspend fun declareTransport(
        context: Context,
        routeId: String,
        driverId: String,
        seats: Int,
        cost: Double,
        durationMinutes: Int,
        date: Long,
        startTime: Long = 0L,
        details: List<TransportDeclarationDetailEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        val db = MySmartRouteDatabase.getInstance(context)
        val declDao = db.transportDeclarationDao()
        val detailDao = db.transportDeclarationDetailDao()
        val id = UUID.randomUUID().toString()
        val entity = TransportDeclarationEntity(id, routeId, driverId, cost, durationMinutes, date, startTime)
        entity.seats = seats
        if (details.isNotEmpty()) {
            entity.vehicleId = details.first().vehicleId
            entity.vehicleType = details.first().vehicleType
            entity.seats = details.first().seats
        }
        declDao.insert(entity)
        val detailEntities = details.map {
            val detailId = if (it.id.isBlank()) UUID.randomUUID().toString() else it.id
            it.copy(id = detailId, declarationId = id)
        }
        if (detailEntities.isNotEmpty()) {
            detailDao.insertAll(detailEntities)
        }
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore
                .collection("transport_declarations")
                .document(id)
                .set(entity.toFirestoreMap())
                .await()
            for (detail in detailEntities) {
                firestore
                    .collection("transport_declarations")
                    .document(id)
                    .collection("details")
                    .document(detail.id)
                    .set(detail.toFirestoreMap())
                    .await()
            }
            Log.d(TAG, "Declaration $id stored remotely")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote store failed", e)
            false
        }
    }

    /**
     * Διαγράφει δηλώσεις μεταφοράς τόσο από τη βάση όσο και από το Firestore.
     * Deletes transport declarations from the database and Firestore.
     */
    fun deleteDeclarations(context: Context, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = MySmartRouteDatabase.getInstance(context).transportDeclarationDao()
            dao.deleteByIds(ids.toList())
            _declarations.value = _declarations.value.filterNot { it.id in ids }
            ids.forEach { id ->
                FirebaseFirestore.getInstance().collection("transport_declarations").document(id).delete()
            }
        }
    }
}
