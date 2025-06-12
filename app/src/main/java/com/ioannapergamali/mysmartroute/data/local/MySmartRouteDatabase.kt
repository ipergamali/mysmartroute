package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.AuthenticationEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.AuthenticationDao
import com.ioannapergamali.mysmartroute.data.local.UserDao
import com.ioannapergamali.mysmartroute.data.local.VehicleDao
import com.ioannapergamali.mysmartroute.data.local.PoIDao
import com.ioannapergamali.mysmartroute.data.local.SettingsDao

@Database(
    entities = [AuthenticationEntity::class, UserEntity::class, VehicleEntity::class, PoIEntity::class, SettingsEntity::class],
    version = 6
)
abstract class MySmartRouteDatabase : RoomDatabase() {
    abstract fun authenticationDao(): AuthenticationDao
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `settings` ADD COLUMN `font` TEXT NOT NULL DEFAULT 'SansSerif'")
                database.execSQL("ALTER TABLE `settings` ADD COLUMN `soundEnabled` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `settings` ADD COLUMN `soundVolume` REAL NOT NULL DEFAULT 1.0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `authentication` (" +
                        "`uid` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, " +
                        "`password` TEXT NOT NULL, " +
                        "PRIMARY KEY(`uid`)" +
                        ")"
                )
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): MySmartRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MySmartRouteDatabase::class.java,
                    "mysmartroute.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
