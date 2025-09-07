package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** DAO για πρόσβαση στις αγαπημένες διαδρομές. */
@Dao
interface FavoriteRouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT routeId FROM favorite_routes WHERE userId = :userId")
    fun getFavoritesForUser(userId: String): Flow<List<String>>

    @Query("SELECT * FROM favorite_routes")
    fun getAll(): Flow<List<FavoriteRouteEntity>>
}
