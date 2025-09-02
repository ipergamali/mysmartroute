package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO για την ανάκτηση πληροφοριών σχετικών με πεζές διαδρομές.
 */
@Dao
interface WalkingDao {
    /**
     * Επιστρέφει τα μοναδικά αναγνωριστικά διαδρομών για έναν χρήστη.
     */
    @Query("SELECT DISTINCT routeId FROM walking WHERE userId = :userId")
    fun getRouteIdsForUser(userId: String): Flow<List<String>>
}
