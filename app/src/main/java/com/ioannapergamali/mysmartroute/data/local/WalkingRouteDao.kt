// DAO πρόσβασης για διαδρομή πεζών.
// DAO for walking route access.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO για τις αποθηκευμένες διαδρομές πεζών.
 */
@Dao
interface WalkingRouteDao {
    @Insert
    suspend fun insert(route: WalkingRouteEntity): Long

    @Query("SELECT * FROM walking_routes")
    fun getAll(): Flow<List<WalkingRouteEntity>>
}

