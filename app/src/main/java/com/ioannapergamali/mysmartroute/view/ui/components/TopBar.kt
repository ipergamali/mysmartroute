package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Home
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import com.ioannapergamali.mysmartroute.utils.LanguagePreferenceManager
import com.ioannapergamali.mysmartroute.utils.LocaleUtils
import com.ioannapergamali.mysmartroute.model.AppLanguage
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

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
    val currentLanguage by LanguagePreferenceManager.languageFlow(context).collectAsState(initial = AppLanguage.Greek.code)

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
            if (showLanguageToggle) {
                IconButton(onClick = {
                    val newLang = if (currentLanguage == AppLanguage.Greek.code) AppLanguage.English.code else AppLanguage.Greek.code
                    Log.d(TAG, "Language toggle pressed. Current: $currentLanguage, switching to: $newLang")
                    coroutineScope.launch {
                        LanguagePreferenceManager.setLanguage(context, newLang)
                        LocaleUtils.updateLocale(context, newLang)
                        Log.d(TAG, "Locale updated to $newLang")
                        (context as? android.app.Activity)?.recreate()
                    }
                }) {
                    Text(AppLanguage.values().first { it.code == currentLanguage }.flag)
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
