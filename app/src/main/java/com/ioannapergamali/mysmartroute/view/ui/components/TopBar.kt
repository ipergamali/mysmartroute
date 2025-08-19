package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
import com.ioannapergamali.mysmartroute.model.AppLanguage
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioannapergamali.mysmartroute.viewmodel.SettingsViewModel
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.viewmodel.VehicleRequestViewModel
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole

private const val TAG = "TopBar"



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    navController: NavController,
    showMenu: Boolean = false,
    showLogout: Boolean = false,
    showBack: Boolean = true,
    showHomeIcon: Boolean = true,
    showLanguageToggle: Boolean = true,
    showNotifications: Boolean = true,
    onMenuClick: () -> Unit = {},
    onLogout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
    }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel()
    val authViewModel: AuthenticationViewModel = viewModel()
    val requestViewModel: VehicleRequestViewModel = viewModel()
    val currentLanguage by LanguagePreferenceManager.languageFlow(context).collectAsState(initial = AppLanguage.Greek.code)
    val role by authViewModel.currentUserRole.collectAsState()
    val hasUnreadNotifications by requestViewModel.hasUnreadNotifications.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.loadCurrentUserRole(context)
    }
    LaunchedEffect(role) {
        role?.let {
            requestViewModel.loadRequests(context, allUsers = it == UserRole.DRIVER)
        }
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val isLoggedIn = userId != null

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

                if (showHomeIcon) {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
            if (showNotifications && isLoggedIn) {
                IconButton(onClick = { navController.navigate("notifications") }) {
                    BadgedBox(badge = {
                        if (hasUnreadNotifications) {
                            Badge(containerColor = Color.Red)
                        }
                    }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (showLanguageToggle) {
                val nextLanguage = if (currentLanguage == AppLanguage.Greek.code) {
                    AppLanguage.English
                } else {
                    AppLanguage.Greek
                }

                IconButton(onClick = {
                    Log.d(
                        TAG,
                        "Language toggle pressed. Current: $currentLanguage, switching to: ${nextLanguage.code}"
                    )
                    coroutineScope.launch {
                        LanguagePreferenceManager.setLanguage(context, nextLanguage.code)
                        LocaleUtils.updateLocale(context, nextLanguage.code)
                        settingsViewModel.saveCurrentSettings(context)
                        Log.d(TAG, "Locale updated to ${nextLanguage.code}")
                        (context as? android.app.Activity)?.recreate()
                    }
                }) {
                    Text(nextLanguage.flag)
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
