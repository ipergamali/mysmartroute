package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MenuDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menu: MenuEntity)

    @Transaction
    @Query("SELECT * FROM menus WHERE roleId = :roleId")
    suspend fun getMenusForRole(roleId: String): List<MenuWithOptions>

    /** Επιστρέφει όλα τα μενού της βάσης. */
    @Query("SELECT * FROM menus")
    suspend fun getAllMenus(): List<MenuEntity>
}
