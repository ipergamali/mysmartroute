package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
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

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: UUID.randomUUID().toString()
                    val userData = hashMapOf(
                        "id" to userId,
                        "name" to name,
                        "surname" to surname,
                        "username" to username,
                        "email" to email,
                        "phoneNum" to phoneNum,
                        "password" to password,
                        "role" to role.name,
                        "address" to mapOf(
                            "city" to address.city,
                            "streetName" to address.streetName,
                            "streetNum" to address.streetNum,
                            "postalCode" to address.postalCode
                        )
                    )

                    db.collection("users")
                        .document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            result.user?.sendEmailVerification()
                            _signUpState.value = SignUpState.Success
                            loadCurrentUserRole()
                        }
                        .addOnFailureListener { e ->
                            _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                        }
                }
                .addOnFailureListener { e ->
                    _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
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
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null && user.isEmailVerified) {
                        _loginState.value = LoginState.Success
                        loadCurrentUserRole()
                    } else {
                        _loginState.value = LoginState.Error("Email not verified")
                    }
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
