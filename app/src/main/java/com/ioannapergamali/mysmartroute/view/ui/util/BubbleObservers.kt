package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

/**
 * Συνδέει ένα TextField με την κατάσταση του bubble ώστε να
 * εμφανίζεται το κείμενό του όταν έχει focus.
 */
fun Modifier.observeBubble(state: KeyboardBubbleState, fieldId: Int, currentValue: () -> String): Modifier = composed {
    this.onFocusChanged { focusState ->
        if (focusState.isFocused) {
            state.activeFieldId = fieldId
            state.text = currentValue()
        } else if (state.activeFieldId == fieldId) {
            state.activeFieldId = null
        }
    }
}
