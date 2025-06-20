package com.ioannapergamali.mysmartroute.utils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity

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
    "userId" to userId,
    "type" to type,
    "seat" to seat
)

/** Μετατροπή [SettingsEntity] σε Map. */
fun SettingsEntity.toFirestoreMap(): Map<String, Any> = mapOf(
    "userId" to userId,
    "theme" to theme,
    "darkTheme" to darkTheme,
    "font" to font,
    "soundEnabled" to soundEnabled,
    "soundVolume" to soundVolume
)

/** Μετατροπή εγγράφου Firestore σε [UserEntity]. */
fun DocumentSnapshot.toUserEntity(): UserEntity? {
    val id = getDocumentReference("id")?.id ?: getString("id") ?: return null
    return UserEntity(
        id = id,
        name = getString("name") ?: "",
        surname = getString("surname") ?: "",
        username = getString("username") ?: "",
        email = getString("email") ?: "",
        phoneNum = getString("phoneNum") ?: "",
        password = getString("password") ?: "",
        role = getString("role") ?: "",
        roleId = getDocumentReference("roleId")?.id ?: getString("roleId") ?: "",
        city = getString("city") ?: "",
        streetName = getString("streetName") ?: "",
        streetNum = (getLong("streetNum") ?: 0L).toInt(),
        postalCode = (getLong("postalCode") ?: 0L).toInt()
    )
}
