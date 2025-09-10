package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.PoIEntity
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.FavoriteRoutesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel

/**
 * Οθόνη προβολής αγαπημένων διαδρομών και σημείων ενδιαφέροντος.
 */
@Composable
fun ViewTransportsScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val favViewModel: FavoriteRoutesViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val favorites by favViewModel.favorites.collectAsState()

    var favoriteRoutes by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }
    var routePois by remember { mutableStateOf<Map<String, List<PoIEntity>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        favViewModel.loadFavorites(context)
    }

    LaunchedEffect(routes, favorites) {
        val favs = routes.filter { favorites.contains(it.id) }
        favoriteRoutes = favs
        val map = mutableMapOf<String, List<PoIEntity>>()
        favs.forEach { route ->
            map[route.id] = routeViewModel.getRoutePois(context, route.id)
        }
        routePois = map
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.view_transports),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (favoriteRoutes.isEmpty()) {
                Text(stringResource(R.string.no_interesting_routes))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.route),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            stringResource(R.string.stops_header),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Divider()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(favoriteRoutes) { route ->
                            val pois = routePois[route.id].orEmpty()
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(route.name, modifier = Modifier.weight(1f))
                                Text(
                                    pois.joinToString { it.name },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

