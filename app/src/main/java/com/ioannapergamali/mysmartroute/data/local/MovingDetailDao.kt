package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO για τις λεπτομέρειες των μετακινήσεων.
 */
@Dao
interface MovingDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: MovingDetailEntity)

    @Query("SELECT * FROM moving_details WHERE movingId = :movingId")
    fun getForMoving(movingId: String): Flow<List<MovingDetailEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM moving_details WHERE movingId = :movingId LIMIT 1)")
    suspend fun hasDetailsForMoving(movingId: String): Boolean

    @Query("DELETE FROM moving_details WHERE movingId = :movingId")
    suspend fun deleteForMoving(movingId: String)

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM moving_details md " +
            "INNER JOIN movings m ON m.id = md.movingId " +
            "WHERE m.routeId = :routeId AND m.userId = :userId " +
            "LIMIT 1" +
            ")"
    )
    suspend fun hasDetailsForRoute(routeId: String, userId: String): Boolean

    @Query(
        "SELECT md.vehicleId FROM moving_details md " +
            "INNER JOIN movings m ON m.id = md.movingId " +
            "WHERE m.routeId = :routeId AND m.userId = :userId " +
            "AND md.vehicleId <> '' LIMIT 1"
    )
    suspend fun findVehicleIdForRoute(routeId: String, userId: String): String?
}
