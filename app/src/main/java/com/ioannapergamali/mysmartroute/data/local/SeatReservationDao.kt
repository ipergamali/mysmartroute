package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SeatReservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reservation: SeatReservationEntity)

    @Query("SELECT * FROM seat_reservations WHERE userId = :userId")
    fun getReservationsForUser(userId: String): Flow<List<SeatReservationEntity>>

    @Query("SELECT * FROM seat_reservations WHERE routeId = :routeId")
    fun getReservationsForRoute(routeId: String): Flow<List<SeatReservationEntity>>
}
