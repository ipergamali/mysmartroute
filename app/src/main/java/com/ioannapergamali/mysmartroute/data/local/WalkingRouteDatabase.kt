package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WalkingRouteEntity::class], version = 1)
abstract class WalkingRouteDatabase : RoomDatabase() {
    abstract fun dao(): WalkingRouteDao

    companion object {
        @Volatile
        private var INSTANCE: WalkingRouteDatabase? = null

        fun getDatabase(context: Context): WalkingRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WalkingRouteDatabase::class.java,
                    "walking_routes.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

