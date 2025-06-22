package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
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
            _pois.value = 7dao.getAll().first()
            db.collection("pois").get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PoIEntity::class.java)
                    }
                    _pois.value = list
                    viewModelScope.launch { dao.insertAll(list) }
                }
        }
    }

    fun addPoi(
        context: Context,
        name: String,
        description: String,
        type: String,
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
            val poi = PoIEntity(id, name, description, type, lat, lng)
            dao.insert(poi)
            val data = hashMapOf(
                "id" to id,
                "name" to name,
                "description" to description,
                "type" to type,
                "lat" to lat,
                "lng" to lng
            )
            db.collection("pois").document(id).set(data)
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
