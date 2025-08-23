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

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePointsForRoute(routeId: String)

    @Query("UPDATE route_points SET poiId = :newId WHERE poiId = :oldId")
    suspend fun updatePoiReferences(oldId: String, newId: String)

    @Query("SELECT * FROM route_points")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<RoutePointEntity>>
}
