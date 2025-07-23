package com.ioannapergamali.mysmartroute.model.enumerations

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R

/**
 * Βήματα για την κράτηση θέσης.

}

/** Επιστρέφει το τοπικοποιημένο όνομα του βήματος. */
@Composable
fun BookingStep.localizedName(): String = stringResource(id = titleRes)
