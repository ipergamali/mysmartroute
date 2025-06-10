package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoIListScreen(navController: NavController) {
    val viewModel: PoIViewModel = viewModel()
    val pois by viewModel.pois.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadPois(context) }

    Scaffold(topBar = { TopBar(title = "PoIs", navController = navController) }) { padding ->
        Column(Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pois) { poi ->
                    Text(text = "${'$'}{poi.name} (${poi.lat}, ${'$'}{poi.lng})")
                }
            }
        }
    }
}
