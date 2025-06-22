package com.ioannapergamali.mysmartroute.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.data.local.MenuOptionEntity
import com.ioannapergamali.mysmartroute.data.local.MenuWithOptions
import com.ioannapergamali.mysmartroute.data.local.insertMenuSafely
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

                    val userData = mapOf(
                        "id" to authRef,
                        "name" to name,
                        "surname" to surname,
                        "username" to username,
                        "email" to email,
                        "phoneNum" to phoneNum,
                        "password" to password,
                        "role" to role.name,
                        "roleId" to roleRef,
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
                        defaultMenus(context, role).forEach { (menuTitle, options) ->
                            val menuId = UUID.randomUUID().toString()
                            val menuDoc = roleMenusRef.document(menuId)
                            batch.set(menuDoc, mapOf("id" to menuId, "title" to menuTitle))
                            options.forEach { (optTitle, route) ->
                                val optId = UUID.randomUUID().toString()
                                batch.set(
                                    menuDoc.collection("options").document(optId),
                                    mapOf("id" to optId, "title" to optTitle, "route" to route)
                                )
                            }
                        }
                    }

                    batch.commit().await()
                    userDao.insert(userEntity.copy(id = uid, roleId = roleId))
                    _signUpState.value = SignUpState.Success
                    loadCurrentUserRole()
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


    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            if (email.isBlank() || password.isBlank()) {
                _loginState.value = LoginState.Error("Email and password are required")
                return@launch
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    _loginState.value = LoginState.Success
                    loadCurrentUserRole()
                }
                .addOnFailureListener { e ->
                    _loginState.value = LoginState.Error(e.localizedMessage ?: "Login failed")
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

    fun loadCurrentUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val roleName = document.getString("role")
                _currentUserRole.value = roleName?.let { UserRole.valueOf(it) }
            }
    }

    private suspend fun loadMenusWithInheritanceLocal(db: MySmartRouteDatabase, roleId: String): List<MenuWithOptions> {
        val result = mutableListOf<MenuWithOptions>()
        val menuDao = db.menuDao()
        val roleDao = db.roleDao()
        var current: String? = roleId
        while (current != null) {
            result += menuDao.getMenusForRole(current)
            current = roleDao.getRole(current)?.parentRoleId
        }
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
                val menuId = menuDoc.getString("id") ?: menuDoc.id
                val optionsSnap = menuDoc.reference.collection("options").get().await()
                val options = optionsSnap.documents.map { optDoc ->
                    MenuOptionEntity(
                        id = optDoc.getString("id") ?: optDoc.id,
                        menuId = menuId,
                        title = optDoc.getString("title") ?: "",
                        route = optDoc.getString("route") ?: ""
                    )
                }
                insertMenuSafely(menuDao, dbLocal.roleDao(), MenuEntity(menuId, roleId, menuDoc.getString("title") ?: ""))
                options.forEach { optionDao.insert(it) }
                menus += MenuWithOptions(MenuEntity(menuId, roleId, menuDoc.getString("title") ?: ""), options)
            }
            val parent = roleDoc.getString("parentRoleId")
            if (!parent.isNullOrEmpty()) {
                menus += loadMenusWithInheritanceRemote(parent, dbLocal)
            }
        }
        return menus
    }

    fun loadCurrentUserMenus(context: Context) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val dbLocal = MySmartRouteDatabase.getInstance(context)
            val user = dbLocal.userDao().getUser(uid)
            val roleId = user?.roleId.takeIf { !it.isNullOrEmpty() } ?: run {
                val doc = db.collection("users").document(uid).get().await()
                val ref = doc.getDocumentReference("roleId") ?: return@launch
                ref.id
            }

            Log.d(TAG, "Using roleId: $roleId")
            val menusLocal = loadMenusWithInheritanceLocal(dbLocal, roleId)
            if (menusLocal.isNotEmpty()) {
                _currentMenus.value = menusLocal
            } else {
                val menusRemote = loadMenusWithInheritanceRemote(roleId, dbLocal)
                _currentMenus.value = menusRemote
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserRole.value = null
    }

    private suspend fun initializeRolesAndMenusIfNeeded(context: Context) {
        val rolesSnap = db.collection("roles").get().await()
        if (rolesSnap.isEmpty) {
            val json = context.assets.open("menus.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, RoleMenuConfig>>() {}.type
            val map: Map<String, RoleMenuConfig> = gson.fromJson(json, type)
            val batch = db.batch()
            map.forEach { (roleName, cfg) ->
                val role = UserRole.valueOf(roleName)
                val roleId = roleIds[role] ?: "role_${role.name.lowercase()}"
                val roleRef = db.collection("roles").document(roleId)
                val data = mutableMapOf<String, Any>("id" to roleId, "name" to roleName)
                cfg.inheritsFrom?.let {
                    val parentId = roleIds[UserRole.valueOf(it)] ?: "role_${it.lowercase()}"
                    data["parentRoleId"] = parentId
                }
                batch.set(roleRef, data)
                cfg.menus.forEach { menu ->
                    val menuId = UUID.randomUUID().toString()
                    val menuDoc = roleRef.collection("menus").document(menuId)
                    batch.set(menuDoc, mapOf("id" to menuId, "title" to menu.title))
                    menu.options.forEach { opt ->
                        val optId = UUID.randomUUID().toString()
                        batch.set(
                            menuDoc.collection("options").document(optId),
                            mapOf("id" to optId, "title" to opt.title, "route" to opt.route)
                        )
                    }
                }
            }
            batch.commit().await()
        }
    }

    private fun defaultMenus(context: Context, role: UserRole): List<Pair<String, List<Pair<String, String>>>> {
        return try {
            val json = context.assets.open("menus.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, RoleMenuConfig>>() {}.type
            val map: Map<String, RoleMenuConfig> = gson.fromJson(json, type)

            fun collect(roleName: String, acc: MutableList<MenuConfig>) {
                val cfg = map[roleName] ?: return
                cfg.inheritsFrom?.let { collect(it, acc) }
                acc.addAll(cfg.menus)
            }

            val allMenus = mutableListOf<MenuConfig>()
            collect(role.name, allMenus)
            allMenus.map { menu ->
                menu.title to menu.options.map { it.title to it.route }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
