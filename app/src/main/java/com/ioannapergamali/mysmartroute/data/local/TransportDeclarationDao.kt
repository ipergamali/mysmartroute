package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportDeclarationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(declaration: TransportDeclarationEntity)

    @Query("SELECT * FROM transport_declarations")
    fun getAll(): Flow<List<TransportDeclarationEntity>>

    @Query("SELECT * FROM transport_declarations WHERE driverId = :driverId")
    fun getForDriver(driverId: String): Flow<List<TransportDeclarationEntity>>

    @Query("SELECT * FROM transport_declarations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransportDeclarationEntity?

    @Query("DELETE FROM transport_declarations WHERE driverId = :driverId")
    suspend fun deleteForDriver(driverId: String)

    @Query("DELETE FROM transport_declarations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
