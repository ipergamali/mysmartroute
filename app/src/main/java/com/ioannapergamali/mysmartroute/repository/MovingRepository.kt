package com.ioannapergamali.mysmartroute.repository

import com.ioannapergamali.mysmartroute.data.local.MovingDao
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Απλό repository που παρέχει τις μετακινήσεις από τη Room βάση.
 * Simple repository exposing movings from the Room database.
 */
@Singleton
class MovingRepository @Inject constructor(
    private val dao: MovingDao
) {
    /** Επιστρέφει όλες τις μετακινήσεις ως ροή. */
    fun getMovings(): Flow<List<MovingEntity>> = dao.getAll()

    /** Επιστρέφει τις εκκρεμείς μετακινήσεις με βάση την τρέχουσα στιγμή. */
    suspend fun getPendingMovings(now: Long = System.currentTimeMillis()): List<MovingEntity> =
        dao.getPendingMovings(now)
}
