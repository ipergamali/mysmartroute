package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Προσπέλαση τύπων σημείων ενδιαφέροντος. */
@Dao
interface PoiTypeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<PoiTypeEntity>)

    @Query("SELECT * FROM poi_types")
    fun getAll(): Flow<List<PoiTypeEntity>>
}
