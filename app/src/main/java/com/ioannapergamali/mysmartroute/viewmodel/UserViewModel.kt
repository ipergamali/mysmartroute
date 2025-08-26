package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

import kotlinx.coroutines.tasks.await
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.NotificationEntity
import com.ioannapergamali.mysmartroute.data.local.demoteDriverToPassenger
import com.ioannapergamali.mysmartroute.data.local.promotePassengerToDriver
import com.ioannapergamali.mysmartroute.utils.toFirestoreMap
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import java.util.UUID

class UserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users

    private val _drivers = MutableStateFlow<List<UserEntity>>(emptyList())
    val drivers: StateFlow<List<UserEntity>> = _drivers

    fun loadUsers(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).userDao()
            var list = dao.getAllUsers().first()
            _users.value = list
            if (NetworkUtils.isInternetAvailable(context)) {
                val snapshot = runCatching {
                    db.collection("users")
                        .get()
                        .await()
                }.getOrNull()
                if (snapshot != null) {
                    val remote = snapshot.documents.mapNotNull { it.toUserEntity() }
                    remote.forEach { dao.insert(it) }
                    list = (list + remote).distinctBy { it.id }
                    _users.value = list
                }
            }
        }
    }

    fun loadDrivers(context: Context) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).userDao()
            var list = dao.getAllUsers().first().filter { it.role == UserRole.DRIVER.name }
            _drivers.value = list
            if (NetworkUtils.isInternetAvailable(context)) {
                val snapshot = runCatching {
                    db.collection("users")
                        .whereEqualTo("role", UserRole.DRIVER.name)
                        .get()
                        .await()
                }.getOrNull()
                if (snapshot != null) {
                    val remote = snapshot.documents.mapNotNull { it.toUserEntity() }
                    remote.forEach { dao.insert(it) }
                    list = (list + remote).distinctBy { it.id }
                    _drivers.value = list
                }
            }
        }
    }

    suspend fun getUser(context: Context, id: String): UserEntity? {
        val dao = MySmartRouteDatabase.getInstance(context).userDao()
        val local = dao.getUser(id)
        if (local != null) return local
        return if (NetworkUtils.isInternetAvailable(context)) {
            runCatching { db.collection("users").document(id).get().await().toUserEntity() }
                .getOrNull()?.also { dao.insert(it) }
        } else null
    }

    suspend fun getUserName(context: Context, id: String): String {
        val user = getUser(context, id)
        return user?.let { "${it.name} ${it.surname}" } ?: ""
    }

    fun getNotifications(context: Context, userId: String) =
        MySmartRouteDatabase.getInstance(context).notificationDao().getForUser(userId)

    /** Διαγράφει τις ειδοποιήσεις του χρήστη αφού διαβαστούν. */
    fun markNotificationsRead(context: Context, userId: String) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).notificationDao()
            val notifications = dao.getForUser(userId).first()
            notifications.forEach { notif ->
                dao.deleteById(notif.id)
                runCatching {
                    db.collection("notifications").document(notif.id).delete().await()
                }
            }
        }
    }

    fun changeUserRole(
        context: Context,
        userId: String,
        newRole: UserRole,
        authViewModel: AuthenticationViewModel? = null
    ) {
        viewModelScope.launch {
            val dbInstance = MySmartRouteDatabase.getInstance(context)
            val userDao = dbInstance.userDao()
            val user = userDao.getUser(userId) ?: return@launch
            val oldRole = runCatching { UserRole.valueOf(user.role) }.getOrNull() ?: UserRole.PASSENGER
            if (oldRole == newRole) return@launch
            user.role = newRole.name
            val newRoleId = when (newRole) {
                UserRole.PASSENGER -> "role_passenger"
                UserRole.DRIVER -> "role_driver"
                UserRole.ADMIN -> "role_admin"
            }
            user.roleId = newRoleId
            userDao.insert(user)
            runCatching {
                db.collection("users").document(userId)
                    .update("role", newRole.name, "roleId", newRoleId)
                    .await()
            }
            if (oldRole == UserRole.DRIVER && newRole == UserRole.PASSENGER) {
                handleDriverDemotion(dbInstance, userId)
            }
            if (oldRole == UserRole.PASSENGER && newRole == UserRole.DRIVER) {
                handlePassengerPromotion(dbInstance, userId)
            }
            if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                authViewModel?.loadCurrentUserRole(context, loadMenus = true)
            }
        }
    }

    private suspend fun handleDriverDemotion(dbInstance: MySmartRouteDatabase, driverId: String) {
        val seatDao = dbInstance.seatReservationDao()
        val notifDao = dbInstance.notificationDao()
        val firestore = FirebaseFirestore.getInstance()

        // Ανακτούμε όλες τις δηλώσεις μεταφοράς του οδηγού από το Firestore
        val declarations = runCatching {
            firestore.collection("transport_declarations")
                .whereEqualTo("driverId", driverId)
                .get()
                .await()
                .documents
        }.getOrNull() ?: emptyList()

        declarations.forEach { declaration ->
            // διαγράφουμε τις κρατήσεις θέσεων για κάθε δήλωση
            val reservations = seatDao.getReservationsForDeclaration(declaration.id).first()
            reservations.forEach { reservation ->
                seatDao.deleteById(reservation.id)
                runCatching {
                    firestore.collection("seat_reservations")
                        .document(reservation.id)
                        .delete()
                        .await()
                }

                // ενημερώνουμε τον επιβάτη με ειδοποίηση
                val notification = NotificationEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = reservation.userId,
                    message = "Η κράτησή σας ακυρώθηκε λόγω αλλαγής οδηγού.",
                )
                notifDao.insert(notification)
                runCatching {
                    firestore.collection("notifications")
                        .document(notification.id)
                        .set(notification.toFirestoreMap())
                        .await()
                }
            }
        }

        // Καθαρίζουμε τα δεδομένα του οδηγού από Room και Firestore
        demoteDriverToPassenger(dbInstance, firestore, driverId)
    }

    private suspend fun handlePassengerPromotion(dbInstance: MySmartRouteDatabase, userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        promotePassengerToDriver(dbInstance, firestore, userId)
    }


    fun updateUser(context: Context, user: UserEntity, imageUri: Uri?) {
        viewModelScope.launch {
            val dao = MySmartRouteDatabase.getInstance(context).userDao()
            val prev = dao.getUser(user.id)

            var updatedUser = user

            if (imageUri != null) {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference
                    val fileName = "profile_${user.id}_${System.currentTimeMillis()}.jpg"
                    val fileRef = storageRef.child("profile_images/$fileName")

                    Log.d("UserViewModel", "Starting upload for URI: $imageUri")

                    val uploadTask = fileRef.putFile(imageUri)
                    val uploadResult = uploadTask.await()

                    Log.d("UserViewModel", "Upload completed successfully")

                    val downloadUrl = fileRef.downloadUrl.await().toString()

                    updatedUser = user.copy(photoUrl = downloadUrl)
                    Log.d("UserViewModel", "Download URL obtained: $downloadUrl")

                } catch (e: Exception) {
                    Log.e("UserViewModel", "Upload failed: ${e.message}", e)
                    updatedUser = if (prev != null) {
                        user.copy(photoUrl = prev.photoUrl)
                    } else {
                        user.copy(photoUrl = "")
                    }
                }
            } else {
                // Κράτα την παλιά φωτογραφία αν δεν επιλέχθηκε νέα
                updatedUser = if (prev != null) {
                    user.copy(photoUrl = prev.photoUrl)
                } else {
                    user.copy(photoUrl = "")
                }
            }

            Log.d("UserViewModel", "Final user before save - photoUrl: '${updatedUser.photoUrl}'")

            // Αποθήκευση στην τοπική βάση
            dao.insert(updatedUser)

            try {
                // ✅ Πάντα να περιλαμβάνεις το photoUrl στο update
                val updateMap = mapOf(
                    "name" to updatedUser.name,
                    "surname" to updatedUser.surname,
                    "email" to updatedUser.email,
                    "phoneNum" to updatedUser.phoneNum,
                    "city" to updatedUser.city,
                    "streetName" to updatedUser.streetName,
                    "streetNum" to updatedUser.streetNum,
                    "postalCode" to updatedUser.postalCode,
                    "photoUrl" to (updatedUser.photoUrl ?: "")  // ✅ Πάντα συμπεριλάμβανε το photoUrl
                )

                Log.d("UserViewModel", "Updating Firestore with: $updateMap")

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(updatedUser.id)
                    .update(updateMap)
                    .await()

                Log.d("UserViewModel", "Successfully updated Firestore")

            } catch (e: Exception) {
                Log.e("UserViewModel", "Firestore update error: ${e.message}", e)

                // Fallback: Δημιούργησε το document αν δεν υπάρχει
                try {
                    Log.d("UserViewModel", "Document might not exist, trying to create with set()")
                    val completeMap = updatedUser.toFirestoreMap()
                    Log.d("UserViewModel", "Creating document with: $completeMap")

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(updatedUser.id)
                        .set(completeMap, SetOptions.merge())
                        .await()
                    Log.d("UserViewModel", "Document created/merged successfully")
                } catch (setException: Exception) {
                    Log.e("UserViewModel", "Failed to create/merge document: ${setException.message}", setException)
                }
            }
        }
    }}

