package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            Text(
                text = step.localizedName(),
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                style = if (isCurrent) MaterialTheme.typography.bodyLarge
                else MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
