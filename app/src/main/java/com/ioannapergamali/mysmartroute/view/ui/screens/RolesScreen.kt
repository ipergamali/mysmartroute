package com.ioannapergamali.mysmartroute.view.ui.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RoleViewModel
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.SyncState
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.RoleCapability
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.model.enumerations.capabilities
import com.ioannapergamali.mysmartroute.model.enumerations.localizedDescription
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName
import com.ioannapergamali.mysmartroute.model.enumerations.descriptionRes

import android.widget.Toast
import androidx.compose.ui.unit.dp

@Composable
fun RolesScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: RoleViewModel = viewModel()
    val dbViewModel: DatabaseViewModel = viewModel()
    val roles by viewModel.roles.collectAsState()
    val syncState by dbViewModel.syncState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Error) {
            val message = (syncState as SyncState.Error).message
            Toast.makeText(context,
                context.getString(R.string.roles_load_error, message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        dbViewModel.syncDatabasesSuspend(context)
        viewModel.loadRoles(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.roles_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            when {
                roles.isEmpty() && syncState is SyncState.Loading -> {
                    CircularProgressIndicator()
                }
                roles.isEmpty() && syncState is SyncState.Error -> {
                    val message = (syncState as SyncState.Error).message
                    Text(stringResource(R.string.roles_load_error, message))
                }
                roles.isEmpty() -> {
                    Text(stringResource(R.string.roles_empty))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(roles) { role ->
                            val roleEnum = try {
                                UserRole.valueOf(role.name)
                            } catch (_: Exception) {
                                null
                            }
                            val displayName = roleEnum?.localizedName() ?: role.name

                            val description = roleEnum?.let {
                                stringResource(id = it.descriptionRes())
                            } ?: stringResource(R.string.role_unknown_description, displayName)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "${role.id} - $displayName",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCard(
    roleId: String,
    displayName: String,
    capabilities: List<RoleCapability>?,
    fallbackMessage: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.role_card_header, roleId, displayName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.role_capabilities_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (capabilities.isNullOrEmpty()) {
                Text(
                    text = fallbackMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                capabilities.forEachIndexed { index, capability ->
                    RoleCapabilityItem(capability)
                    if (index < capabilities.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleCapabilityItem(capability: RoleCapability) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = capability.localizedTitle(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = capability.localizedDescription(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
