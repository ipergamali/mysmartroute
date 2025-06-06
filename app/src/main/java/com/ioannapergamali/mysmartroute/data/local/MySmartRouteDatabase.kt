package com.ioannapergamali.mysmartroute.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class], version = 1)
abstract class MySmartRouteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MySmartRouteDatabase? = null

        fun getInstance(context: Context): MySmartRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MySmartRouteDatabase::class.java,
                    "mysmartroute.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
