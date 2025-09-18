// DAO πρόσβασης για αξιολόγηση ταξιδιού.
// DAO for trip rating access.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.ioannapergamali.mysmartroute.model.classes.users.DriverRating
import com.ioannapergamali.mysmartroute.model.classes.users.PassengerSatisfaction

@Dao
interface TripRatingDao {
    @Query("SELECT * FROM trip_ratings")
    fun getAll(): Flow<List<TripRatingEntity>>

    @Query("SELECT * FROM trip_ratings WHERE movingId = :movingId AND userId = :userId LIMIT 1")
    suspend fun get(movingId: String, userId: String): TripRatingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: TripRatingEntity)

    @Query(
        """
            SELECT u.id AS driverId, u.name AS name, u.surname AS surname, AVG(r.rating) AS averageRating
            FROM trip_ratings r
            INNER JOIN movings m ON m.id = r.movingId
            INNER JOIN users u ON u.id = m.driverId
            WHERE m.driverId <> ''
            GROUP BY u.id
            ORDER BY averageRating DESC
            LIMIT 10
        """
    )
    fun getTopDrivers(): Flow<List<DriverRating>>

    @Query(
        """
            SELECT u.id AS driverId, u.name AS name, u.surname AS surname, AVG(r.rating) AS averageRating
            FROM trip_ratings r
            INNER JOIN movings m ON m.id = r.movingId
            INNER JOIN users u ON u.id = m.driverId
            WHERE m.driverId <> ''
            GROUP BY u.id
            ORDER BY averageRating ASC
            LIMIT 10
        """
    )
    fun getWorstDrivers(): Flow<List<DriverRating>>

    @Query(
        """
            SELECT u.id AS passengerId, u.name AS name, u.surname AS surname, AVG(r.rating) AS averageRating
            FROM trip_ratings r
            INNER JOIN users u ON u.id = r.userId
            WHERE r.userId <> ''
            GROUP BY u.id
            ORDER BY averageRating DESC, COUNT(r.rating) DESC
            LIMIT 10
        """
    )
    fun getMostSatisfiedPassengers(): Flow<List<PassengerSatisfaction>>

    @Query(
        """
            SELECT u.id AS passengerId, u.name AS name, u.surname AS surname, AVG(r.rating) AS averageRating
            FROM trip_ratings r
            INNER JOIN users u ON u.id = r.userId
            WHERE r.userId <> ''
            GROUP BY u.id
            ORDER BY averageRating ASC, COUNT(r.rating) DESC
            LIMIT 10
        """
    )
    fun getLeastSatisfiedPassengers(): Flow<List<PassengerSatisfaction>>
}
