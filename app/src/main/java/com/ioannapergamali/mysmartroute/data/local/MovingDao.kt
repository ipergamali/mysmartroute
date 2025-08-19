package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moving: MovingEntity)

    @Query("SELECT * FROM movings WHERE userId = :userId")
    fun getMovingsForUser(userId: String): kotlinx.coroutines.flow.Flow<List<MovingEntity>>

    @Query("SELECT * FROM movings")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<MovingEntity>>

    @Query("DELETE FROM movings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM movings WHERE driverId = :driverId")
    suspend fun deleteForDriver(driverId: String)

    @Query("DELETE FROM movings WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Query("SELECT COUNT(*) FROM movings WHERE routeId = :routeId AND date = :date")
    suspend fun countForRoute(routeId: String, date: Long): Int

    @Query(
        "SELECT COUNT(*) FROM movings WHERE routeId = :routeId AND date = :date AND status = 'completed'"
    )
    suspend fun countCompletedForRoute(routeId: String, date: Long): Int
}
