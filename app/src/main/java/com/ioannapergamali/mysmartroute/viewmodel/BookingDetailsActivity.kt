package com.ioannapergamali.mysmartroute.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ioannapergamali.mysmartroute.model.classes.booking.Booking
import com.ioannapergamali.mysmartroute.view.ui.AppFont
import com.ioannapergamali.mysmartroute.view.ui.AppTheme
import com.ioannapergamali.mysmartroute.view.ui.MysmartrouteTheme
import com.ioannapergamali.mysmartroute.view.ui.screens.BookingDetailsScreen

/**
 * Απλή δραστηριότητα Jetpack Compose για εμφάνιση στοιχείων κράτησης.
 */
class BookingDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val booking: Booking = intent.getParcelableExtra("booking")
            ?: Booking("", "", "", "", "", 0.0)

        setContent {
            MysmartrouteTheme(
                theme = AppTheme.Ocean,
                darkTheme = false,
                font = AppFont.SansSerif.fontFamily
            ) {
                BookingDetailsScreen(booking = booking)
            }
        }
    }
}
