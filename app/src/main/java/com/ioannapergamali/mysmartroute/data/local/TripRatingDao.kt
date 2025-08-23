package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.ioannapergamali.mysmartroute.model.classes.users.DriverRating

@Dao
interface TripRatingDao {
    @Query("SELECT * FROM trip_ratings")
    fun getAll(): Flow<List<TripRatingEntity>>

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
}
