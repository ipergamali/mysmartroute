package com.ioannapergamali.mysmartroute.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.MenuWithOptions
import com.ioannapergamali.mysmartroute.data.local.insertMenuSafely
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.model.classes.users.Admin
import com.ioannapergamali.mysmartroute.model.classes.users.Driver
import com.ioannapergamali.mysmartroute.model.classes.users.Passenger
import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.model.menus.MenuConfig
import com.ioannapergamali.mysmartroute.model.menus.RoleMenuConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.room.withTransaction

class AuthenticationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Χρήση του Gson για μετατροπή JSON σε αντικείμενα Kotlin
    private val gson: Gson = Gson()
    private companion object { const val TAG = "AuthenticationViewModel" }

    /**
     * Παράδειγμα μετατροπής JSON σε αντικείμενο [UserAddress].
     */
    private fun parseUserAddressJson(json: String): UserAddress {
        return gson.fromJson(json, UserAddress::class.java)
    }

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState

    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole

    private val _currentMenus = MutableStateFlow<List<MenuWithOptions>>(emptyList())
    val currentMenus: StateFlow<List<MenuWithOptions>> = _currentMenus

    private val roleIds = mapOf(
        UserRole.PASSENGER to "role_passenger",
        UserRole.DRIVER to "role_driver",
        UserRole.ADMIN to "role_admin"
    )

    fun signUp(
        activity: Activity,
        context: Context,
        name: String,
        surname: String,
        username: String,
        email: String,
        phoneNum: String,
        password: String,
        address: UserAddress,
        role: UserRole
    ) {
        viewModelScope.launch {
            _signUpState.value = SignUpState.Loading

            if (
                name.isBlank() || surname.isBlank() || username.isBlank() ||
                email.isBlank() || phoneNum.isBlank() || password.isBlank() ||
                address.city.isBlank() || address.streetName.isBlank()
            ) {
                _signUpState.value = SignUpState.Error("All fields are required")
                return@launch
            }

            val userIdLocal = UUID.randomUUID().toString()
            val userEntity = UserEntity(
                userIdLocal,
                name,
                surname,
                username,
                email,
                phoneNum,
                password,
                role.name,
                "",
                address.city,
                address.streetName,
                address.streetNum,
                address.postalCode
            )
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val userDao = dbLocal.userDao()

            val user = when (role) {
                UserRole.DRIVER -> Driver(userIdLocal, name, email, surname, address, phoneNum, username, password)
                UserRole.PASSENGER -> Passenger(userIdLocal, name, email, surname, address, phoneNum, username, password)
                UserRole.ADMIN -> Admin(userIdLocal, name, email, surname, address, phoneNum, username, password)
            }

            if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: userIdLocal
                    initializeRolesAndMenusIfNeeded(context)
                    val authRef = db.collection("Authedication").document(uid)
                    val roleId = roleIds[role] ?: "role_passenger"
                    val roleRef = db.collection("roles").document(roleId)

                    // Αποθηκεύουμε το roleId ως απλό String ώστε να είναι πάντα
                    // συμβατό με την ανάγνωση από την εφαρμογή.
                    val userData = mapOf(
                        "id" to authRef,
                        "name" to name,
                        "surname" to surname,
                        "username" to username,
                        "email" to email,
                        "phoneNum" to phoneNum,
                        "password" to password,
                        "role" to role.name,
                        "roleId" to roleId,
                        "city" to address.city,
                        "streetName" to address.streetName,
                        "streetNum" to address.streetNum,
                        "postalCode" to address.postalCode
                    )

                    val batch = db.batch()
                    val userDoc = db.collection("users").document(uid)
                    batch.set(userDoc, userData)

                    val roleMenusRef = roleRef.collection("menus")
                    val snapshot = roleMenusRef.get().await()
                    if (snapshot.isEmpty) {
                        defaultMenus(context, role).forEachIndexed { menuIndex, (menuTitle, options) ->
                            val menuId = "menu_${role.name.lowercase()}_${menuIndex}"
                            val menuDoc = roleMenusRef.document(menuId)
                            batch.set(menuDoc, mapOf("id" to menuId, "titleKey" to menuTitle))
                            options.forEachIndexed { optionIndex, (optTitle, route) ->
                                val base = role.name.lowercase()
                                val optId = "opt_${base}_${menuIndex}_${optionIndex}"
                                batch.set(
                                    menuDoc.collection("options").document(optId),
                                    mapOf("id" to optId, "titleKey" to optTitle, "route" to route)
                                )
                            }
                        }
                    }

                    batch.commit().await()
                    userDao.insert(userEntity.copy(id = uid, roleId = roleId))
                    _signUpState.value = SignUpState.Success
                    loadCurrentUserRole(context, loadMenus = true)
                } catch (e: Exception) {
                    _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                }
            } else {
                val roleId = roleIds[role] ?: "role_passenger"
                userDao.insert(userEntity.copy(roleId = roleId))
                _signUpState.value = SignUpState.Success
            }
        }
    }


    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            if (email.isBlank() || password.isBlank()) {
                _loginState.value = LoginState.Error("Email and password are required")
                return@launch
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    _loginState.value = LoginState.Success
                    loadCurrentUserRole(context, loadMenus = true)
                }
                .addOnFailureListener { e ->
                    _loginState.value = LoginState.Error(e.localizedMessage ?: "Login failed")
                }
        }
    }

    fun resetPassword(email: String, context: Context) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            if (email.isBlank()) {
                val message = context.getString(R.string.email_required)
                Log.w(TAG, message)
                _resetPasswordState.value = ResetPasswordState.Error(message)
                return@launch
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Log.i(TAG, "Password reset email sent to $email")
                    _resetPasswordState.value = ResetPasswordState.Success
                }
                .addOnFailureListener { e ->
                    val message = if (e is FirebaseAuthException) {
                        when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
                            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.error_user_not_found)
                            "ERROR_OPERATION_NOT_ALLOWED" -> context.getString(R.string.error_operation_not_allowed)
                            else -> e.localizedMessage ?: context.getString(R.string.error_send_reset)
                        }
                    } else {
                        e.localizedMessage ?: context.getString(R.string.error_send_reset)
                    }
                    Log.e(TAG, message, e)
                    _resetPasswordState.value = ResetPasswordState.Error(message)
                }
        }
    }

    fun confirmPasswordReset(oobCode: String, newPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading
            auth.verifyPasswordResetCode(oobCode)
                .addOnSuccessListener {
                    auth.confirmPasswordReset(oobCode, newPassword)
                        .addOnSuccessListener {
                            _resetPasswordState.value = ResetPasswordState.Success
                        }
                        .addOnFailureListener { e ->
                            _resetPasswordState.value = ResetPasswordState.Error(
                                e.localizedMessage ?: "Failed to reset password",
                            )
                        }
                }
                .addOnFailureListener { e ->
                    _resetPasswordState.value = ResetPasswordState.Error(
                        e.localizedMessage ?: "Invalid or expired reset link",
                    )
                }
        }
    }

    fun clearResetPasswordState() {
        _resetPasswordState.value = ResetPasswordState.Idle
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser ?: run {
            _changePasswordState.value = ChangePasswordState.Error("Δεν υπάρχει συνδεδεμένος χρήστης")
            return
        }
        val email = user.email ?: run {
            _changePasswordState.value = ChangePasswordState.Error("Το email δεν βρέθηκε")
            return
        }
        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        _changePasswordState.value = ChangePasswordState.Loading
        user.reauthenticate(credential).addOnCompleteListener { reauth ->
            if (reauth.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { update ->
                    _changePasswordState.value =
                        if (update.isSuccessful) ChangePasswordState.Success
                        else ChangePasswordState.Error(update.exception?.localizedMessage ?: "Αποτυχία ενημέρωσης κωδικού")
                }
            } else {
                _changePasswordState.value = ChangePasswordState.Error(
                    reauth.exception?.localizedMessage ?: "Αποτυχία επαλήθευσης ταυτότητας"
                )
            }
        }
    }


    sealed class SignUpState {
        object Idle : SignUpState()
        object Loading : SignUpState()
        object Success : SignUpState()
        data class Error(val message: String) : SignUpState()
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class ResetPasswordState {
        object Idle : ResetPasswordState()
        object Loading : ResetPasswordState()
        object Success : ResetPasswordState()
        data class Error(val message: String) : ResetPasswordState()
    }

    sealed class ChangePasswordState {
        object Idle : ChangePasswordState()
        object Loading : ChangePasswordState()
        object Success : ChangePasswordState()
        data class Error(val message: String) : ChangePasswordState()
    }

    fun loadCurrentUserRole(context: Context, loadMenus: Boolean = false) {
        Log.i(TAG, "loadCurrentUserRole invoked")
        val uid = auth.currentUser?.uid ?: run {
            Log.w(TAG, "No authenticated user")
            return
        }

        viewModelScope.launch {
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val userDao = dbLocal.userDao()

            // Πρώτα προσπαθούμε να πάρουμε τον ρόλο από το Firestore ώστε να είναι πάντα ενημερωμένος
            val document = runCatching {
                db.collection("users").document(uid).get().await()
            }.getOrNull()

            val roleNameRaw = document?.getString("role")
            val roleIdField = when (val field = document?.get("roleId")) {
                is String -> field
                is DocumentReference -> field.id
                else -> null
            }
            val roleName = roleNameRaw ?: roleIdField?.let { id ->
                roleIds.entries.find { it.value == id }?.key?.name
            }
            val roleId = roleIdField ?: ""

            if (roleName != null) {
                Log.i(TAG, "Role from Firestore: $roleName")
                _currentUserRole.value = runCatching { UserRole.valueOf(roleName) }.getOrNull()
                val current = userDao.getUser(uid) ?: UserEntity(id = uid)
                userDao.insert(current.copy(role = roleName, roleId = roleId))
                if (loadMenus) loadCurrentUserMenus(context)
                return@launch
            }

            // Αν αποτύχει η ανάκτηση από το Firestore, πέφτουμε στην τοπική τιμή
            val localRole = userDao.getUser(uid)?.role
            if (!localRole.isNullOrEmpty()) {
                Log.i(TAG, "Role from local DB: $localRole")
                _currentUserRole.value = runCatching { UserRole.valueOf(localRole) }.getOrNull()
                if (loadMenus) loadCurrentUserMenus(context)
            } else {
                _currentUserRole.value = null
            }
        }
    }

    private suspend fun loadMenusWithInheritanceLocal(db: MySmartRouteDatabase, roleId: String): List<MenuWithOptions> {
        val result = mutableListOf<MenuWithOptions>()
        val menuDao = db.menuDao()
        val roleDao = db.roleDao()
        var current: String? = roleId
        while (current != null) {
            val menus = menuDao.getMenusForRole(current)
                .filter { it.menu.id.startsWith("menu_") }
            result += menus
            current = roleDao.getRole(current)?.parentRoleId
        }
        // Επιστρέφουμε όλα τα μενού όπως φορτώθηκαν
        return result
    }

    private suspend fun loadMenusWithInheritanceRemote(roleId: String, dbLocal: MySmartRouteDatabase): List<MenuWithOptions> {
        val roleDoc = db.collection("roles").document(roleId).get().await()
        val menuDao = dbLocal.menuDao()
        val optionDao = dbLocal.menuOptionDao()
        val menus = mutableListOf<MenuWithOptions>()
        if (roleDoc.exists()) {
            val menusSnap = roleDoc.reference.collection("menus").get().await()
            for (menuDoc in menusSnap.documents) {
                val menuIdRaw = menuDoc.getString("id") ?: menuDoc.id
                if (!menuIdRaw.startsWith("menu_")) continue
                val menuId = menuIdRaw
                val optionsSnap = menuDoc.reference.collection("options").get().await()
                val options = optionsSnap.documents.map { optDoc ->
                    val titleKey = optDoc.getString("titleKey")
                        ?: optDoc.getString("titleResKey")
                        ?: ""
                    MenuOptionEntity(
                        id = optDoc.getString("id") ?: optDoc.id,
                        menuId = menuId,
                        titleResKey = titleKey,
                        route = optDoc.getString("route") ?: ""
                    )
                }
                val menuTitleKey = menuDoc.getString("titleKey")
                    ?: menuDoc.getString("titleResKey")
                    ?: ""
                insertMenuSafely(
                    menuDao,
                    dbLocal.roleDao(),
                    MenuEntity(menuId, roleId, menuTitleKey)
                )
                options.forEach { optionDao.insert(it) }
                menus += MenuWithOptions(
                    MenuEntity(menuId, roleId, menuTitleKey),
                    options
                )
            }
            val parent = roleDoc.getString("parentRoleId")
            if (!parent.isNullOrEmpty()) {
                menus += loadMenusWithInheritanceRemote(parent, dbLocal)
            }
        }
        // Επιστρέφουμε όλα τα μενού όπως φορτώθηκαν
        return menus
    }

    /**
     * Φορτώνει τα μενού του τρέχοντος χρήστη από την τοπική βάση.
     * Αν δεν υπάρχουν και υπάρχει σύνδεση στο διαδίκτυο,
     * τα ανακτά από το Firestore. Σε περίπτωση που και εκεί
     * δεν βρεθούν, γίνεται αρχικοποίηση από το τοπικό menus.json.
     */
    fun loadCurrentUserMenus(context: Context) {
        viewModelScope.launch {
            Log.i(TAG, "loadCurrentUserMenus invoked")
            val uid = auth.currentUser?.uid ?: run {
                Log.w(TAG, "No authenticated user")
                return@launch
            }
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val user = dbLocal.userDao().getUser(uid)
            val roleId = user?.roleId?.takeIf { it.isNotEmpty() } ?: run {
                val doc = db.collection("users").document(uid).get().await()
                val remoteRoleId = when (val field = doc.get("roleId")) {
                    is String -> field
                    is DocumentReference -> field.id
                    else -> null
                }
                val fallbackRole = doc.getString("role")?.let {
                    runCatching { UserRole.valueOf(it) }.getOrNull()
                }
                val resolvedRoleId = remoteRoleId ?: fallbackRole?.let { roleIds[it] }
                    ?: return@launch
                user?.let { dbLocal.userDao().insert(it.copy(roleId = resolvedRoleId)) }
                Log.i(TAG, "Fetched roleId from Firestore: $resolvedRoleId")
                resolvedRoleId
            }

            Log.i(TAG, "Using roleId: $roleId")
            var menusLocal = loadMenusWithInheritanceLocal(dbLocal, roleId)
            var hasOptions = menusLocal.all { it.options.isNotEmpty() }
            val currentRole = roleIds.entries.find { it.value == roleId }?.key
            val expectedOptionKeys = currentRole?.let { role ->
                defaultMenus(context, role).flatMap { it.second.map { opt -> opt.first } }
            } ?: emptyList()
            val existingOptionKeys = menusLocal.flatMap { it.options.map { opt -> opt.titleResKey } }
            val missingOptions = expectedOptionKeys.any { it !in existingOptionKeys }
            if (menusLocal.isEmpty() || !hasOptions || missingOptions) {
                // Βεβαιωνόμαστε ότι η τοπική βάση περιέχει τα προεπιλεγμένα μενού
                try {
                    initializeRolesAndMenusIfNeeded(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize menus", e)
                }
                menusLocal = loadMenusWithInheritanceLocal(dbLocal, roleId)
                hasOptions = menusLocal.all { it.options.isNotEmpty() }
            }

            if (menusLocal.isNotEmpty() && hasOptions) {
                _currentMenus.value = menusLocal
            } else if (NetworkUtils.isInternetAvailable(context)) {
                Log.i(TAG, "Loading menus from Firestore")
                val menusRemote = try {
                    loadMenusWithInheritanceRemote(roleId, dbLocal)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load menus", e)
                    emptyList()
                }
                val remoteHasOptions = menusRemote.all { it.options.isNotEmpty() }
                if (menusRemote.isEmpty() || !remoteHasOptions) {
                    Log.i(TAG, "No menus found remotely, initializing defaults")
                    try {
                        initializeRolesAndMenusIfNeeded(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to initialize menus", e)
                    }
                    _currentMenus.value = loadMenusWithInheritanceLocal(dbLocal, roleId)
                } else {
                    _currentMenus.value = menusRemote
                }
            } else {
                Log.w(TAG, "No internet connection and no local menus")
                _currentMenus.value = emptyList()
            }
            Log.i(TAG, "Menus loaded: ${_currentMenus.value.size}")
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserRole.value = null
        _currentMenus.value = emptyList()
    }

    /** Δημόσια μέθοδος που φροντίζει να δημιουργηθούν οι ρόλοι και τα μενού αν χρειάζεται. */
    fun ensureMenusInitialized(context: Context) {
        viewModelScope.launch {
            initializeRolesAndMenusIfNeeded(context)
        }
    }


    private suspend fun initializeRolesAndMenusIfNeeded(context: Context) {
        val json = context.assets.open("menus.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, RoleMenuConfig>>() {}.type
        val map: Map<String, RoleMenuConfig> = gson.fromJson(json, type)

        val dbLocal = MySmartRouteDatabase.getInstance(context)
        val roleDao = dbLocal.roleDao()
        val menuDao = dbLocal.menuDao()
        val optionDao = dbLocal.menuOptionDao()

        val existingRoles = db.collection("roles").get().await().documents.associateBy { it.id }
        val batch = db.batch()
        var commitNeeded = false

        map.forEach { (roleName, cfg) ->
            val role = UserRole.valueOf(roleName)
            val roleId = roleIds[role] ?: "role_${role.name.lowercase()}"
            val roleRef = db.collection("roles").document(roleId)
            val parentId = cfg.inheritsFrom?.let { parent ->
                roleIds[UserRole.valueOf(parent)] ?: "role_${parent.lowercase()}"
            }
            val data = mutableMapOf<String, Any>("id" to roleId, "name" to roleName)
            parentId?.let { data["parentRoleId"] = it }

            val existing = existingRoles[roleId]
            if (existing == null || (parentId != null && existing.getString("parentRoleId") != parentId)) {
                batch.set(roleRef, data)
                commitNeeded = true
            }

            roleDao.insert(RoleEntity(id = roleId, name = roleName, parentRoleId = parentId))

            cfg.menus.forEachIndexed { menuIndex, menuCfg ->
                val menuId = "menu_${role.name.lowercase()}_${menuIndex}"
                val menuRef = roleRef.collection("menus").document(menuId)
                batch.set(menuRef, mapOf("id" to menuId, "titleKey" to menuCfg.titleKey))
                commitNeeded = true
                insertMenuSafely(menuDao, roleDao, MenuEntity(menuId, roleId, menuCfg.titleKey))

                menuCfg.options.forEachIndexed { optIndex, optCfg ->
                    val optId = "opt_${role.name.lowercase()}_${menuIndex}_${optIndex}"
                    batch.set(
                        menuRef.collection("options").document(optId),
                        mapOf("id" to optId, "titleKey" to optCfg.titleKey, "route" to optCfg.route)
                    )
                    commitNeeded = true
                    optionDao.insert(
                        MenuOptionEntity(
                            id = optId,
                            menuId = menuId,
                            titleResKey = optCfg.titleKey,
                            route = optCfg.route
                        )
                    )
                }
            }
        }

        if (commitNeeded) {
            batch.commit().await()
        }
    }

    private fun defaultMenus(context: Context, role: UserRole): List<Pair<String, List<Pair<String, String>>>> {
        val json = context.assets.open("menus.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, RoleMenuConfig>>() {}.type
        val map: Map<String, RoleMenuConfig> = gson.fromJson(json, type)

        val menus = mutableListOf<MenuConfig>()
        var current: String? = role.name
        while (current != null) {
            val cfg = map[current] ?: break
            menus += cfg.menus
            current = cfg.inheritsFrom
        }

        return menus.map { menu ->
            menu.titleKey to menu.options.map { opt -> opt.titleKey to opt.route }
        }
    }
}
