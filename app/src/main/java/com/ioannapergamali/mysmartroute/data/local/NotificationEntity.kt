package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ειδοποίηση που προβάλλεται στον χρήστη στην επόμενη σύνδεση. */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val message: String = ""
)
