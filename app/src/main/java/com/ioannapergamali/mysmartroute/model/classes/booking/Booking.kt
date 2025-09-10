package com.ioannapergamali.mysmartroute.model.classes.booking

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
/**
 * Δεδομένα μιας κράτησης για απλή προβολή στοιχείων.
 */
data class Booking(
    val date: String,
    val route: String,
    val startPoint: String,
    val destination: String,
    val driver: String,
    val cost: Double,
    val passengerName: String? = null
) : Parcelable
