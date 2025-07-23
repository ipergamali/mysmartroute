package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(availability: AvailabilityEntity)

    @Query("SELECT * FROM availabilities")
    fun getAll(): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM availabilities WHERE userId = :userId")
    fun getForUser(userId: String): Flow<List<AvailabilityEntity>>
}
