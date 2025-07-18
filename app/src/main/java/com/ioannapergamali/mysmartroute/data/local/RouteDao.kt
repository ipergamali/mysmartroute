package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity)

    @Query("SELECT * FROM routes")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE userId = :userId")
    fun getRoutesForUser(userId: String): kotlinx.coroutines.flow.Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): RouteEntity?
}
