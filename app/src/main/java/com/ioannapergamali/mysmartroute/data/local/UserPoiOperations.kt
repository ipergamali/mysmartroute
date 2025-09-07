package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει αποθηκευμένο σημείο μόνο αν υπάρχουν ο χρήστης και το POI.
 * Inserts a saved point only if both user and POI exist.
 */
@Transaction
suspend fun insertUserPoiSafely(
    userPoiDao: UserPoiDao,
    userDao: UserDao,
    poIDao: PoIDao,
    userPoi: UserPoiEntity
) {
    if (userDao.getUser(userPoi.userId) != null && poIDao.findById(userPoi.poiId) != null) {
        userPoiDao.insert(userPoi)
    }
}
