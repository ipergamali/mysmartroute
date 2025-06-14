package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuthenticationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(auth: AuthenticationEntity)

    @Query("SELECT * FROM authentication WHERE uid = :uid LIMIT 1")
    suspend fun get(uid: String): AuthenticationEntity?
}
