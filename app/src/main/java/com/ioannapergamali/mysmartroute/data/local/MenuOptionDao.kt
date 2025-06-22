package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MenuOptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(option: MenuOptionEntity)

    @Query("SELECT * FROM menu_options WHERE menuId = :menuId")
    suspend fun getOptionsForMenu(menuId: String): List<MenuOptionEntity>

    /** Επιστρέφει όλες τις επιλογές μενού. */
    @Query("SELECT * FROM menu_options")
    fun getAllMenuOptions(): kotlinx.coroutines.flow.Flow<List<MenuOptionEntity>>
}
