package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.ioannapergamali.mysmartroute.model.navigation.NavigationHost
import com.ioannapergamali.mysmartroute.utils.MiuiUtils



class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        // Προαιρετικός έλεγχος ύπαρξης του MIUI Service Delivery provider
        MiuiUtils.callServiceDelivery(this, "ping")
        setContent {
                val navController = rememberNavController()
                NavigationHost(navController = navController)
        }
    }

    override fun onBackPressed() {
        finishAfterTransition()
        super.onBackPressed()
    }
}
