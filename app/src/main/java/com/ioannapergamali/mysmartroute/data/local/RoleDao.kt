package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(role: RoleEntity)

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun getRole(id: String): RoleEntity?

    /** Επιστρέφει όλους τους ρόλους της βάσης. */
    @Query("SELECT * FROM roles")
    fun getAllRoles(): kotlinx.coroutines.flow.Flow<List<RoleEntity>>
}