// Μέσα στην updateUser στο UserViewModel
suspend fun updateUser(context: Context, userEntity: UserEntity, imageUri: Uri?): Boolean {
    return try {
        var entityToSave = userEntity // Ξεκινάμε με το entity που λάβαμε

        if (imageUri != null) {
            // 1. Ανέβασμα εικόνας στο Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            // Δημιουργήστε ένα μοναδικό όνομα αρχείου, π.χ., με UUID ή το όνομα από το URI αν είναι ασφαλές
            val fileName =
                "profile_${userEntity.id}_${UUID.randomUUID()}" // Ή imageUri.lastPathSegment αν είναι αξιόπιστο
            val profileImageRef = storageRef.child("profile_images/${userEntity.id}/$fileName")

            Log.d(
                "UserViewModel",
                "Uploading image from URI: $imageUri to path: ${profileImageRef.path}"
            )

            // Χρήση try-catch ειδικά για το ανέβασμα
            try {
                val uploadTaskSnapshot =
                    profileImageRef.putFile(imageUri).await() // Περιμένουμε το ανέβασμα
                val downloadUrl = uploadTaskSnapshot.storage.downloadUrl.await().toString()
                Log.d(
                    "UserViewModel",
                    "Image uploaded successfully. Download URL: $downloadUrl"
                )
                // Ενημέρωση του entity με το νέο photoUrl
                entityToSave = entityToSave.copy(photoUrl = downloadUrl)
            } catch (storageException: Exception) {
                Log.e(
                    "UserViewModel",
                    "Firebase Storage upload failed: ${storageException.message}",
                    storageException
                )
                // Αποφασίστε αν θέλετε να συνεχίσετε την αποθήκευση των άλλων δεδομένων ή να επιστρέψετε false
                // return false // Αν το ανέβασμα εικόνας είναι κρίσιμο
            }
        } else {
            // Δεν επιλέχθηκε νέα εικόνα. Το entityToSave.photoUrl παραμένει αυτό που ήρθε από την ProfileScreen
            // (το οποίο ήταν userEntity.value?.photoUrl).
            Log.d(
                "UserViewModel",
                "No new image URI provided. Keeping existing photoUrl: ${entityToSave.photoUrl}"
            )
        }

        // 2. Αποθήκευση του (πιθανώς ενημερωμένου) entityToSave στο Firestore
        Log.d(
            "UserViewModel",
            "Saving user to Firestore with photoUrl: ${entityToSave.photoUrl}"
        )
        FirebaseFirestore.getInstance().collection("users")
            .document(entityToSave.id)
            .set(entityToSave) // Χρησιμοποιήστε set(entity, SetOptions.merge()) αν θέλετε να κάνετε merge και όχι overwrite πάντα
            .await()
        Log.d("UserViewModel", "User saved to Firestore successfully.")

        // 3. (Προαιρετικά) Αποθήκευση/Ενημέρωση στην τοπική βάση Room
        // Π.χ. localUserDao.updateUser(entityToSave)
        // Log.d("UserViewModel", "User updated in local Room database.")

        true // Επιτυχής ολοκλήρωση
    } catch (e: Exception) {
        Log.e("UserViewModel", "Error in updateUser: ${e.message}", e)
        false // Σφάλμα κατά τη διαδικασία
    }
}

