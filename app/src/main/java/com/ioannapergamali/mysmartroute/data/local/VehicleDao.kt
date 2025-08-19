package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicles WHERE userId = :userId")
    suspend fun getVehiclesForUser(userId: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): kotlinx.coroutines.flow.Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicle(id: String): VehicleEntity?

    @Query("DELETE FROM vehicles WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
