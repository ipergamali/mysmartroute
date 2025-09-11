// DAO πρόσβασης για στάσεις λεωφορείου διαδρομής.
// DAO for route bus station access.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteBusStationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: RouteBusStationEntity)

    @Query("SELECT * FROM route_bus_station WHERE routeId = :routeId ORDER BY position")
    fun getStationsForRoute(routeId: String): Flow<List<RouteBusStationEntity>>

    @Query("DELETE FROM route_bus_station WHERE routeId = :routeId")
    suspend fun deleteStationsForRoute(routeId: String)

    @Query("UPDATE route_bus_station SET poiId = :newId WHERE poiId = :oldId")
    suspend fun updatePoiReferences(oldId: String, newId: String)

    @Query("SELECT * FROM route_bus_station")
    fun getAll(): Flow<List<RouteBusStationEntity>>
}
