package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: LanguageSettingEntity)

    @Query("SELECT * FROM app_language LIMIT 1")
    suspend fun get(): LanguageSettingEntity?

    @Query("SELECT * FROM app_language")
    fun getAll(): Flow<List<LanguageSettingEntity>>
}
