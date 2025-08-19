package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel

@Composable
fun EditPrivilegesScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: UserViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.edit_privileges),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (users.isEmpty()) {
                Text(stringResource(R.string.no_users))
            } else {
                LazyColumn {
                    items(users) { user ->
                        UserRoleRow(user = user) { role ->
                            viewModel.changeUserRole(context, user.id, role, authViewModel)
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRoleRow(user: UserEntity, onRoleChange: (UserRole) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember {
        mutableStateOf(runCatching { UserRole.valueOf(user.role) }.getOrElse { UserRole.PASSENGER })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${user.name} ${user.surname}", modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedRole.localizedName())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                UserRole.values().forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.localizedName()) },
                        onClick = {
                            expanded = false
                            selectedRole = role
                            onRoleChange(role)
                        }
                    )
                }
            }
        }
    }
}
