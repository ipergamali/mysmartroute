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

/**
 * Ελληνικά: Μια δυνατότητα ρόλου με τίτλο και περιγραφή.
 * English: A role capability with a title and description resource.
 */
data class RoleCapability(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

/** Επιστρέφει τις διαθέσιμες δυνατότητες του ρόλου. / Returns the capabilities for the given role. */
fun UserRole.capabilities(): List<RoleCapability> = when (this) {
    UserRole.PASSENGER -> passengerCapabilities()
    UserRole.DRIVER -> passengerCapabilities() + driverCapabilities()
    UserRole.ADMIN -> passengerCapabilities() + driverCapabilities() + adminCapabilities()
}

/** Επιστρέφει τοπικοποιημένο τίτλο δυνατότητας. / Returns a localized capability title. */
@Composable
fun RoleCapability.localizedTitle(): String = stringResource(id = titleRes)

/** Επιστρέφει τοπικοποιημένη περιγραφή δυνατότητας. / Returns a localized capability description. */
@Composable
fun RoleCapability.localizedDescription(): String = stringResource(id = descriptionRes)

private fun passengerCapabilities(): List<RoleCapability> = listOf(
    RoleCapability(
        titleRes = R.string.role_passenger_capability_search_title,
        descriptionRes = R.string.role_passenger_capability_search_desc
    ),
    RoleCapability(
        titleRes = R.string.role_passenger_capability_booking_title,
        descriptionRes = R.string.role_passenger_capability_booking_desc
    ),
    RoleCapability(
        titleRes = R.string.role_passenger_capability_profile_title,
        descriptionRes = R.string.role_passenger_capability_profile_desc
    )
)

private fun driverCapabilities(): List<RoleCapability> = listOf(
    RoleCapability(
        titleRes = R.string.role_driver_capability_fleet_title,
        descriptionRes = R.string.role_driver_capability_fleet_desc
    ),
    RoleCapability(
        titleRes = R.string.role_driver_capability_passengers_title,
        descriptionRes = R.string.role_driver_capability_passengers_desc
    ),
    RoleCapability(
        titleRes = R.string.role_driver_capability_routes_title,
        descriptionRes = R.string.role_driver_capability_routes_desc
    )
)

private fun adminCapabilities(): List<RoleCapability> = listOf(
    RoleCapability(
        titleRes = R.string.role_admin_capability_users_title,
        descriptionRes = R.string.role_admin_capability_users_desc
    ),
    RoleCapability(
        titleRes = R.string.role_admin_capability_content_title,
        descriptionRes = R.string.role_admin_capability_content_desc
    ),
    RoleCapability(
        titleRes = R.string.role_admin_capability_system_title,
        descriptionRes = R.string.role_admin_capability_system_desc
    )
)

