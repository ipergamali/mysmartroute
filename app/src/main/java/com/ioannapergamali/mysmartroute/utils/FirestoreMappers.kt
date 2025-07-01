package com.ioannapergamali.mysmartroute.utils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.TransportAnnouncementEntity

/** Βοηθητικά extensions για μετατροπή οντοτήτων σε δομές κατάλληλες για το Firestore. */
/** Μετατροπή ενός [UserEntity] σε Map. */
fun UserEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to FirebaseFirestore.getInstance()
        .collection("Authedication")
        .document(id),
    "name" to name,
    "surname" to surname,
    "username" to username,
    "email" to email,
    "phoneNum" to phoneNum,
    "password" to password,
    "role" to role,
    "roleId" to roleId,
    "city" to city,
    "streetName" to streetName,
    "streetNum" to streetNum,
    "postalCode" to postalCode
)

/** Μετατροπή [VehicleEntity] σε Map. */
fun VehicleEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "description" to description,
    "userId" to FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId),
    "type" to type,
    "seat" to seat
)

/** Μετατροπή [SettingsEntity] σε Map. */
fun SettingsEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "userId" to FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId),
    "theme" to theme,
    "darkTheme" to darkTheme,
    "font" to font,
    "soundEnabled" to soundEnabled,
    "soundVolume" to soundVolume,
    "language" to language
)

/** Μετατροπή εγγράφου Firestore σε [UserEntity]. */
fun DocumentSnapshot.toUserEntity(): UserEntity? {
    val id = when (val rawId = get("id")) {
        is com.google.firebase.firestore.DocumentReference -> rawId.id
        is String -> rawId
        else -> getString("id")
    } ?: return null
    return UserEntity(
        id = id,
        name = getString("name") ?: "",
        surname = getString("surname") ?: "",
        username = getString("username") ?: "",
        email = getString("email") ?: "",
        phoneNum = getString("phoneNum") ?: "",
        password = getString("password") ?: "",
        role = getString("role") ?: "",
        roleId = when (val rawRole = get("roleId")) {
            is com.google.firebase.firestore.DocumentReference -> rawRole.id
            is String -> rawRole
            else -> getString("roleId")
        } ?: "",
        city = getString("city") ?: "",
        streetName = getString("streetName") ?: "",
        streetNum = (getLong("streetNum") ?: 0L).toInt(),
        postalCode = (getLong("postalCode") ?: 0L).toInt()
    )
}

fun RoleEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "parentRoleId" to (parentRoleId ?: "")
)

fun MenuEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "titleKey" to titleResKey
)

fun MenuOptionEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "titleKey" to titleResKey,
    "route" to route
)

fun TransportAnnouncementEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "driverId" to FirebaseFirestore.getInstance().collection("users").document(driverId),
    "vehicleType" to vehicleType,
    "start" to start,
    "end" to end,
    "date" to date,
    "cost" to cost,
    "durationMinutes" to durationMinutes
)

fun DocumentSnapshot.toTransportAnnouncementEntity(): TransportAnnouncementEntity? {
    val announcementId = getString("id") ?: id
    val driver = when (val raw = get("driverId")) {
        is String -> raw
        is com.google.firebase.firestore.DocumentReference -> raw.id
        else -> null
    } ?: return null
    val routeStart = getString("start") ?: return null
    val routeEnd = getString("end") ?: return null
    val routeId = "" // not stored separately
    return TransportAnnouncementEntity(
        id = announcementId,
        driverId = driver,
        vehicleType = getString("vehicleType") ?: "",
        start = routeStart,
        end = routeEnd,
        date = (getLong("date") ?: 0L).toInt(),
        cost = getDouble("cost") ?: 0.0,
        durationMinutes = (getLong("durationMinutes") ?: 0L).toInt()
    )
}
