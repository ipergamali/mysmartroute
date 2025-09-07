package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO για πρόσβαση στα αποθηκευμένα σημεία ενδιαφέροντος ενός χρήστη.
 * DAO for accessing user saved points of interest.
 */
@Dao
interface UserPoiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserPoiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<UserPoiEntity>)

    @Query("SELECT * FROM user_pois")
    fun getAll(): Flow<List<UserPoiEntity>>

    @Query("SELECT poiId FROM user_pois WHERE userId = :userId")
    fun getPoiIds(userId: String): Flow<List<String>>

    @Query("DELETE FROM user_pois WHERE userId = :userId AND poiId = :poiId")
    suspend fun delete(userId: String, poiId: String)

    @Query("DELETE FROM user_pois WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
