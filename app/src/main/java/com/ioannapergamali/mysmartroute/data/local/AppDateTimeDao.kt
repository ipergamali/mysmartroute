package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDateTimeDao {
    @Query("SELECT * FROM app_datetime WHERE id = 1")
    suspend fun getDateTime(): AppDateTimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppDateTimeEntity)
}
