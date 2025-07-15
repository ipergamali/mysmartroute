package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType
import kotlinx.coroutines.launch
import java.util.UUID

/** ViewModel για αποθήκευση δηλώσεων μεταφοράς. */
class TransportDeclarationViewModel : ViewModel() {
    fun declareTransport(
        context: Context,
        routeId: String,
        vehicleType: VehicleType,
        cost: Double,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).transportDeclarationDao()
            val id = UUID.randomUUID().toString()
            val entity = TransportDeclarationEntity(id, routeId, vehicleType.name, cost, durationMinutes)
            dao.insert(entity)
        }
    }
}
