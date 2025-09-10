package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ioannapergamali.mysmartroute.model.classes.booking.Booking

/**
 * Προβάλλει τα στοιχεία κράτησης και ειδικά το ονοματεπώνυμο του επιβάτη.
 */
@Composable
fun BookingDetailsScreen(booking: Booking) {
    val passenger = booking.passengerName?.takeIf { it.isNotBlank() } ?: "Δεν δηλώθηκε"

    Text(
        text = "Επιβάτης: $passenger",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}
