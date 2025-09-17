// DAO πρόσβασης για ειδοποίηση.
// DAO for notification access.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query(
        """
            SELECT id
            FROM notifications
            WHERE receiverId = :receiverId
              AND message = :message
              AND sentDate = :sentDate
              AND sentTime = :sentTime
            LIMIT 1
        """
    )
    suspend fun findIdForMessage(
        receiverId: String,
        message: String,
        sentDate: String,
        sentTime: String
    ): String?

    @Query("SELECT * FROM notifications WHERE receiverId = :userId ORDER BY sentDate DESC, sentTime DESC")
    fun getForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    fun getAll(): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)
}
