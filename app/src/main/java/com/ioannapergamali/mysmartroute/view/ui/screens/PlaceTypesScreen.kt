package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.utils.PlacesHelper
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceTypesScreen(navController: NavController, openDrawer: () -> Unit) {
    val context = LocalContext.current
    val types = PlacesHelper.samplePlaceTypes()

    Scaffold(topBar = {
        TopBar(title = stringResource(R.string.place_types), navController = navController, showMenu = true, onMenuClick = openDrawer)
    }) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(types) { type ->
                    Text(text = type.name)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { PlacesHelper.logCurrentPlaceTypes(context) }) {
                Text(stringResource(R.string.fetch_nearby_places))
            }
        }
    }
}
