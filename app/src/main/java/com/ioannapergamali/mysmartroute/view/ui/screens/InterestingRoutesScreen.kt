package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save

import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.FloatingActionButton

import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import android.widget.Toast

import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.FavoriteRoutesViewModel
import com.ioannapergamali.mysmartroute.viewmodel.RouteViewModel

/** Οθόνη επιλογής διαδρομών που ενδιαφέρουν τον επιβάτη. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestingRoutesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val routeViewModel: RouteViewModel = viewModel()
    val favViewModel: FavoriteRoutesViewModel = viewModel()
    val routes by routeViewModel.routes.collectAsState()
    val favorites by favViewModel.favorites.collectAsState()

    LaunchedEffect(Unit) {
        routeViewModel.loadRoutes(context, includeAll = true)
        favViewModel.loadFavorites()
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.interesting_routes),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )

        },
        floatingActionButton = {
            if (routes.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        favViewModel.saveFavorites { success ->
                            val msg = if (success) {
                                R.string.favorite_routes_saved
                            } else {
                                R.string.favorite_routes_save_failed
                            }
                            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }

        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (routes.isEmpty()) {
                Text(
                    stringResource(R.string.no_interesting_routes),
                    modifier = Modifier.weight(1f).padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(routes) { route ->
                        val checked = favorites.contains(route.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { favViewModel.toggleFavorite(route.id) }
                            )
                            Text(route.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

            }

            Button(
                onClick = {
                    favViewModel.saveFavorites { success ->
                        val msg = if (success) {
                            R.string.favorite_routes_saved
                        } else {
                            R.string.favorite_routes_save_failed
                        }
                        Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = routes.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save))

            }
        }
    }
}

