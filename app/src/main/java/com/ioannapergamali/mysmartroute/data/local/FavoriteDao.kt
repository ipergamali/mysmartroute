package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND preferred = 1")
    fun getPreferred(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND preferred = 0")
    fun getNonPreferred(userId: String): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM favorites WHERE userId = :userId AND vehicleType = :vehicleType")
    suspend fun delete(userId: String, vehicleType: String)

}
