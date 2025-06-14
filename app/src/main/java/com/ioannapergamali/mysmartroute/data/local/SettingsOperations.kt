package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

import com.ioannapergamali.mysmartroute.data.local.AuthenticationDao
import com.ioannapergamali.mysmartroute.data.local.AuthenticationEntity

/**
 * Εισάγει ρυθμίσεις μόνο εφόσον υπάρχει η αντίστοιχη εγγραφή χρήστη.
 * Αν δεν υπάρχει, δημιουργείται πρώτα ένας χρήστης με το δοσμένο id.
 */
@Transaction
suspend fun insertSettingsSafely(
    settingsDao: SettingsDao,
    authDao: AuthenticationDao,
    userDao: UserDao,
    settings: SettingsEntity
) {
    val userId = settings.userId
    if (authDao.getAuth(userId) == null) {
        authDao.insert(AuthenticationEntity(id = userId))
    }
    if (userDao.getUser(userId) == null) {
        userDao.insert(UserEntity(id = userId))
    }
    settingsDao.insert(settings)
}
