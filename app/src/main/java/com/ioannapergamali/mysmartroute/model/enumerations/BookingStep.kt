package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

/**
 * Βήματα για την κράτηση θέσης.
 */
enum class BookingStep(@StringRes val titleRes: Int) {
    /** Δήλωση Διαδρομής */
    DECLARE_ROUTE(R.string.declare_route),
    /** Επιλογή Διαδρομής */
    SELECT_ROUTE(R.string.select_route),
    /** Προσθήκη σημείου ενδιαφέροντος */
    ADD_POI(R.string.add_poi_option),
    /** Επανασχεδίαση διαδρομής */
    RECALCULATE_ROUTE(R.string.recalculate_route),
    /** Επιλογή ημερομηνίας */
    SELECT_DATE(R.string.select_date),
    /** Κλείσιμο θέσης */
    RESERVE_SEAT(R.string.reserve_seat)
}

/** Επιστρέφει το τοπικοποιημένο όνομα του βήματος. */
@Composable
fun BookingStep.localizedName(): String = stringResource(id = titleRes)
