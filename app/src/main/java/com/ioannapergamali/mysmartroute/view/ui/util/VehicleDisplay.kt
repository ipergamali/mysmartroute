package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

fun iconForVehicle(type: VehicleType): ImageVector = when (type) {
    VehicleType.CAR, VehicleType.TAXI -> Icons.Default.DirectionsCar
    VehicleType.BIGBUS, VehicleType.SMALLBUS -> Icons.Default.DirectionsBus
    VehicleType.BICYCLE -> Icons.Default.DirectionsBike
    VehicleType.MOTORBIKE -> Icons.Default.TwoWheeler
}

fun labelForVehicle(type: VehicleType): String = when (type) {
    VehicleType.CAR -> "Αυτοκίνητο"
    VehicleType.TAXI -> "Ταξί"
    VehicleType.BIGBUS -> "Λεωφορείο"
    VehicleType.SMALLBUS -> "Βαν"
    VehicleType.BICYCLE -> "Ποδήλατο"
    VehicleType.MOTORBIKE -> "Μηχανάκι"
}
