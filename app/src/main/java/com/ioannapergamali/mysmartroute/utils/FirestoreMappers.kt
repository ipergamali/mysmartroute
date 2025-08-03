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
import com.ioannapergamali.mysmartroute.data.local.RoutePointEntity
import com.ioannapergamali.mysmartroute.data.local.PoiTypeEntity
import com.google.firebase.firestore.DocumentReference
import com.ioannapergamali.mysmartroute.data.local.MovingEntity
import com.ioannapergamali.mysmartroute.data.local.TransportDeclarationEntity
import com.ioannapergamali.mysmartroute.data.local.AvailabilityEntity
import com.ioannapergamali.mysmartroute.data.local.SeatReservationEntity
import com.ioannapergamali.mysmartroute.data.local.FavoriteEntity

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
    "name" to name,
    "description" to description,
    "userId" to FirebaseFirestore.getInstance()
        .collection("users")
        .document(userId),
    "type" to type,
    "seat" to seat,
    "color" to color,
    "plate" to plate
)

fun com.google.firebase.firestore.DocumentSnapshot.toVehicleEntity(): VehicleEntity? {
    val userId = when (val uid = get("userId")) {
        is String -> uid
        is com.google.firebase.firestore.DocumentReference -> uid.id
        else -> return null
    }
    return VehicleEntity(
        id = getString("id") ?: id,
        name = getString("name") ?: "",
        description = getString("description") ?: "",
        userId = userId,
        type = getString("type") ?: "",
        seat = (getLong("seat") ?: 0L).toInt(),
        color = getString("color") ?: "",
        plate = getString("plate") ?: ""
    )
}

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


fun RouteEntity.toFirestoreMap(points: List<RoutePointEntity> = emptyList()): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to userId,
    "name" to name,
    "start" to FirebaseFirestore.getInstance().collection("pois").document(startPoiId),
    "end" to FirebaseFirestore.getInstance().collection("pois").document(endPoiId),
    "points" to points.sortedBy { it.position }.map {
        FirebaseFirestore.getInstance().collection("pois").document(it.poiId)
    }
)

fun DocumentSnapshot.toRouteEntity(): RouteEntity? {
    val routeId = getString("id") ?: id
    val userId = getString("userId") ?: ""
    val routeName = getString("name") ?: ""
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
    return RouteEntity(routeId, userId, routeName, start, end)
}

fun DocumentSnapshot.toRouteWithPoints(): Pair<RouteEntity, List<RoutePointEntity>>? {
    val route = toRouteEntity() ?: return null
    val rawPoints = get("points") as? List<*> ?: emptyList<Any>()
    val points = rawPoints.mapIndexedNotNull { idx, p ->
        val id = when (p) {
            is DocumentReference -> p.id
            is String -> p
            else -> null
        }
        id?.let { RoutePointEntity(route.id, idx, it) }
    }
    return route to points
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

fun MovingEntity.toFirestoreMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
        "id" to id,
        "routeId" to FirebaseFirestore.getInstance().collection("routes").document(routeId),
        "userId" to FirebaseFirestore.getInstance().collection("users").document(userId),
        "date" to date,
        "cost" to cost,
        "durationMinutes" to durationMinutes,
        "startPoiId" to FirebaseFirestore.getInstance().collection("pois").document(startPoiId),
        "endPoiId" to FirebaseFirestore.getInstance().collection("pois").document(endPoiId)
    )
    if (vehicleId.isNotEmpty()) {
        map["vehicleId"] = FirebaseFirestore.getInstance().collection("vehicles").document(vehicleId)
    }
    if (createdById.isNotEmpty()) {
        map["createdById"] = FirebaseFirestore.getInstance().collection("users").document(createdById)
        map["createdByName"] = createdByName
    }
    return map
}

fun DocumentSnapshot.toMovingEntity(): MovingEntity? {
    val movingId = getString("id") ?: id
    val routeId = when (val r = get("routeId")) {
        is DocumentReference -> r.id
        is String -> r
        else -> getString("routeId")
    } ?: return null
    val userId = when (val u = get("userId")) {
        is DocumentReference -> u.id
        is String -> u
        else -> getString("userId")
    } ?: ""
    val vehicleId = when (val v = get("vehicleId")) {
        is DocumentReference -> v.id
        is String -> v
        else -> getString("vehicleId")
    } ?: ""
    val dateVal = getLong("date") ?: 0L
    val costVal = getDouble("cost") ?: 0.0
    val durVal = (getLong("durationMinutes") ?: 0L).toInt()
    val startPoiId = when (val s = get("startPoiId")) {
        is DocumentReference -> s.id
        is String -> s
        else -> getString("startPoiId")
    } ?: ""
    val endPoiId = when (val e = get("endPoiId")) {
        is DocumentReference -> e.id
        is String -> e
        else -> getString("endPoiId")
    } ?: ""
    val createdById = when (val c = get("createdById")) {
        is DocumentReference -> c.id
        is String -> c
        else -> getString("createdById")
    } ?: ""
    val createdByName = getString("createdByName") ?: ""
    return MovingEntity(
        movingId,
        routeId,
        userId,
        dateVal,
        vehicleId,
        costVal,
        durVal,
        startPoiId,
        endPoiId,
        createdById,
        createdByName
    )
}

