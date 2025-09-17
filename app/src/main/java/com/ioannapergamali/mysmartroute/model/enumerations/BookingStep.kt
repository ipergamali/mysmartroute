package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

/**
 * Ελληνικά: Βήματα που ακολουθεί ο χρήστης για κράτηση θέσης.
 * English: Steps the user follows to reserve a seat.
 */
enum class BookingStep(@StringRes val titleRes: Int, val position: Int) {
    /** Δήλωση Διαδρομής / Declare Route */
    DECLARE_ROUTE(R.string.declare_route, 1),
    /** Επιλογή Διαδρομής / Select Route */
    SELECT_ROUTE(R.string.select_route, 2),
    /** Προσθήκη σημείου ενδιαφέροντος / Add point of interest */
    ADD_POI(R.string.add_poi_option, 3),
    /** Επανασχεδίαση διαδρομής / Recalculate route */
    RECALCULATE_ROUTE(R.string.recalculate_route, 4),
    /** Επιλογή ημερομηνίας / Select date */
    SELECT_DATE(R.string.select_date, 5),
    /** Επιλογή ώρας / Select time */
    SELECT_TIME(R.string.select_time, 6),
    /** Εύρεση τώρα / Reserve seat */
    RESERVE_SEAT(R.string.find_now, 7);

    companion object {
        /** Βήματα στη σωστή σειρά. / Steps in proper order. */
        val ordered: List<BookingStep> = values().sortedBy { it.position }
    }
}

/** Επιστρέφει το τοπικοποιημένο όνομα του βήματος. / Returns the localized name of the step. */
@Composable
fun BookingStep.localizedName(): String = stringResource(id = titleRes)
