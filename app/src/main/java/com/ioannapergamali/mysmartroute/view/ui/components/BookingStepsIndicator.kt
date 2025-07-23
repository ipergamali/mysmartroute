package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ioannapergamali.mysmartroute.model.enumerations.BookingStep
import com.ioannapergamali.mysmartroute.model.enumerations.localizedName

/**
 * Εμφανίζει τα βήματα της κράτησης με επισήμανση του τρέχοντος.
 */
@Composable
fun BookingStepsIndicator(currentStep: BookingStep) {
    Column {
        BookingStep.ordered.forEach { step ->
            val isCurrent = step == currentStep

        }
    }
}
