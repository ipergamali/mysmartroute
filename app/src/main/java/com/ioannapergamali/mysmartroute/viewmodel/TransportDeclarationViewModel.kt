package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** ViewModel για αποθήκευση δηλώσεων μεταφοράς. */
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

    fun loadDeclarations(context: Context, driverId: String? = null) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            val dao = db.transportDeclarationDao()
            val movingDao = db.movingDao()
            val flow = if (driverId == null) dao.getAll() else dao.getForDriver(driverId)
            flow.collect { list ->
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
    fun declareTransport(
        context: Context,
        routeId: String,
        driverId: String,
        vehicleId: String,
        vehicleType: VehicleType,
        seats: Int,
        cost: Double,
        durationMinutes: Int,
        date: Long,
        startTime: Long = 0L
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).transportDeclarationDao()
            val id = UUID.randomUUID().toString()
            val entity = TransportDeclarationEntity(id, routeId, driverId, vehicleId, vehicleType.name, cost, durationMinutes, seats, date, startTime)
            dao.insert(entity)
            try {
                FirebaseFirestore.getInstance()
                    .collection("transport_declarations")
                    .document(id)
                    .set(entity.toFirestoreMap())
                    .await()
                Log.d(TAG, "Declaration $id stored remotely")
            } catch (e: Exception) {
                Log.w(TAG, "Remote store failed", e)
                // Σε περίπτωση αποτυχίας, θα αποσταλεί αργότερα μέσω συγχρονισμού
            }
        }
    }

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
