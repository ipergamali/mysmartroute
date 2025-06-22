package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import android.util.Log
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
    version = 17
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

        private const val TAG = "MySmartRouteDB"

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
                        "`parentRoleId` TEXT, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
                database.execSQL(
                    "INSERT INTO roles (id, name, parentRoleId) VALUES " +
                        "('role_passenger', 'PASSENGER', NULL)," +
                        "('role_driver', 'DRIVER', 'role_passenger')," +
                        "('role_admin', 'ADMIN', 'role_driver')"
                )
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

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                fun insertMenu(id: String, roleId: String, title: String) {
                    database.execSQL(
                        "INSERT OR IGNORE INTO `menus` (`id`, `roleId`, `title`) VALUES ('" +
                            id + "', '" + roleId + "', '" + title + "')"
                    )
                }

                fun insertOption(id: String, menuId: String, title: String, route: String) {
                    database.execSQL(
                        "INSERT OR IGNORE INTO `menu_options` (`id`, `menuId`, `title`, `route`) VALUES ('" +
                            id + "', '" + menuId + "', '" + title + "', '" + route + "')"
                    )
                }

                val passengerMenuId = "menu_passenger_main"
                insertMenu(passengerMenuId, "role_passenger", "Passenger Menu")
                insertOption("opt_passenger_0", passengerMenuId, "Sign out", "signOut")
                insertOption("opt_passenger_1", passengerMenuId, "Manage Favorite Means of Transport", "manageFavorites")
                insertOption("opt_passenger_2", passengerMenuId, "Mode Of Transportation For A Specific Route", "routeMode")
                insertOption("opt_passenger_3", passengerMenuId, "Find a Vehicle for a specific Transport", "findVehicle")
                insertOption("opt_passenger_4", passengerMenuId, "Find Way of Transport", "findWay")
                insertOption("opt_passenger_5", passengerMenuId, "Book a Seat or Buy a Ticket", "bookSeat")
                insertOption("opt_passenger_6", passengerMenuId, "View Interesting Routes", "viewRoutes")
                insertOption("opt_passenger_7", passengerMenuId, "View Transports", "viewTransports")
                insertOption("opt_passenger_8", passengerMenuId, "Print Booked Seat or Ticket", "printTicket")
                insertOption("opt_passenger_9", passengerMenuId, "Cancel Booked Seat", "cancelSeat")
                insertOption("opt_passenger_10", passengerMenuId, "View, Rank and Comment on Completed Transports", "rankTransports")
                insertOption("opt_passenger_11", passengerMenuId, "Shut Down the System", "shutdown")

                val driverMenuId = "menu_driver_main"
                insertMenu(driverMenuId, "role_driver", "Driver Menu")
                insertOption("opt_driver_1", driverMenuId, "Register Vehicle", "registerVehicle")
                insertOption("opt_driver_2", driverMenuId, "Announce Availability for a specific Transport", "announceAvailability")
                insertOption("opt_driver_3", driverMenuId, "Find Passengers", "findPassengers")
                insertOption("opt_driver_4", driverMenuId, "Print Passenger List", "printList")
                insertOption("opt_driver_5", driverMenuId, "Print Passenger List for Scheduled Transports", "printScheduled")
                insertOption("opt_driver_6", driverMenuId, "Print Passenger List for Completed Transports", "printCompleted")

                val adminMenuId = "menu_admin_main"
                insertMenu(adminMenuId, "role_admin", "Admin Menu")
                insertOption("opt_admin_1", adminMenuId, "Initialize System", "initSystem")
                insertOption("opt_admin_2", adminMenuId, "Create User Account", "createUser")
                insertOption("opt_admin_3", adminMenuId, "Promote or Demote User", "editPrivileges")
                insertOption("opt_admin_4", adminMenuId, "Define Point of Interest", "definePoi")
                insertOption("opt_admin_5", adminMenuId, "Define Duration of Travel by Foot", "defineDuration")
                insertOption("opt_admin_6", adminMenuId, "View List of Unassigned Routes", "viewUnassigned")
                insertOption("opt_admin_7", adminMenuId, "Review Point of Interest Names", "reviewPoi")
                insertOption("opt_admin_8", adminMenuId, "Show 10 Best and Worst Drivers", "rankDrivers")
                insertOption("opt_admin_9", adminMenuId, "View 10 Happiest/Least Happy Passengers", "rankPassengers")
                insertOption("opt_admin_10", adminMenuId, "View Available Vehicles", "viewVehicles")
                insertOption("opt_admin_11", adminMenuId, "View PoIs", "viewPois")
                insertOption("opt_admin_12", adminMenuId, "View Users", "viewUsers")
                insertOption("opt_admin_13", adminMenuId, "Advance Date", "advanceDate")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `roles` ADD COLUMN `parentRoleId` TEXT")
                database.execSQL("UPDATE roles SET parentRoleId = NULL WHERE id = 'role_passenger'")
                database.execSQL("UPDATE roles SET parentRoleId = 'role_passenger' WHERE id = 'role_driver'")
                database.execSQL("UPDATE roles SET parentRoleId = 'role_driver' WHERE id = 'role_admin'")
            }
        }

        private fun prepopulate(db: SupportSQLiteDatabase) {
            Log.d(TAG, "Prepopulating database")
            db.execSQL(
                "INSERT INTO roles (id, name, parentRoleId) VALUES " +
                    "('role_passenger', 'PASSENGER', NULL)," +
                    "('role_driver', 'DRIVER', 'role_passenger')," +
                    "('role_admin', 'ADMIN', 'role_driver')"
            )

            fun insertMenu(id: String, roleId: String, title: String) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `menus` (`id`, `roleId`, `title`) VALUES ('" +
                        id + "', '" + roleId + "', '" + title + "')"
                )
            }

            fun insertOption(id: String, menuId: String, title: String, route: String) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `menu_options` (`id`, `menuId`, `title`, `route`) VALUES ('" +
                        id + "', '" + menuId + "', '" + title + "', '" + route + "')"
                )
            }

            val passengerMenuId = "menu_passenger_main"
            insertMenu(passengerMenuId, "role_passenger", "Passenger Menu")
            insertOption("opt_passenger_0", passengerMenuId, "Sign out", "signOut")
            insertOption("opt_passenger_1", passengerMenuId, "Manage Favorite Means of Transport", "manageFavorites")
            insertOption("opt_passenger_2", passengerMenuId, "Mode Of Transportation For A Specific Route", "routeMode")
            insertOption("opt_passenger_3", passengerMenuId, "Find a Vehicle for a specific Transport", "findVehicle")
            insertOption("opt_passenger_4", passengerMenuId, "Find Way of Transport", "findWay")
            insertOption("opt_passenger_5", passengerMenuId, "Book a Seat or Buy a Ticket", "bookSeat")
            insertOption("opt_passenger_6", passengerMenuId, "View Interesting Routes", "viewRoutes")
            insertOption("opt_passenger_7", passengerMenuId, "View Transports", "viewTransports")
            insertOption("opt_passenger_8", passengerMenuId, "Print Booked Seat or Ticket", "printTicket")
            insertOption("opt_passenger_9", passengerMenuId, "Cancel Booked Seat", "cancelSeat")
            insertOption("opt_passenger_10", passengerMenuId, "View, Rank and Comment on Completed Transports", "rankTransports")
            insertOption("opt_passenger_11", passengerMenuId, "Shut Down the System", "shutdown")

            val driverMenuId = "menu_driver_main"
            insertMenu(driverMenuId, "role_driver", "Driver Menu")
            insertOption("opt_driver_1", driverMenuId, "Register Vehicle", "registerVehicle")
            insertOption("opt_driver_2", driverMenuId, "Announce Availability for a specific Transport", "announceAvailability")
            insertOption("opt_driver_3", driverMenuId, "Find Passengers", "findPassengers")
            insertOption("opt_driver_4", driverMenuId, "Print Passenger List", "printList")
            insertOption("opt_driver_5", driverMenuId, "Print Passenger List for Scheduled Transports", "printScheduled")
            insertOption("opt_driver_6", driverMenuId, "Print Passenger List for Completed Transports", "printCompleted")

            val adminMenuId = "menu_admin_main"
            insertMenu(adminMenuId, "role_admin", "Admin Menu")
            insertOption("opt_admin_1", adminMenuId, "Initialize System", "initSystem")
            insertOption("opt_admin_2", adminMenuId, "Create User Account", "createUser")
            insertOption("opt_admin_3", adminMenuId, "Promote or Demote User", "editPrivileges")
            insertOption("opt_admin_4", adminMenuId, "Define Point of Interest", "definePoi")
            insertOption("opt_admin_5", adminMenuId, "Define Duration of Travel by Foot", "defineDuration")
            insertOption("opt_admin_6", adminMenuId, "View List of Unassigned Routes", "viewUnassigned")
            insertOption("opt_admin_7", adminMenuId, "Review Point of Interest Names", "reviewPoi")
            insertOption("opt_admin_8", adminMenuId, "Show 10 Best and Worst Drivers", "rankDrivers")
            insertOption("opt_admin_9", adminMenuId, "View 10 Happiest/Least Happy Passengers", "rankPassengers")
            insertOption("opt_admin_10", adminMenuId, "View Available Vehicles", "viewVehicles")
            insertOption("opt_admin_11", adminMenuId, "View PoIs", "viewPois")
            insertOption("opt_admin_12", adminMenuId, "View Users", "viewUsers")
            insertOption("opt_admin_13", adminMenuId, "Advance Date", "advanceDate")

            Log.d(TAG, "Prepopulate complete")
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
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            prepopulate(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
