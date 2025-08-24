package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.ioannapergamali.mysmartroute.R

@Composable
fun DrawerMenu(navController: NavController, closeDrawer: () -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 4.dp
    ) {
        val userState = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
        val username = remember { mutableStateOf<String?>(null) }
        val role = remember { mutableStateOf<String?>(null) }

        DisposableEffect(Unit) {
            val auth = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                userState.value = firebaseAuth.currentUser
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            username.value = doc.getString("username")
                            role.value = doc.getString("role")
                        }
                } else {
                    username.value = null
                }
            }
            auth.addAuthStateListener(listener)
            onDispose { auth.removeAuthStateListener(listener) }
        }

        val user = userState.value

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = username.value ?: if (user != null) user.email ?: "" else stringResource(R.string.guest),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.about_title)) },
            selected = false,
            onClick = {
                navController.navigate("about")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.support_title)) },
            selected = false,
            onClick = {
                navController.navigate("support")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.databases_title)) },
            selected = false,
            onClick = {
                navController.navigate("databaseMenu")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.admin_option)) },
            selected = false,
            onClick = {
                navController.navigate("adminSignup")
                closeDrawer()
            },
            icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
        if (user != null) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.profile)) },
                selected = false,
                onClick = {
                    navController.navigate("profile")
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings)) },
                selected = false,
                onClick = {
                    navController.navigate("settings")
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.menu_title)) },
                selected = false,
                onClick = {
                    navController.navigate("menu")
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )

            if (role.value == "ADMIN") {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.view_pois)) },
                    selected = false,
                    onClick = {
                        navController.navigate("viewPois")
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.logout)) },
                selected = false,
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        } else {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.login)) },
                selected = false,
                onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                    closeDrawer()
                },
                icon = { Icon(Icons.Filled.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
        val activity = (LocalContext.current as? Activity)
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.exit)) },
            selected = false,
            onClick = {
                activity?.finishAffinity()
            },
            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerWrapper(
    navController: NavController,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerMenu(navController) { scope.launch { drawerState.close() } }
        },
        scrimColor = MaterialTheme.colorScheme.scrim
    ) {
        content { scope.launch { drawerState.open() } }
    }
}
