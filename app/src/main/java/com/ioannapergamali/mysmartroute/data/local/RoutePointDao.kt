package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: RoutePointEntity)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY position")
    fun getPointsForRoute(routeId: String): kotlinx.coroutines.flow.Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM route_points")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<RoutePointEntity>>
}
