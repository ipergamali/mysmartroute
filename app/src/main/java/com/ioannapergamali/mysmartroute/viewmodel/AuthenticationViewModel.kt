package com.ioannapergamali.mysmartroute.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import com.ioannapergamali.mysmartroute.data.local.MenuEntity
import com.ioannapergamali.mysmartroute.model.classes.users.Admin
import com.ioannapergamali.mysmartroute.model.classes.users.Driver
import com.ioannapergamali.mysmartroute.model.classes.users.Passenger
import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthenticationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole

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
                        val roleId = UUID.randomUUID().toString()
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

                        val roleData = mapOf(
                            "id" to roleId,
                            "userId" to authRef,
                            "name" to role.name
                        )

                        val menuDefaults = when (role) {
                            UserRole.ADMIN -> listOf("Users" to "adminUsers")
                            UserRole.DRIVER -> listOf("Routes" to "driverRoutes")
                            UserRole.PASSENGER -> listOf("Home" to "passengerHome")
                        }

                        val batch = db.batch()
                        val userDoc = db.collection("users").document(uid)
                        batch.set(userDoc, userData)
                        batch.set(roleRef, roleData)

                        menuDefaults.forEach { (title, route) ->
                            val menuId = UUID.randomUUID().toString()
                            val menuDoc = db.collection("menus").document(menuId)
                            val menuData = mapOf(
                                "id" to menuId,
                                "roleId" to roleRef,
                                "title" to title,
                                "route" to route
                            )
                            batch.set(menuDoc, menuData)
                        }

                        batch.commit().addOnSuccessListener {
                            viewModelScope.launch {
                                val roleEntity = RoleEntity(roleId, uid, role.name)
                                userDao.insert(userEntity.copy(id = uid, roleId = roleId))
                                dbLocal.roleDao().insert(roleEntity)
                                val menuDao = dbLocal.menuDao()
                                menuDefaults.forEach { (title, route) ->
                                    val menuId = UUID.randomUUID().toString()
                                    menuDao.insert(MenuEntity(menuId, roleId, title, route))
                                }
                            }
                            _signUpState.value = SignUpState.Success
                            loadCurrentUserRole()
                        }.addOnFailureListener { e ->
                            _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                        }
                    }
                    .addOnFailureListener { e ->
                        _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                    }
            } else {
                val roleId = UUID.randomUUID().toString()
                val roleEntity = RoleEntity(roleId, userIdLocal, role.name)
                val menuDao = dbLocal.menuDao()
                userDao.insert(userEntity.copy(roleId = roleId))
                dbLocal.roleDao().insert(roleEntity)
                val defaults = when (role) {
                    UserRole.ADMIN -> listOf("Users" to "adminUsers")
                    UserRole.DRIVER -> listOf("Routes" to "driverRoutes")
                    UserRole.PASSENGER -> listOf("Home" to "passengerHome")
                }
                defaults.forEach { (title, route) ->
                    val id = UUID.randomUUID().toString()
                    menuDao.insert(MenuEntity(id, roleId, title, route))
                }
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

    fun signOut() {
        auth.signOut()
        _currentUserRole.value = null
    }
}
