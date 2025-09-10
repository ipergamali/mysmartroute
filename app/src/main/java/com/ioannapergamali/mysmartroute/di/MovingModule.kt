package com.ioannapergamali.mysmartroute.di

import android.content.Context
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.MovingDao
import com.ioannapergamali.mysmartroute.repository.MovingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module που παρέχει τη βάση δεδομένων, το DAO και το repository μετακινήσεων.
 * Hilt module providing the database, DAO and moving repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object MovingModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MySmartRouteDatabase =
        MySmartRouteDatabase.getInstance(context)

    @Provides
    fun provideMovingDao(db: MySmartRouteDatabase): MovingDao = db.movingDao()

    @Provides
    @Singleton
    fun provideMovingRepository(dao: MovingDao): MovingRepository = MovingRepository(dao)
}
