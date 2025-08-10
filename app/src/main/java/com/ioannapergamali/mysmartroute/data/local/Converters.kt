package com.ioannapergamali.mysmartroute.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.google.android.libraries.places.api.model.Place
import com.ioannapergamali.mysmartroute.model.enumerations.RequestStatus

/** Μετατροπές για αποθήκευση σύνθετων τύπων στη Room. */
object Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPoiType(type: Place.Type): String = type.name

    @TypeConverter
    fun toPoiType(value: String): Place.Type = enumValueOf(value)

    @TypeConverter
    fun fromAddress(address: PoiAddress): String = gson.toJson(address)

    @TypeConverter
    fun toAddress(json: String): PoiAddress =
        gson.fromJson(json, PoiAddress::class.java)

    @TypeConverter
    fun fromRequestStatus(status: RequestStatus): String = status.name

    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = enumValueOf(value)
}
