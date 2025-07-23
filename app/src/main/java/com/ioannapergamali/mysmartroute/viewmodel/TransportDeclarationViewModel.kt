package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** ViewModel για αποθήκευση δηλώσεων μεταφοράς. */
class TransportDeclarationViewModel : ViewModel() {
    private val _declarations = MutableStateFlow<List<TransportDeclarationEntity>>(emptyList())
    val declarations: StateFlow<List<TransportDeclarationEntity>> = _declarations

    fun loadDeclarations(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).transportDeclarationDao()
            dao.getAll().collect { list ->
                _declarations.value = list
            }
        }
    }
    fun declareTransport(
        context: Context,
        routeId: String,
        vehicleType: VehicleType,
        cost: Double,
        durationMinutes: Int,
        date: Long
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).transportDeclarationDao()
            val id = UUID.randomUUID().toString()
            val entity = TransportDeclarationEntity(id, routeId, vehicleType.name, cost, durationMinutes, date)
            dao.insert(entity)
        }
    }
}
