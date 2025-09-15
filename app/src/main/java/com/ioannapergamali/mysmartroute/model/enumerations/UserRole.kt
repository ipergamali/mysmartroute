package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

/**
 * Ελληνικά: Διακριτοί ρόλοι χρηστών στην εφαρμογή.
 * English: Distinct user roles within the app.
 */
enum class UserRole {
    PASSENGER,
    DRIVER,
    ADMIN
}

/** Επιστρέφει το resource id της ονομασίας του ρόλου. / Returns the title resource id for the role. */
@StringRes
fun UserRole.titleRes(): Int = when (this) {
    UserRole.PASSENGER -> R.string.role_passenger
    UserRole.DRIVER -> R.string.role_driver
    UserRole.ADMIN -> R.string.role_admin
}

/** Επιστρέφει το resource id της περιγραφής του ρόλου. / Returns the description resource id for the role. */
@StringRes
fun UserRole.descriptionRes(): Int = when (this) {
    UserRole.PASSENGER -> R.string.role_passenger_description
    UserRole.DRIVER -> R.string.role_driver_description
    UserRole.ADMIN -> R.string.role_admin_description
}

/** Επιστρέφει την τοπικοποιημένη ονομασία του ρόλου. / Returns the localized role name. */
@Composable
fun UserRole.localizedName(): String = stringResource(id = titleRes())

/** Επιστρέφει τον γονικό ρόλο, αν υπάρχει. / Returns the parent role, if any. */
fun UserRole.parent(): UserRole? = when (this) {
    UserRole.PASSENGER -> null
    UserRole.DRIVER -> UserRole.PASSENGER
    UserRole.ADMIN -> UserRole.DRIVER
}

