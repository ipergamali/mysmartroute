package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO για λεπτομέρειες δηλώσεων μεταφοράς. */
@Dao
interface TransportDeclarationDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<TransportDeclarationDetailEntity>)

    @Query("SELECT * FROM transport_declarations_details WHERE declarationId = :declarationId")
    suspend fun getForDeclaration(declarationId: String): List<TransportDeclarationDetailEntity>
}
