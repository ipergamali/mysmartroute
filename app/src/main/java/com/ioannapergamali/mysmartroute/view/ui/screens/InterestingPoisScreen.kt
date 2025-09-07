package com.ioannapergamali.mysmartroute.view.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
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
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.FavoritePoisViewModel
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestingPoisScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val poiViewModel: PoIViewModel = viewModel()
    val favViewModel: FavoritePoisViewModel = viewModel()
    val pois by poiViewModel.pois.collectAsState()
    val favorites by favViewModel.favorites.collectAsState()

    LaunchedEffect(Unit) {
        poiViewModel.loadPois(context)
        favViewModel.loadFavorites(context)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.select_pois_screen_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        },
        floatingActionButton = {
            if (pois.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        favViewModel.saveFavorites(context) { success ->
                            val msg = if (success) R.string.favorite_pois_saved else R.string.favorite_pois_save_failed
                            Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (pois.isEmpty()) {
                Text(
                    stringResource(R.string.no_pois),
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(pois) { poi ->
                        val checked = favorites.contains(poi.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { favViewModel.toggleFavorite(poi.id) }
                            )
                            Text(poi.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            Button(
                onClick = {
                    favViewModel.saveFavorites(context) { success ->
                        val msg = if (success) R.string.favorite_pois_saved else R.string.favorite_pois_save_failed
                        Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = pois.isNotEmpty(),
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
