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

    @Query("SELECT * FROM seat_reservations")
    fun getAll(): Flow<List<SeatReservationEntity>>

    @Query("DELETE FROM seat_reservations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM seat_reservations WHERE userId = :userId")
    fun getReservationsForUser(userId: String): Flow<List<SeatReservationEntity>>

    @Query("DELETE FROM seat_reservations WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)

    @Query("SELECT * FROM seat_reservations WHERE routeId = :routeId")
    fun getReservationsForRoute(routeId: String): Flow<List<SeatReservationEntity>>

    @Query("SELECT * FROM seat_reservations WHERE routeId = :routeId AND date = :date AND startTime = :startTime")
    fun getReservationsForRouteAndDateTime(routeId: String, date: Long, startTime: Long): Flow<List<SeatReservationEntity>>

    @Query("SELECT * FROM seat_reservations WHERE declarationId = :declarationId")
    fun getReservationsForDeclaration(declarationId: String): Flow<List<SeatReservationEntity>>

    /** Ελέγχει αν υπάρχει ήδη κράτηση για τον ίδιο χρήστη, διαδρομή, ημερομηνία και ώρα */
    @Query(
        "SELECT * FROM seat_reservations WHERE userId = :userId AND routeId = :routeId AND date = :date AND startTime = :startTime LIMIT 1"
    )
    suspend fun findUserReservation(
        userId: String,
        routeId: String,
        date: Long,
        startTime: Long
    ): SeatReservationEntity?
}
