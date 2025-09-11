// Οντότητα Room για σημείο ενδιαφέροντος.
// Room entity for po i.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.model.interfaces.PoI

@Entity(
    tableName = "pois",
    foreignKeys = [
        ForeignKey(
            entity = PoiTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["typeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["lat", "lng"]),
        Index("typeId")
    ]
)
data class PoIEntity(
    @PrimaryKey override val id: String = "",
    override val name: String = "",
    @Embedded override val address: PoiAddress = PoiAddress(),
    @ColumnInfo(name = "typeId") override val type: Place.Type = Place.Type.ESTABLISHMENT,
    val lat: Double = 0.0,
    val lng: Double = 0.0
) : PoI
