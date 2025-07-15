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
}
