package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity

@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        PoIEntity::class,
        SettingsEntity::class,
        RoleEntity::class,
        MenuEntity::class,
        MenuOptionEntity::class
    ],
    version = 15
)
abstract class MySmartRouteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun poIDao(): PoIDao
    abstract fun settingsDao(): SettingsDao
    abstract fun roleDao(): RoleDao
    abstract fun menuDao(): MenuDao
    abstract fun menuOptionDao(): MenuOptionDao

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
                    "CREATE TABLE IF NOT EXISTS `vehicles_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`seat` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
                database.execSQL(
                    "INSERT INTO `vehicles_new` (`id`, `description`, `userId`, `type`, `seat`) " +
                        "SELECT `id`, `description`, `userId`, `type`, `seat` FROM `vehicles`"
                )
                database.execSQL("DROP TABLE `vehicles`")
                database.execSQL("ALTER TABLE `vehicles_new` RENAME TO `vehicles`")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settings_new` (" +
                        "`userId` TEXT NOT NULL, " +
                        "`theme` TEXT NOT NULL, " +
                        "`darkTheme` INTEGER NOT NULL, " +
                        "`font` TEXT NOT NULL, " +
                        "`soundEnabled` INTEGER NOT NULL, " +
                        "`soundVolume` REAL NOT NULL, " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`userId`)" +
                    ")"
                )
                database.execSQL(
                    "INSERT INTO `settings_new` (`userId`, `theme`, `darkTheme`, `font`, `soundEnabled`, `soundVolume`) " +
                        "SELECT `userId`, `theme`, `darkTheme`, `font`, `soundEnabled`, `soundVolume` FROM `settings`"
                )
                database.execSQL("DROP TABLE `settings`")
                database.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `roles` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
                database.execSQL("INSERT INTO roles (id, name) VALUES " +
                        "('role_passenger', 'PASSENGER')," +
                        "('role_driver', 'DRIVER')," +
                        "('role_admin', 'ADMIN')")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `menus` (" +
                        "`id` TEXT NOT NULL, " +
                        "`roleId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`route` TEXT NOT NULL, " +
                        "FOREIGN KEY(`roleId`) REFERENCES `roles`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `menu_options` (" +
                        "`id` TEXT NOT NULL, " +
                        "`menuId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`route` TEXT NOT NULL, " +
                        "FOREIGN KEY(`menuId`) REFERENCES `menus`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
                database.execSQL("CREATE TABLE IF NOT EXISTS `menus_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`roleId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")")
                database.execSQL(
                    "INSERT INTO `menus_new` (`id`, `roleId`, `title`) " +
                        "SELECT `id`, `roleId`, `title` FROM `menus`"
                )
                database.execSQL("DROP TABLE `menus`")
                database.execSQL("ALTER TABLE `menus_new` RENAME TO `menus`")
            }
        }

        fun getInstance(context: Context): MySmartRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MySmartRouteDatabase::class.java,
                    "mysmartroute.db"
                ).addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
