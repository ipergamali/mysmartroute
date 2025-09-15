package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Οντότητα που αποθηκεύει τα διαπιστευτήρια σύνδεσης ενός χρήστη.
 * Room entity storing encrypted credentials for offline authentication.
 */
@Entity(
    tableName = "auth_credentials",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["email"], unique = true)]
)
data class AuthenticationEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val encryptedPassword: String
)
