package com.ioannapergamali.mysmartroute.view

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.MySmartRouteDatabase
import com.ioannapergamali.mysmartroute.utils.ReservationPrinter
import kotlinx.coroutines.launch

/**
 * Απλή δραστηριότητα που εμφανίζει τις κρατήσεις από τη βάση.
 * Simple activity that prints seat reservations from the database.
 */
class TicketActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)

        lifecycleScope.launch {
            val db = MySmartRouteDatabase.getInstance(applicationContext)
            val printer = ReservationPrinter(
                db.seatReservationDao(),
                db.seatReservationDetailDao(),
                db.poIDao()
            )
            val text = printer.buildPrintText()
            findViewById<TextView>(R.id.printTextView).text = text
        }
    }
}
