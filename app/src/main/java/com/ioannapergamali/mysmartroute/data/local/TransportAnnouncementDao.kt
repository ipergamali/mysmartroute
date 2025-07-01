package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportAnnouncementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(announcement: TransportAnnouncementEntity)

    @Query("SELECT * FROM transport_announcements WHERE driverId = :driverId")
    fun getAnnouncementsForDriver(driverId: String): Flow<List<TransportAnnouncementEntity>>
}
