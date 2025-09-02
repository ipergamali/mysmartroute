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

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RouteEntity?

    @Query("UPDATE routes SET startPoiId = CASE WHEN startPoiId = :oldId THEN :newId ELSE startPoiId END, endPoiId = CASE WHEN endPoiId = :oldId THEN :newId ELSE endPoiId END")
    suspend fun updatePoiReferences(oldId: String, newId: String)
}
