package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

/**
 * Βήματα για την κράτηση θέσης.
 */
enum class BookingStep(@StringRes val titleRes: Int, val position: Int) {
    /** Δήλωση Διαδρομής */
    DECLARE_ROUTE(R.string.declare_route, 1),
    /** Επιλογή Διαδρομής */
    SELECT_ROUTE(R.string.select_route, 2),
    /** Προσθήκη σημείου ενδιαφέροντος */
    ADD_POI(R.string.add_poi_option, 3),
    /** Επανασχεδίαση διαδρομής */
    RECALCULATE_ROUTE(R.string.recalculate_route, 4),
    /** Επιλογή ημερομηνίας */
    SELECT_DATE(R.string.select_date, 5),
    /** Εύρεση τώρα */
    RESERVE_SEAT(R.string.find_now, 6);

    companion object {
        /** Βήματα στη σωστή σειρά. */
        val ordered: List<BookingStep> = values().sortedBy { it.position }
    }
}

/** Επιστρέφει το τοπικοποιημένο όνομα του βήματος. */
@Composable
fun BookingStep.localizedName(): String = stringResource(id = titleRes)
