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
import com.ioannapergamali.mysmartroute.data.local.LanguageSettingEntity
import com.ioannapergamali.mysmartroute.data.local.LanguageSettingDao
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.RoutePointDao
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationDao
import com.ioannapergamali.mysmartroute.data.local.AvailabilityEntity
import com.ioannapergamali.mysmartroute.data.local.AvailabilityDao
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationDao
import androidx.room.TypeConverters
import com.ioannapergamali.mysmartroute.data.local.Converters

@Database(
    entities = [
        UserEntity::class,
        VehicleEntity::class,
        PoiTypeEntity::class,
        PoIEntity::class,
        SettingsEntity::class,
        RoleEntity::class,
        MenuEntity::class,
        MenuOptionEntity::class,
        LanguageSettingEntity::class,
        RouteEntity::class,
        MovingEntity::class,
        RoutePointEntity::class,
        TransportDeclarationEntity::class,
        AvailabilityEntity::class,
        SeatReservationEntity::class
    ],
    version = 35
)
@TypeConverters(Converters::class)
abstract class MySmartRouteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun poiTypeDao(): PoiTypeDao
    abstract fun poIDao(): PoIDao
    abstract fun settingsDao(): SettingsDao
    abstract fun roleDao(): RoleDao
    abstract fun menuDao(): MenuDao
    abstract fun menuOptionDao(): MenuOptionDao
    abstract fun languageSettingDao(): LanguageSettingDao
    abstract fun routeDao(): RouteDao
    abstract fun movingDao(): MovingDao
    abstract fun routePointDao(): RoutePointDao
    abstract fun transportDeclarationDao(): TransportDeclarationDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun seatReservationDao(): SeatReservationDao

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
                        "`language` TEXT NOT NULL DEFAULT 'el', " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`userId`)" +
                    ")"
                )
                database.execSQL(
                    "INSERT INTO `settings_new` (`userId`, `theme`, `darkTheme`, `font`, `soundEnabled`, `soundVolume`, `language`) " +
                        "SELECT `userId`, `theme`, `darkTheme`, `font`, `soundEnabled`, `soundVolume`, 'el' FROM `settings`"
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
                        "`titleResKey` TEXT NOT NULL, " +
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
                        "`titleResKey` TEXT NOT NULL, " +
                        "`route` TEXT NOT NULL, " +
                        "FOREIGN KEY(`menuId`) REFERENCES `menus`(`id`) ON DELETE CASCADE, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
                database.execSQL("CREATE TABLE IF NOT EXISTS `menus_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`roleId` TEXT NOT NULL, " +
                        "`titleResKey` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")")
                database.execSQL(
                    "INSERT INTO `menus_new` (`id`, `roleId`, `titleResKey`) " +
                        "SELECT `id`, `roleId`, `titleResKey` FROM `menus`"
                )
                database.execSQL("DROP TABLE `menus`")
                database.execSQL("ALTER TABLE `menus_new` RENAME TO `menus`")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                fun insertMenu(id: String, roleId: String, titleResKey: String) {
                    database.execSQL(
                        "INSERT OR IGNORE INTO `menus` (`id`, `roleId`, `titleResKey`) VALUES ('" +
                            id + "', '" + roleId + "', '" + titleResKey + "')"
                    )
                }

                fun insertOption(id: String, menuId: String, titleResKey: String, route: String) {
                    database.execSQL(
                        "INSERT OR IGNORE INTO `menu_options` (`id`, `menuId`, `titleResKey`, `route`) VALUES ('" +
                            id + "', '" + menuId + "', '" + titleResKey + "', '" + route + "')"
                    )
                }

                val passengerMenuId = "menu_passenger_main"
                insertMenu(passengerMenuId, "role_passenger", "passenger_menu_title")
                insertOption("opt_passenger_0", passengerMenuId, "sign_out", "signOut")
                insertOption("opt_passenger_1", passengerMenuId, "manage_favorites", "manageFavorites")
                insertOption("opt_passenger_2", passengerMenuId, "route_mode", "routeMode")
                insertOption("opt_passenger_3", passengerMenuId, "find_vehicle", "findVehicle")
                insertOption("opt_passenger_4", passengerMenuId, "find_way", "findWay")
                insertOption("opt_passenger_5", passengerMenuId, "book_seat", "bookSeat")
                insertOption("opt_passenger_6", passengerMenuId, "view_routes", "viewRoutes")
                insertOption("opt_passenger_7", passengerMenuId, "view_transports", "viewTransports")
                insertOption("opt_passenger_8", passengerMenuId, "print_ticket", "printTicket")
                insertOption("opt_passenger_9", passengerMenuId, "cancel_seat", "cancelSeat")
                insertOption("opt_passenger_10", passengerMenuId, "rank_transports", "rankTransports")
                insertOption("opt_passenger_11", passengerMenuId, "shutdown", "shutdown")

                val driverMenuId = "menu_driver_main"
                insertMenu(driverMenuId, "role_driver", "driver_menu_title")
                insertOption("opt_driver_1", driverMenuId, "register_vehicle", "registerVehicle")
                insertOption("opt_driver_2", driverMenuId, "declare_route", "declareRoute")
                insertOption("opt_driver_3", driverMenuId, "announce_availability", "announceAvailability")
                insertOption("opt_driver_4", driverMenuId, "find_passengers", "findPassengers")
                insertOption("opt_driver_5", driverMenuId, "print_list", "printList")
                insertOption("opt_driver_6", driverMenuId, "print_scheduled", "printScheduled")
                insertOption("opt_driver_7", driverMenuId, "print_completed", "printCompleted")

                val adminMenuId = "menu_admin_main"
                insertMenu(adminMenuId, "role_admin", "admin_menu_title")
                insertOption("opt_admin_1", adminMenuId, "init_system", "initSystem")
                insertOption("opt_admin_2", adminMenuId, "create_user", "createUser")
                insertOption("opt_admin_3", adminMenuId, "edit_privileges", "editPrivileges")
                insertOption("opt_admin_4", adminMenuId, "define_poi", "definePoi")
                insertOption("opt_admin_5", adminMenuId, "define_duration", "defineDuration")
                insertOption("opt_admin_6", adminMenuId, "view_unassigned", "viewUnassigned")
                insertOption("opt_admin_7", adminMenuId, "review_poi", "reviewPoi")
                insertOption("opt_admin_8", adminMenuId, "rank_drivers", "rankDrivers")
                insertOption("opt_admin_9", adminMenuId, "rank_passengers", "rankPassengers")
                insertOption("opt_admin_10", adminMenuId, "view_vehicles", "viewVehicles")
                insertOption("opt_admin_11", adminMenuId, "view_pois", "viewPois")
                insertOption("opt_admin_12", adminMenuId, "view_users", "viewUsers")
                insertOption("opt_admin_13", adminMenuId, "advance_date", "advanceDate")
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

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `settings` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'el'")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_language` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`language` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL("INSERT INTO `app_language` (`id`, `language`) VALUES (1, 'el')")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routes` (" +
                        "`id` TEXT NOT NULL, " +
                        "`startPoiId` TEXT NOT NULL, " +
                        "`endPoiId` TEXT NOT NULL, " +
                        "`cost` REAL NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `movings` (" +
                        "`id` TEXT NOT NULL, " +
                        "`routeId` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`vehicleId` TEXT NOT NULL, " +
                        "`cost` REAL NOT NULL, " +
                        "`durationMinutes` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `country` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `city` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `streetName` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `streetNum` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `pois` ADD COLUMN `postalCode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `poi_types` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL(
                    "INSERT INTO `poi_types` (`id`, `name`) VALUES " +
                        "('HISTORICAL','HISTORICAL')," +
                        "('BUS_STOP','BUS_STOP')," +
                        "('RESTAURANT','RESTAURANT')," +
                        "('PARKING','PARKING')," +
                        "('SHOPPING','SHOPPING')," +
                        "('GENERAL','GENERAL')"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pois_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`country` TEXT NOT NULL, " +
                        "`city` TEXT NOT NULL, " +
                        "`streetName` TEXT NOT NULL, " +
                        "`streetNum` INTEGER NOT NULL, " +
                        "`postalCode` INTEGER NOT NULL, " +
                        "`typeId` TEXT NOT NULL, " +
                        "`lat` REAL NOT NULL, " +
                        "`lng` REAL NOT NULL, " +
                        "FOREIGN KEY(`typeId`) REFERENCES `poi_types`(`id`) ON DELETE RESTRICT, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL(
                    "INSERT INTO `pois_new` (`id`, `name`, `country`, `city`, `streetName`, `streetNum`, `postalCode`, `typeId`, `lat`, `lng`) " +
                        "SELECT `id`, `name`, `country`, `city`, `streetName`, `streetNum`, `postalCode`, `type`, `lat`, `lng` FROM `pois`"
                )
                database.execSQL("DROP TABLE `pois`")
                database.execSQL("ALTER TABLE `pois_new` RENAME TO `pois`")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `route_points` (" +
                        "`routeId` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`poiId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`routeId`, `position`)" +
                        ")"
                )
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routes_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`startPoiId` TEXT NOT NULL, " +
                        "`endPoiId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL(
                    "INSERT INTO `routes_new` (`id`, `startPoiId`, `endPoiId`) " +
                        "SELECT `id`, `startPoiId`, `endPoiId` FROM `routes`"
                )
                database.execSQL("DROP TABLE `routes`")
                database.execSQL("ALTER TABLE `routes_new` RENAME TO `routes`")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routes_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL DEFAULT '', " +
                        "`startPoiId` TEXT NOT NULL, " +
                        "`endPoiId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
                database.execSQL(
                    "INSERT INTO `routes_new` (`id`, `name`, `startPoiId`, `endPoiId`) " +
                        "SELECT `id`, '' as `name`, `startPoiId`, `endPoiId` FROM `routes`"
                )
                database.execSQL("DROP TABLE `routes`")
                database.execSQL("ALTER TABLE `routes_new` RENAME TO `routes`")
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `vehicles` ADD COLUMN `color` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `vehicles` ADD COLUMN `plate` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transport_declarations` (" +
                        "`id` TEXT NOT NULL, " +
                        "`routeId` TEXT NOT NULL, " +
                        "`vehicleType` TEXT NOT NULL, " +
                        "`cost` REAL NOT NULL, " +
                        "`durationMinutes` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `routes` ADD COLUMN `userId` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `vehicles` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `transport_declarations` ADD COLUMN `date` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `availabilities` (" +
                        "`id` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`fromTime` INTEGER NOT NULL, " +
                        "`toTime` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                        ")"
                )
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `transport_declarations` ADD COLUMN `driverId` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `seat_reservations` (" +
                        "`id` TEXT NOT NULL, " +
                        "`routeId` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`)" +
                    ")"
                )
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

            db.execSQL(
                "INSERT INTO poi_types (id, name) VALUES " +
                    "('HISTORICAL','HISTORICAL')," +
                    "('BUS_STOP','BUS_STOP')," +
                    "('RESTAURANT','RESTAURANT')," +
                    "('PARKING','PARKING')," +
                    "('SHOPPING','SHOPPING')," +
                    "('GENERAL','GENERAL')"
            )

            fun insertMenu(id: String, roleId: String, titleResKey: String) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `menus` (`id`, `roleId`, `titleResKey`) VALUES ('" +
                        id + "', '" + roleId + "', '" + titleResKey + "')"
                )
            }

            fun insertOption(id: String, menuId: String, titleResKey: String, route: String) {
                db.execSQL(
                    "INSERT OR IGNORE INTO `menu_options` (`id`, `menuId`, `titleResKey`, `route`) VALUES ('" +
                        id + "', '" + menuId + "', '" + titleResKey + "', '" + route + "')"
                )
            }

            val passengerMenuId = "menu_passenger_main"
            insertMenu(passengerMenuId, "role_passenger", "passenger_menu_title")
            insertOption("opt_passenger_0", passengerMenuId, "sign_out", "signOut")
            insertOption("opt_passenger_1", passengerMenuId, "manage_favorites", "manageFavorites")
            insertOption("opt_passenger_2", passengerMenuId, "route_mode", "routeMode")
            insertOption("opt_passenger_3", passengerMenuId, "find_vehicle", "findVehicle")
            insertOption("opt_passenger_4", passengerMenuId, "find_way", "findWay")
            insertOption("opt_passenger_5", passengerMenuId, "book_seat", "bookSeat")
            insertOption("opt_passenger_6", passengerMenuId, "view_routes", "viewRoutes")
            insertOption("opt_passenger_7", passengerMenuId, "view_transports", "viewTransports")
            insertOption("opt_passenger_8", passengerMenuId, "print_ticket", "printTicket")
            insertOption("opt_passenger_9", passengerMenuId, "cancel_seat", "cancelSeat")
            insertOption("opt_passenger_10", passengerMenuId, "rank_transports", "rankTransports")
            insertOption("opt_passenger_11", passengerMenuId, "shutdown", "shutdown")

            val driverMenuId = "menu_driver_main"
            insertMenu(driverMenuId, "role_driver", "driver_menu_title")
            insertOption("opt_driver_1", driverMenuId, "register_vehicle", "registerVehicle")
            insertOption("opt_driver_2", driverMenuId, "declare_route", "declareRoute")
            insertOption("opt_driver_3", driverMenuId, "announce_availability", "announceAvailability")
            insertOption("opt_driver_4", driverMenuId, "find_passengers", "findPassengers")
            insertOption("opt_driver_5", driverMenuId, "print_list", "printList")
            insertOption("opt_driver_6", driverMenuId, "print_scheduled", "printScheduled")
            insertOption("opt_driver_7", driverMenuId, "print_completed", "printCompleted")

            val adminMenuId = "menu_admin_main"
            insertMenu(adminMenuId, "role_admin", "admin_menu_title")
            insertOption("opt_admin_1", adminMenuId, "init_system", "initSystem")
            insertOption("opt_admin_2", adminMenuId, "create_user", "createUser")
            insertOption("opt_admin_3", adminMenuId, "edit_privileges", "editPrivileges")
            insertOption("opt_admin_4", adminMenuId, "define_poi", "definePoi")
            insertOption("opt_admin_5", adminMenuId, "define_duration", "defineDuration")
            insertOption("opt_admin_6", adminMenuId, "view_unassigned", "viewUnassigned")
            insertOption("opt_admin_7", adminMenuId, "review_poi", "reviewPoi")
            insertOption("opt_admin_8", adminMenuId, "rank_drivers", "rankDrivers")
            insertOption("opt_admin_9", adminMenuId, "rank_passengers", "rankPassengers")
            insertOption("opt_admin_10", adminMenuId, "view_vehicles", "viewVehicles")
            insertOption("opt_admin_11", adminMenuId, "view_pois", "viewPois")
            insertOption("opt_admin_12", adminMenuId, "view_users", "viewUsers")
            insertOption("opt_admin_13", adminMenuId, "advance_date", "advanceDate")

            Log.d(TAG, "Prepopulate complete")
            db.execSQL("INSERT INTO app_language (id, language) VALUES (1, 'el')")
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
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_21_22,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30,
                    MIGRATION_30_31,
                    MIGRATION_31_32,
                    MIGRATION_32_33,
                    MIGRATION_33_34,
                    MIGRATION_34_35
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
