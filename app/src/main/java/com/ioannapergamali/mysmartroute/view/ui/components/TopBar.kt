package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navController: NavController,
    showMenu: Boolean = false,
    showLogout: Boolean = false,
    showBack: Boolean = true,
    onMenuClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val username = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    username.value = doc.getString("username")
                }
        }
    }

    Box(modifier = Modifier.statusBarsPadding()) {
        TopAppBar(
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary),
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
        title = { Text(title) },
        navigationIcon = {
            Row {
                IconButton(onClick = {
                    if (showMenu) onMenuClick() else navController.popBackStack()
                }) {
                    Icon(
                        if (showMenu) Icons.Filled.Menu else Icons.AutoMirrored.Filled.List,
                        contentDescription = if (showMenu) "menu" else "list",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }) {
                    Icon(Icons.Filled.Home, contentDescription = "home", tint = MaterialTheme.colorScheme.primary)
                }

                if (showBack) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            username.value?.let { Text(it, modifier = Modifier.padding(end = 8.dp)) }
            if (FirebaseAuth.getInstance().currentUser != null) {
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (showLogout) {
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = "logout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        )
    }
}
