package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.PoIViewModel
import com.ioannapergamali.mysmartroute.data.local.PoIEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteEditorScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: PoIViewModel = viewModel()
    val availablePois by viewModel.pois.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadPois(context) }

    val routePois = remember { mutableStateListOf<PoIEntity>() }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopBar(
            title = stringResource(R.string.route_editor),
            navController = navController,
            showMenu = true,
            onMenuClick = openDrawer
        )
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            routePois.forEachIndexed { index, poi ->
                Text(text = "${index + 1}. ${poi.name}")
            }
            Spacer(Modifier.height(16.dp))
            Box {
                Button(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_stop))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    availablePois.forEach { poi ->
                        DropdownMenuItem(
                            text = { Text(poi.name) },
                            onClick = {
                                if (routePois.lastOrNull()?.id != poi.id) {
                                    routePois.add(poi)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.poi_already_last),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
