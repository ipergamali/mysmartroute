package com.ioannapergamali.mysmartroute.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.model.classes.users.UserAddress
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthenticationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

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

            val userId = UUID.randomUUID().toString()
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
                    _signUpState.value = SignUpState.Success
                }
                .addOnFailureListener { e ->
                    _signUpState.value = SignUpState.Error(e.localizedMessage ?: "Sign-up failed")
                }
        }
    }

    sealed class SignUpState {
        object Idle : SignUpState()
        object Loading : SignUpState()
        object Success : SignUpState()
        data class Error(val message: String) : SignUpState()
    }
}