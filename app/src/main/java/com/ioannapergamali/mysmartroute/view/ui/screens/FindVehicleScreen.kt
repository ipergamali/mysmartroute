package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.R

/**
 * Οθόνη εύρεσης οχήματος βάσει κόστους. Επαναχρησιμοποιεί την RouteModeScreen
 * ώστε να εμφανίζεται ο χάρτης, ο πίνακας με τα POI και τα κουμπιά "Εύρεση τώρα"
 * και "Αποθήκευση αιτήματος" όπως στην αναζήτηση βάσει ημερομηνίας.
 */
@Composable
fun FindVehicleScreen(navController: NavController, openDrawer: () -> Unit) {
    RouteModeScreen(
        navController = navController,
        openDrawer = openDrawer,
        titleRes = R.string.find_vehicle,
        includeCost = true
    )
}
