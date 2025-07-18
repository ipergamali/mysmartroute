package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Κράτημα κατάστασης του κειμένου που εμφανίζεται στο bubble.
 */
class KeyboardBubbleState {
    var text by mutableStateOf("")
    var activeFieldId by mutableStateOf<Int?>(null)
}

@Composable
fun rememberKeyboardBubbleState() = remember { KeyboardBubbleState() }

val LocalKeyboardBubbleState = compositionLocalOf<KeyboardBubbleState?> { null }
