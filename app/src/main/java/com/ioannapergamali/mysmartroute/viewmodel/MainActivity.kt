package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.ioannapergamali.mysmartroute.model.navigation.NavigationHost



class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
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
