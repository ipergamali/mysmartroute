package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Εμφανίζει ένα ημιδιαφανές πλαίσιο πάνω από το πληκτρολόγιο
 * με το κείμενο που πληκτρολογεί ο χρήστης.
 */
@Composable
fun KeyboardBubble(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding() + 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xAA666666), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(text = text, color = Color.White)
            }
        }
    }
}
