package com.ioannapergamali.mysmartroute.data.local

import androidx.room.Transaction

/**
 * Εισάγει μενού μόνο εφόσον υπάρχει ο αντίστοιχος ρόλος.
 * Αν δεν υπάρχει, δημιουργείται placeholder ρόλος με το συγκεκριμένο id.
 */
@Transaction
suspend fun insertMenuSafely(
    menuDao: MenuDao,
    roleDao: RoleDao,
    menu: MenuEntity
) {
    val roleId = menu.roleId
    if (roleDao.getRole(roleId) == null) {
        roleDao.insert(RoleEntity(id = roleId))
    }
    menuDao.insert(menu)
}
