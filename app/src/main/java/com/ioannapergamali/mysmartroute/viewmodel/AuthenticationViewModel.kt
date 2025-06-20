package com.ioannapergamali.mysmartroute.viewmodel

import android.app.Activity
import android.content.Context
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
import com.ioannapergamali.mysmartroute.model.classes.users.Admin
import com.ioannapergamali.mysmartroute.model.classes.users.Driver
import com.ioannapergamali.mysmartroute.model.classes.users.Passenger
import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.model.menus.MenuConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AuthenticationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // Χρήση του Gson για μετατροπή JSON σε αντικείμενα Kotlin
    private val gson = Gson()

    /**
     * Παράδειγμα μετατροπής JSON σε αντικείμενο [UserAddress].
     */
    private fun parseUserAddressJson(json: String): UserAddress {
        val type = object : TypeToken<UserAddress>() {}.type
        return gson.fromJson(json, type)
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
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: userIdLocal
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
                        roleMenusRef.get().addOnSuccessListener { snapshot ->
                            if (snapshot.isEmpty) {
                                defaultMenus(context, role).forEach { (menuTitle, options) ->
                                    val menuId = UUID.randomUUID().toString()
                                    val menuDoc = roleMenusRef.document(menuId)
                                    batch.set(menuDoc, mapOf("id" to menuId, "title" to menuTitle))
                                    options.forEach { (optTitle, route) ->
                                        val optId = UUID.randomUUID().toString()
                                        batch.set(menuDoc.collection("options").document(optId),
                                            mapOf("id" to optId, "title" to optTitle, "route" to route))
                                    }
                                }
                            }

                            batch.commit().addOnSuccessListener {
                                viewModelScope.launch {
                                    userDao.insert(userEntity.copy(id = uid, roleId = roleId))
                                }
                                _signUpState.value = SignUpState.Success
                                loadCurrentUserRole()
                            }.addOnFailureListener { e ->
                                _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                            }
                        }.addOnFailureListener { e ->
                            _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                        }
                    }
                    .addOnFailureListener { e ->
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

            val menusLocal = dbLocal.menuDao().getMenusForRole(roleId)
            if (menusLocal.isNotEmpty()) {
                _currentMenus.value = menusLocal
            } else {
                val roleRef = db.collection("roles").document(roleId)
                val snapshot = roleRef.collection("menus").get().await()
                val menuDao = dbLocal.menuDao()
                val optionDao = dbLocal.menuOptionDao()
                val menusRemote = snapshot.documents.map { doc ->
                    val menuId = doc.getString("id") ?: doc.id
                    val optionsSnap = roleRef.collection("menus").document(menuId)
                        .collection("options").get().await()
                    val options = optionsSnap.documents.map { optDoc ->
                        MenuOptionEntity(
                            id = optDoc.getString("id") ?: optDoc.id,
                            menuId = menuId,
                            title = optDoc.getString("title") ?: "",
                            route = optDoc.getString("route") ?: ""
                        )
                    }
                    menuDao.insert(MenuEntity(menuId, roleId, doc.getString("title") ?: ""))
                    options.forEach { optionDao.insert(it) }
                    MenuWithOptions(MenuEntity(menuId, roleId, doc.getString("title") ?: ""), options)
                }
                _currentMenus.value = menusRemote
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUserRole.value = null
    }

    private fun defaultMenus(context: Context, role: UserRole): List<Pair<String, List<Pair<String, String>>>> {
        return try {
            val json = context.assets.open("menus.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, List<MenuConfig>>>() {}.type
            // Παράδειγμα χρήσης του Gson για ανάγνωση του menus.json
            val map: Map<String, List<MenuConfig>> = gson.fromJson(json, type)
            val roleMenus = map[role.name].orEmpty()
            roleMenus.map { menu ->
                menu.title to menu.options.map { it.title to it.route }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
