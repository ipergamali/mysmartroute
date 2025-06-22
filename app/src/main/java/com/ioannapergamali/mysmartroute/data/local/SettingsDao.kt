package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettings(userId: String): SettingsEntity?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): kotlinx.coroutines.flow.Flow<List<SettingsEntity>>
}
