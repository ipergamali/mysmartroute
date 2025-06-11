package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity

@Database(
    entities = [UserEntity::class, VehicleEntity::class, PoIEntity::class, SettingsEntity::class],
    version = 4
)
abstract class MySmartRouteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun poIDao(): PoIDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MySmartRouteDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pois` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`lat` REAL NOT NULL, " +
                        "`lng` REAL NOT NULL, " +
                        "PRIMARY KEY(`id`)"
                        + ")"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settings` (" +
                        "`userId` TEXT NOT NULL, " +
                        "`theme` TEXT NOT NULL, " +
                        "`darkTheme` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`userId`)" +
                        ")"
                )
            }
        }

        fun getInstance(context: Context): MySmartRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MySmartRouteDatabase::class.java,
                    "mysmartroute.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
