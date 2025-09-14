package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO για πρόσβαση στις λεπτομέρειες των κρατήσεων θέσεων.
 */
@Dao
interface SeatReservationDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: SeatReservationDetailEntity)

    @Query("SELECT * FROM seat_reservation_details WHERE reservationId = :reservationId")
    fun getForReservation(reservationId: String): Flow<List<SeatReservationDetailEntity>>

    @Query("SELECT * FROM seat_reservation_details")
    fun getAll(): Flow<List<SeatReservationDetailEntity>>

    @Query("DELETE FROM seat_reservation_details WHERE reservationId = :reservationId")
    suspend fun deleteForReservation(reservationId: String)
}
