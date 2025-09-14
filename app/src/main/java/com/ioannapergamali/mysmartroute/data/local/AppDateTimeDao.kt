package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDateTimeDao {
    @Query("SELECT * FROM app_datetime WHERE id = 1")
    suspend fun getDateTime(): AppDateTimeEntity?

    @Query("SELECT * FROM app_datetime")
    fun getAll(): Flow<List<AppDateTimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppDateTimeEntity)
}