fun TransportDeclarationEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    // Αποθηκεύουμε απλά τα ids για ευκολότερη αναζήτηση
    "routeId" to routeId,
    "driverId" to driverId,
    "vehicleId" to vehicleId,
    "vehicleType" to vehicleType,
    "cost" to cost,
    "durationMinutes" to durationMinutes,
    "seats" to seats,
    "date" to date
)

fun DocumentSnapshot.toTransportDeclarationEntity(): TransportDeclarationEntity? {
    val declId = getString("id") ?: id
    val routeId = when (val r = get("routeId")) {
        is DocumentReference -> r.id
        is String -> r
        else -> getString("routeId")
    } ?: return null
    val driverId = when (val d = get("driverId")) {
        is DocumentReference -> d.id
        is String -> d
        else -> getString("driverId")
    } ?: ""
    val vehicleId = when (val v = get("vehicleId")) {
        is DocumentReference -> v.id
        is String -> v
        else -> getString("vehicleId")
    } ?: ""
    val type = getString("vehicleType") ?: return null
    val costVal = getDouble("cost") ?: 0.0
    val durVal = (getLong("durationMinutes") ?: 0L).toInt()
    val seatsVal = (getLong("seats") ?: 0L).toInt()
    val dateVal = getLong("date") ?: 0L
    return TransportDeclarationEntity(declId, routeId, driverId, vehicleId, type, costVal, durVal, seatsVal, dateVal)
}

fun AvailabilityEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to FirebaseFirestore.getInstance().collection("users").document(userId),
    "date" to date,
    "fromTime" to fromTime,
    "toTime" to toTime
)

fun DocumentSnapshot.toAvailabilityEntity(): AvailabilityEntity? {
    val availId = getString("id") ?: id
    val userId = when (val u = get("userId")) {
        is DocumentReference -> u.id
        is String -> u
        else -> getString("userId")
    } ?: return null
    val dateVal = getLong("date") ?: 0L
    val fromVal = (getLong("fromTime") ?: 0L).toInt()
    val toVal = (getLong("toTime") ?: 0L).toInt()
    return AvailabilityEntity(availId, userId, dateVal, fromVal, toVal)
}

fun SeatReservationEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "declarationId" to FirebaseFirestore.getInstance().collection("transport_declarations").document(declarationId),
    "routeId" to FirebaseFirestore.getInstance().collection("routes").document(routeId),
    "userId" to FirebaseFirestore.getInstance().collection("users").document(userId),
    "date" to date,
    "startPoiId" to FirebaseFirestore.getInstance().collection("pois").document(startPoiId),
    "endPoiId" to FirebaseFirestore.getInstance().collection("pois").document(endPoiId)
)

fun DocumentSnapshot.toSeatReservationEntity(): SeatReservationEntity? {
    val resId = getString("id") ?: id
    val routeId = when (val r = get("routeId")) {
        is DocumentReference -> r.id
        is String -> r
        else -> getString("routeId")
    } ?: return null
    val declarationId = when (val d = get("declarationId")) {
        is DocumentReference -> d.id
        is String -> d
        else -> getString("declarationId")
    } ?: ""
    val userId = when (val u = get("userId")) {
        is DocumentReference -> u.id
        is String -> u
        else -> getString("userId")
    } ?: return null
    val dateVal = getLong("date") ?: 0L
    val startPoiId = when (val s = get("startPoiId")) {
        is DocumentReference -> s.id
        is String -> s
        else -> getString("startPoiId")
    } ?: ""
    val endPoiId = when (val e = get("endPoiId")) {
        is DocumentReference -> e.id
        is String -> e
        else -> getString("endPoiId")
    } ?: ""
    return SeatReservationEntity(resId, declarationId, routeId, userId, dateVal, startPoiId, endPoiId)
}

fun FavoriteEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "id" to id,
    "userId" to FirebaseFirestore.getInstance().collection("users").document(userId),
    "vehicleType" to vehicleType,
    "preferred" to preferred
)

fun DocumentSnapshot.toFavoriteEntity(): FavoriteEntity? {
    val favId = getString("id") ?: id
    val userId = when (val u = get("userId")) {
        is DocumentReference -> u.id
        is String -> u
        else -> getString("userId")
    } ?: return null
    val type = getString("vehicleType") ?: return null
    val preferred = getBoolean("preferred") ?: false
    return FavoriteEntity(favId, userId, type, preferred)
}
