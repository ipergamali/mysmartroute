package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.utils.NetworkUtils
import com.ioannapergamali.mysmartroute.utils.toUserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _drivers = MutableStateFlow<List<UserEntity>>(emptyList())
    val drivers: StateFlow<List<UserEntity>> = _drivers

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
}
