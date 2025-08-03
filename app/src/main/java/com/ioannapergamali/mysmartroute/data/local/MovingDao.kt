package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moving: MovingEntity)

    @Query("SELECT * FROM movings WHERE userId = :userId")
    fun getMovingsForUser(userId: String): kotlinx.coroutines.flow.Flow<List<MovingEntity>>

    @Query("SELECT * FROM movings")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<MovingEntity>>

    @Query("DELETE FROM movings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
