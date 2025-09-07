// Βοηθητικές λειτουργίες για ρυθμίσεις.
// Helper operations for settings.
package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction


/**
 * Εισάγει ρυθμίσεις μόνο αν υπάρχει ήδη ο χρήστης.
 */
@Transaction
suspend fun insertSettingsSafely(
    settingsDao: SettingsDao,
    userDao: UserDao,
    settings: SettingsEntity
) {
    if (userDao.getUser(settings.userId) != null) {
        settingsDao.insert(settings)
    }
}
