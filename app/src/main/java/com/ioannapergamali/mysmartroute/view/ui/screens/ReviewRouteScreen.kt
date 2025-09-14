package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.RouteEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.AdminRouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRouteScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AdminRouteViewModel = viewModel(factory = AdminRouteViewModel.Factory(context))
    val duplicateGroups by viewModel.duplicateRoutes.collectAsState()

    Scaffold(topBar = { TopBar(title = stringResource(R.string.review_route), navController = navController, showMenu = true, onMenuClick = openDrawer) }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding), scrollable = false) {
            if (duplicateGroups.isEmpty()) {
                Text(text = stringResource(R.string.no_duplicate_routes), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(duplicateGroups) { group ->
                        var selected by remember { mutableStateOf<RouteEntity?>(null) }
                        var edited by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            group.forEach { route ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    RadioButton(
                                        selected = selected?.id == route.id,
                                        onClick = {
                                            selected = route
                                            edited = route.name
                                        }
                                    )
                                    if (selected?.id == route.id) {
                                        TextField(
                                            value = edited,
                                            onValueChange = { edited = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(text = route.name)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    selected?.let { keep ->
                                        val updated = keep.copy(name = edited)
                                        viewModel.updateRoute(updated)
                                        group.filter { it.id != updated.id }.forEach { other ->
                                            viewModel.mergeRoutes(updated.id, other.id)
                                        }
                                    }
                                    selected = null
                                },
                                enabled = selected != null,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.keep))
                            }
                        }
                    }
                }
            }
        }
    }
}
