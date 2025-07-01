package com.ioannapergamali.mysmartroute.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress
import com.ioannapergamali.mysmartroute.model.enumerations.PoIType

/** Μετατροπές για αποθήκευση σύνθετων τύπων στη Room. */
object Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPoiType(type: PoIType): String = type.name

    @TypeConverter
    fun toPoiType(value: String): PoIType = PoIType.valueOf(value)

    @TypeConverter
    fun fromAddress(address: PoiAddress): String = gson.toJson(address)

    @TypeConverter
    fun toAddress(json: String): PoiAddress =
        gson.fromJson(json, PoiAddress::class.java)
}
