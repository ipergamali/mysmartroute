package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει ή ενημερώνει έναν χρήστη χωρίς να διαγραφούν τα σχετικά οχήματα.
 */
@Transaction
suspend fun insertUserSafely(
    userDao: UserDao,
    user: UserEntity
) {
    if (userDao.getUser(user.id) == null) {
        userDao.insert(user)
    } else {
        userDao.update(user)
    }
}
