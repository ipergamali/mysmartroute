package com.ioannapergamali.mysmartroute.utils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.data.local.PoiTypeEntity
import com.google.firebase.firestore.DocumentReference

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

fun PoIEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "address" to address.toFirestoreMap(),
    "type" to FirebaseFirestore.getInstance()
        .collection("poi_types")
        .document(type.name),
    "lat" to lat,
    "lng" to lng
)

fun PoiTypeEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name
)

/** Μετατροπή [PoiAddress] σε Map για αποθήκευση ως ενιαίο αντικείμενο. */
fun com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "country" to country,
        "city" to city,
        "streetName" to streetName,
        "streetNum" to streetNum,
        "postalCode" to postalCode
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


fun RouteEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "start" to FirebaseFirestore.getInstance().collection("pois").document(startPoiId),
    "end" to FirebaseFirestore.getInstance().collection("pois").document(endPoiId),
    "cost" to cost
)

fun DocumentSnapshot.toRouteEntity(): RouteEntity? {
    val routeId = getString("id") ?: id
    val start = when (val raw = get("start")) {
        is DocumentReference -> raw.id
        is String -> raw
        else -> getString("start")
    } ?: return null
    val end = when (val raw = get("end")) {
        is DocumentReference -> raw.id
        is String -> raw
        else -> getString("end")
    } ?: return null
    val costVal = getDouble("cost") ?: 0.0
    return RouteEntity(routeId, start, end, costVal)
}

fun DocumentSnapshot.toPoIEntity(): PoIEntity? {
    val poiId = getString("id") ?: id
    val poiName = getString("name") ?: return null
    val addressMap = get("address") as? Map<*, *> ?: return null
    val address = com.ioannapergamali.mysmartroute.model.classes.poi.PoiAddress(
        country = addressMap["country"] as? String ?: "",
        city = addressMap["city"] as? String ?: "",
        streetName = addressMap["streetName"] as? String ?: "",
        streetNum = (addressMap["streetNum"] as? Long)?.toInt() ?: 0,
        postalCode = (addressMap["postalCode"] as? Long)?.toInt() ?: 0
    )
    val typeStr = when (val rawType = get("type")) {
        is String -> rawType
        is DocumentReference -> rawType.id
        else -> getString("type")
    } ?: com.google.android.libraries.places.api.model.Place.Type.ESTABLISHMENT.name
    val type = runCatching {
        enumValueOf<com.google.android.libraries.places.api.model.Place.Type>(typeStr)
    }.getOrElse { com.google.android.libraries.places.api.model.Place.Type.ESTABLISHMENT }
    val latVal = getDouble("lat") ?: 0.0
    val lngVal = getDouble("lng") ?: 0.0
    return PoIEntity(poiId, poiName, address, type, latVal, lngVal)
}


fun DocumentSnapshot.toPoiTypeEntity(): PoiTypeEntity? {
    val typeId = getString("id") ?: id
    val typeName = getString("name") ?: return null
    return PoiTypeEntity(typeId, typeName)
}
