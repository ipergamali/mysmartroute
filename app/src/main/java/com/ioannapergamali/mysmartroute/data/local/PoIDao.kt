package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PoIDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pois: List<PoIEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poi: PoIEntity)

    @Query("SELECT * FROM pois")
    suspend fun getAll(): List<PoIEntity>
}
