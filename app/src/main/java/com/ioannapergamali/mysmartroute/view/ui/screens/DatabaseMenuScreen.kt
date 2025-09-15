package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.ClearState
import com.ioannapergamali.mysmartroute.viewmodel.DatabaseViewModel
import com.ioannapergamali.mysmartroute.viewmodel.TableToggleState

@Composable
fun DatabaseMenuScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: DatabaseViewModel = viewModel()
    val localTables by viewModel.localTables.collectAsState()
    val firebaseTables by viewModel.firebaseTables.collectAsState()
    val clearState by viewModel.clearState.collectAsState()
    val context = LocalContext.current

    val isClearing = clearState is ClearState.Running
    val hasSelection = localTables.any { it.selected } || firebaseTables.any { it.selected }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.databases_title),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Text(
                text = stringResource(R.string.room_tables),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TableSelectionCard(
                tables = localTables,
                enabled = !isClearing,
                onToggle = { id, checked -> viewModel.setLocalTableSelected(id, checked) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.firebase_tables),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TableSelectionCard(
                tables = firebaseTables,
                enabled = !isClearing,
                onToggle = { id, checked -> viewModel.setFirebaseTableSelected(id, checked) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isClearing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { viewModel.clearSelectedTables(context) },
                enabled = hasSelection && !isClearing
            ) {
                Text(stringResource(R.string.clear_data))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = clearState) {
                is ClearState.Success -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ClearState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun TableSelectionCard(
    tables: List<TableToggleState>,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            tables.forEachIndexed { index, table ->
                TableSelectionRow(
                    table = table,
                    enabled = enabled,
                    onToggle = { checked -> onToggle(table.id, checked) }
                )
                if (index < tables.lastIndex) {
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun TableSelectionRow(
    table: TableToggleState,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = table.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Switch(
            checked = table.selected,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}
