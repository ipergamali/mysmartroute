package com.ioannapergamali.mysmartroute.utils

import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.model.enumerations.VehicleType

/**
 * Επιστρέφει `true` εάν η συγκεκριμένη δήλωση μεταφοράς συμβαδίζει με τις προτιμήσεις
 * αγαπημένων ή μη αγαπημένων μέσων μεταφοράς του χρήστη.
 */
fun TransportDeclarationEntity.matchesFavorites(
    preferred: Set<VehicleType>,
    nonPreferred: Set<VehicleType>
): Boolean {
    val type = runCatching { VehicleType.valueOf(vehicleType) }.getOrNull() ?: return true
    if (preferred.isNotEmpty() && !preferred.contains(type)) return false
    if (nonPreferred.contains(type)) return false
    return true
}
