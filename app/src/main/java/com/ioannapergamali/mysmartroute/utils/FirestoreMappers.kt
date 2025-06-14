package com.ioannapergamali.mysmartroute.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.SettingsEntity
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.VehicleEntity

/** Βοηθητικά extensions για μετατροπή οντοτήτων σε δομές κατάλληλες για το Firestore. */

/** Επιστρέφει αναφορά εγγράφου του πίνακα authentication για το δοσμένο [id]. */
fun FirebaseFirestore.authRef(id: String): DocumentReference =
    collection("authentication").document(id)

/** Μετατροπή ενός [UserEntity] σε Map όπου το πεδίο id είναι DocumentReference. */
fun UserEntity.toFirestoreMap(db: FirebaseFirestore): Map<String, Any> = mapOf(
    "id" to db.authRef(id),
    "name" to name,
    "surname" to surname,
    "username" to username,
    "email" to email,
    "phoneNum" to phoneNum,
    "password" to password,
    "role" to role,
    "city" to city,
    "streetName" to streetName,
    "streetNum" to streetNum,
    "postalCode" to postalCode
)

/** Μετατροπή [VehicleEntity] με userId ως DocumentReference. */
fun VehicleEntity.toFirestoreMap(db: FirebaseFirestore): Map<String, Any> = mapOf(
    "id" to id,
    "description" to description,
    "userId" to db.authRef(userId),
    "type" to type,
    "seat" to seat
)

/** Μετατροπή [SettingsEntity] με userId ως DocumentReference. */
fun SettingsEntity.toFirestoreMap(db: FirebaseFirestore): Map<String, Any> = mapOf(
    "userId" to db.authRef(userId),
    "theme" to theme,
    "darkTheme" to darkTheme,
    "font" to font,
    "soundEnabled" to soundEnabled,
    "soundVolume" to soundVolume
)
