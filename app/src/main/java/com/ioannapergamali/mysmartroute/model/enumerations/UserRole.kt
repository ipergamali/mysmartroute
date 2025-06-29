package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

enum class UserRole {
    PASSENGER,
    DRIVER,
    ADMIN
}

/** Επιστρέφει το resource id της ονομασίας του ρόλου. */
@StringRes
fun UserRole.titleRes(): Int = when (this) {
    UserRole.PASSENGER -> R.string.role_passenger
    UserRole.DRIVER -> R.string.role_driver
    UserRole.ADMIN -> R.string.role_admin
}

/** Επιστρέφει την τοπικοποιημένη ονομασία του ρόλου. */
@Composable
fun UserRole.localizedName(): String = stringResource(id = titleRes())

/** Επιστρέφει τον γονικό ρόλο, αν υπάρχει. */
fun UserRole.parent(): UserRole? = when (this) {
    UserRole.PASSENGER -> null
    UserRole.DRIVER -> UserRole.PASSENGER
    UserRole.ADMIN -> UserRole.DRIVER
}

