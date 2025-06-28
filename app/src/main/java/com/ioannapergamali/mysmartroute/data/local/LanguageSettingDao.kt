package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LanguageSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: LanguageSettingEntity)

    @Query("SELECT * FROM app_language LIMIT 1")
    suspend fun get(): LanguageSettingEntity?
}
