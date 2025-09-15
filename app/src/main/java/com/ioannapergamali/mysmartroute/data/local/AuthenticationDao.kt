package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO για αποθήκευση και ανάκτηση κρυπτογραφημένων διαπιστευτηρίων.
 * DAO used to persist and retrieve encrypted credentials.
 */
@Dao
interface AuthenticationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AuthenticationEntity)

    @Query("SELECT * FROM auth_credentials WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AuthenticationEntity?

    @Query("DELETE FROM auth_credentials WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)
}
