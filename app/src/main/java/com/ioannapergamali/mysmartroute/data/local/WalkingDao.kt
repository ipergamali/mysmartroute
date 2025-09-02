package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WalkingRouteEntity)

    @Query("SELECT * FROM walking WHERE userId = :userId")
    fun getRoutesForUser(userId: String): Flow<List<WalkingRouteEntity>>

    @Query("SELECT DISTINCT routeId FROM walking")
    suspend fun getAllRouteIds(): List<String>
}
