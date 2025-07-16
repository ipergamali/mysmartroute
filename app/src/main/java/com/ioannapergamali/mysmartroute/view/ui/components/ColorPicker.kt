package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * Απλός picker χρωμάτων με πλέγμα από κουμπιά.
 * @param colors Λίστα διαθέσιμων χρωμάτων
 * @param selectedColor Το επιλεγμένο χρώμα
 * @param onColorSelected Callback όταν επιλεχθεί χρώμα
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { color ->
                Box(
                    Modifier
                        .size(36.dp)
                        .background(color, shape)
                        .border(
                            width = if (color == selectedColor) 2.dp else 1.dp,
                            color = if (color == selectedColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            shape = shape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .size(40.dp)
                .background(selectedColor, shape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface, shape)
        )
    }
}
