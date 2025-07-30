package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/** Εισάγει αγαπημένο μεταφορικό μέσο εφόσον υπάρχει ο χρήστης. */
@Transaction
suspend fun insertFavoriteSafely(
    favoriteDao: FavoriteDao,
    userDao: UserDao,
    favorite: FavoriteEntity
) {
    if (userDao.getUser(favorite.userId) == null) {
        userDao.insert(UserEntity(id = favorite.userId))
    }
    favoriteDao.insert(favorite)
}
