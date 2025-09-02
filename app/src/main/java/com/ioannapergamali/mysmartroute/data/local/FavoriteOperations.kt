package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/** Εισάγει αγαπημένο μεταφορικό μέσο μόνο αν υπάρχει ήδη ο χρήστης. */
@Transaction
suspend fun insertFavoriteSafely(
    favoriteDao: FavoriteDao,
    userDao: UserDao,
    favorite: FavoriteEntity
) {
    if (userDao.getUser(favorite.userId) != null) {
        favoriteDao.insert(favorite)
    }
}
