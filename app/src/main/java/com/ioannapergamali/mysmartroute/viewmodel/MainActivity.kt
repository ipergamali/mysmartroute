package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.ioannapergamali.mysmartroute.model.navigation.NavigationHost
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.components.DrawerWrapper
import com.ioannapergamali.mysmartroute.utils.MiuiUtils
import com.ioannapergamali.mysmartroute.utils.ThemePreferenceManager



class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        // Προαιρετικός έλεγχος ύπαρξης του MIUI Service Delivery provider
        MiuiUtils.callServiceDelivery(this, "ping")
        setContent {
            val context = LocalContext.current
            val theme by ThemePreferenceManager.themeFlow(context).collectAsState(initial = AppTheme.Ocean)
            val dark by ThemePreferenceManager.darkThemeFlow(context).collectAsState(initial = false)
            MysmartrouteTheme(theme = theme, darkTheme = dark) {
                val navController = rememberNavController()
                DrawerWrapper(navController = navController) { openDrawer ->
                    NavigationHost(navController = navController, openDrawer = openDrawer)
                }
            }
        }
    }

    override fun onBackPressed() {
        finishAfterTransition()
        super.onBackPressed()
    }
}
