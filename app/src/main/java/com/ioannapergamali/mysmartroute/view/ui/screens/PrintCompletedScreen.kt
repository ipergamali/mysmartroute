package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TransportDeclarationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrintCompletedScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val declarationViewModel: TransportDeclarationViewModel = viewModel()
    val routeViewModel: RouteViewModel = viewModel()
    val declarations by declarationViewModel.completedDeclarations.collectAsState()
    val routes by routeViewModel.routes.collectAsState()

    LaunchedEffect(Unit) {
        val driverId = FirebaseAuth.getInstance().currentUser?.uid
        if (driverId != null) {
            declarationViewModel.loadDeclarations(context, driverId)
            routeViewModel.loadRoutes(context)
        }
    }

    val routeNames = routes.associate { it.id to it.name }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.print_completed),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues), scrollable = false) {
            if (declarations.isEmpty()) {
                Text(text = stringResource(R.string.no_completed_transports))
            } else {
                val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                LazyColumn {
                    items(declarations) { decl ->
                        val routeName = routeNames[decl.routeId] ?: ""
                        val dateText = formatter.format(Date(decl.date))
                        Text("$routeName – $dateText")
                        Divider()
                    }
                }
            }
        }
    }
}

