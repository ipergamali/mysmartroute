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
 * ViewModel για τη διαχείριση σημείων ενδιαφέροντος (PoIs).
 * ViewModel to manage Points of Interest (PoIs).
 */
class PoIViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pois = MutableStateFlow<List<PoIEntity>>(emptyList())
    val pois: StateFlow<List<PoIEntity>> = _pois

    private val _addState = MutableStateFlow<AddPoiState>(AddPoiState.Idle)
    val addState: StateFlow<AddPoiState> = _addState

    /**
     * Φορτώνει όλα τα σημεία ενδιαφέροντος από την τοπική βάση και το Firestore.
     * Loads all points of interest from the local database and Firestore.
     */
    fun loadPois(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            _pois.value = dao.getAll().first()
            try {
                val snapshot = db.collection("pois").get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toPoIEntity()
                }
                _pois.value = list
                dao.insertAll(list)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Προσθέτει νέο σημείο ενδιαφέροντος χωρίς περιορισμό ονόματος ή συντεταγμένων.
     * Adds a new point of interest without enforcing unique name or coordinates.
     */
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

            val id = UUID.randomUUID().toString()
            val poi = PoIEntity(
                id = id,
                name = name,
                address = address,
                type = type,
                lat = lat,
                lng = lng
            )
            try {
                db.collection("pois")
                    .document(id)
                    .set(poi.toFirestoreMap())
                    .await()
                dao.insert(poi)
                _pois.value = _pois.value + poi
                _addState.value = AddPoiState.Success(id)
            } catch (e: Exception) {
                _addState.value = AddPoiState.Error(
                    e.localizedMessage ?: "Αποτυχία αποθήκευσης στο Firebase"
                )
            }
        }
    }

    sealed class AddPoiState {
        object Idle : AddPoiState()
        data class Success(val id: String) : AddPoiState()
        data class Error(val message: String) : AddPoiState()
    }

    /**
     * Επαναφέρει την κατάσταση προσθήκης στην αδράνεια.
     * Resets the add state to idle.
     */
    fun resetAddState() {
        _addState.value = AddPoiState.Idle
    }

    /**
     * Διαγράφει σημείο ενδιαφέροντος από τη βάση και το Firestore.
     * Deletes a point of interest from the database and Firestore.
     */
    fun deletePoi(context: Context, id: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            dao.deleteById(id)
            _pois.value = _pois.value.filterNot { it.id == id }
            db.collection("pois").document(id).delete()
        }
    }

    /**
     * Ενημερώνει τα δεδομένα ενός υπάρχοντος σημείου.
     * Updates the data of an existing point.
     */
    fun updatePoi(context: Context, poi: PoIEntity) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).poIDao()
            dao.insert(poi)
            _pois.value = _pois.value.map { if (it.id == poi.id) poi else it }
            db.collection("pois").document(poi.id).set(poi.toFirestoreMap())
        }
    }

    /**
     * Συγχωνεύει δύο σημεία, αντικαθιστώντας αναφορές του διαγραφόμενου.
     * Merges two points, replacing references of the removed one.
     */
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
                // Update local routes so they no longer reference the removed point.
                routePointDao.updatePoiReferences(removeId, keepId)
                routeDao.updatePoiReferences(removeId, keepId)

                poiDao.deleteById(removeId)
                _pois.value = _pois.value.filterNot { it.id == removeId }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Ανακτά σημείο ενδιαφέροντος από τοπική ή απομακρυσμένη βάση.
     * Retrieves a point of interest from local or remote storage.
     */
    suspend fun getPoi(context: Context, id: String): PoIEntity? {
        val dao = MySmartRouteDatabase.getInstance(context).poIDao()
        val local = dao.findById(id)
        if (local != null) return local
        return runCatching {
            db.collection("pois").document(id).get().await().toPoIEntity()
        }.getOrNull()?.also { dao.insert(it) }
    }

    /**
     * Επιστρέφει το όνομα του σημείου ή κενή συμβολοσειρά αν δεν βρεθεί.
     * Returns the point's name or an empty string if not found.
     */
    suspend fun getPoiName(context: Context, id: String): String {
        return getPoi(context, id)?.name ?: ""
    }
}
