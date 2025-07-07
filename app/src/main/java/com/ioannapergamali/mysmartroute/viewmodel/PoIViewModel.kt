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
            val exists = dao.findByLocation(lat, lng) != null || dao.findByName(name) != null
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
            _addState.value = AddPoiState.Success
        }
    }

    sealed class AddPoiState {
        object Idle : AddPoiState()
        object Success : AddPoiState()
        object Exists : AddPoiState()
        data class Error(val message: String) : AddPoiState()
    }

    fun resetAddState() {
        _addState.value = AddPoiState.Idle
    }
}
