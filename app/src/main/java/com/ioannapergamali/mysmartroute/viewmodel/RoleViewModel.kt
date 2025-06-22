package com.ioannapergamali.mysmartroute.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.data.local.RoleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Απλό ViewModel για ανάκτηση των ρόλων από τη βάση.
 */
class RoleViewModel : ViewModel() {
    private val _roles = MutableStateFlow<List<RoleEntity>>(emptyList())
    val roles: StateFlow<List<RoleEntity>> = _roles

    fun loadRoles(context: Context) {
        viewModelScope.launch {
            val db = MySmartRouteDatabase.getInstance(context)
            db.roleDao().getAllRoles().collect { list ->
                _roles.value = list
            }
        }
    }
}
