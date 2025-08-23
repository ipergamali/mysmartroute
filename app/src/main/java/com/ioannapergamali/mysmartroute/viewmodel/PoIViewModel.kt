package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.utils.toPoIEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ViewModel to manage Points of Interest (PoIs).
 */
class PoIViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pois = MutableStateFlow<List<PoIEntity>>(emptyList())
    val pois: StateFlow<List<PoIEntity>> = _pois

    private val _addState = MutableStateFlow<AddPoiState>(AddPoiState.Idle)
    val addState: StateFlow<AddPoiState> = _addState

    fun loadPois(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            _pois.value = dao.getAll().first()
            db.collection("pois").get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toPoIEntity()
                    }
                    _pois.value = list
                    viewModelScope.launch { dao.insertAll(list) }
                }
        }
    }

    fun addPoi(
        context: Context,
        name: String,
        address: PoiAddress,
        type: Place.Type,
        lat: Double,
        lng: Double
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            // Επιτρέπουμε αποθήκευση ίδιου σημείου αν έχει διαφορετικό όνομα.
            val exists = dao.findByName(name) != null
            if (exists) {
                _addState.value = AddPoiState.Exists
                return@launch
            }

            val id = UUID.randomUUID().toString()
            val poi = PoIEntity(
                id = id,
                name = name,
                address = address,
                type = type,
                lat = lat,
                lng = lng
            )
            dao.insert(poi)
            _pois.value = _pois.value + poi
            db.collection("pois")
                .document(id)
                .set(poi.toFirestoreMap())
            _addState.value = AddPoiState.Success(id)
        }
    }

    sealed class AddPoiState {
        object Idle : AddPoiState()
        data class Success(val id: String) : AddPoiState()
        object Exists : AddPoiState()
        data class Error(val message: String) : AddPoiState()
    }

    fun resetAddState() {
        _addState.value = AddPoiState.Idle
    }

    fun deletePoi(context: Context, id: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            dao.deleteById(id)
            _pois.value = _pois.value.filterNot { it.id == id }
            db.collection("pois").document(id).delete()
        }
    }

    fun updatePoi(context: Context, poi: PoIEntity) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            dao.insert(poi)
            _pois.value = _pois.value.map { if (it.id == poi.id) poi else it }
            db.collection("pois").document(poi.id).set(poi.toFirestoreMap())
        }
    }

    fun mergePois(context: Context, keepId: String, removeId: String) {
        viewModelScope.launch {
            val database = MySmartRouteDatabase.getInstance(context)
            val poiDao = database.poIDao()
            val routePointDao = database.routePointDao()
            val routeDao = database.routeDao()

            val removeRef = db.collection("pois").document(removeId)
            val keepRef = db.collection("pois").document(keepId)

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

            try {
                db.runTransaction { tx ->
                    allDocs.forEach { doc ->
                        val points = doc.get("points") as? MutableList<Any?> ?: mutableListOf()
                        var updatedPoints = false
                        for (i in points.indices) {
                            val ref = points[i]
                            if (ref is com.google.firebase.firestore.DocumentReference && ref.id == removeId) {
                                points[i] = keepRef
                                updatedPoints = true
                            }
                        }
                        val startRef = doc.get("start") as? com.google.firebase.firestore.DocumentReference
                        val endRef = doc.get("end") as? com.google.firebase.firestore.DocumentReference
                        if (startRef?.id == removeId) tx.update(doc.reference, "start", keepRef)
                        if (endRef?.id == removeId) tx.update(doc.reference, "end", keepRef)
                        if (updatedPoints) tx.update(doc.reference, "points", points)
                    }
                    tx.delete(removeRef)
                }.await()

                // Ενημέρωση των τοπικών διαδρομών ώστε να μην παραμένουν
                // αναφορές στο παλιό σημείο που διαγράφεται.
                routePointDao.updatePoiReferences(removeId, keepId)
                routeDao.updatePoiReferences(removeId, keepId)

                poiDao.deleteById(removeId)
                _pois.value = _pois.value.filterNot { it.id == removeId }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getPoi(context: Context, id: String): PoIEntity? {
        val dao = MySmartRouteDatabase.getInstance(context).poIDao()
        val local = dao.findById(id)
        if (local != null) return local
        return runCatching {
            db.collection("pois").document(id).get().await().toPoIEntity()
        }.getOrNull()?.also { dao.insert(it) }
    }

    suspend fun getPoiName(context: Context, id: String): String {
        return getPoi(context, id)?.name ?: ""
    }
}
