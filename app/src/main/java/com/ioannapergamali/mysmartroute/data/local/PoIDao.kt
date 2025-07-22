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
    fun getAll(): kotlinx.coroutines.flow.Flow<List<PoIEntity>>

    @Query("SELECT * FROM pois WHERE lat = :lat AND lng = :lng LIMIT 1")
    suspend fun findByLocation(lat: Double, lng: Double): PoIEntity?

    @Query("SELECT * FROM pois WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): PoIEntity?

    @Query("DELETE FROM pois WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM pois WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PoIEntity?
}
