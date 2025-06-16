package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction


/**
 * Εισάγει ρυθμίσεις μόνο εφόσον υπάρχει η αντίστοιχη εγγραφή χρήστη.
 * Αν δεν υπάρχει, δημιουργείται πρώτα ένας χρήστης με το δοσμένο id.
 */
@Transaction
suspend fun insertSettingsSafely(
    settingsDao: SettingsDao,
    userDao: UserDao,
    settings: SettingsEntity
) {
    val userId = settings.userId
    if (userDao.getUser(userId) == null) {
        userDao.insert(UserEntity(id = userId))
    }
    settingsDao.insert(settings)
}
