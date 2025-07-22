package com.ioannapergamali.mysmartroute.view.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Color picker με κυκλική διάταξη που καλύπτει όλο το φάσμα αποχρώσεων.
 * @param selectedColor Το επιλεγμένο χρώμα
 * @param onColorSelected Callback όταν γίνει επιλογή
 * @param modifier Modifier διάταξης
 * @param shades Πλήθος αποχρώσεων που θα εμφανιστούν
 */
@Composable
fun ColorWheelPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    shades: Int = 60
) {
    val colors = remember(shades) { generateColorPalette(shades) }
    BoxWithConstraints(modifier) {
        val size = if (maxWidth < maxHeight) maxWidth else maxHeight
        val circleRadiusPx = with(LocalDensity.current) { (size / 2).toPx() }
        val itemRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
        val borderColor = MaterialTheme.colorScheme.onSurface
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(colors) {
                    detectTapGestures { offset ->
                        val center = Offset(circleRadiusPx, circleRadiusPx)
                        val angle = ((atan2(offset.y - center.y, offset.x - center.x) + 2 * PI) % (2 * PI))
                        val index = ((angle / (2 * PI)) * colors.size).roundToInt() % colors.size
                        onColorSelected(colors[index])
                    }
                }
        ) {
            val center = Offset(circleRadiusPx, circleRadiusPx)
            colors.forEachIndexed { index, color ->
                val angle = 2 * PI * index / colors.size
                val radius = circleRadiusPx - itemRadiusPx - 4.dp.toPx()
                val x = center.x + cos(angle).toFloat() * radius
                val y = center.y + sin(angle).toFloat() * radius
                drawCircle(
                    color = color,
                    radius = itemRadiusPx,
                    center = Offset(x, y)
                )
                if (color == selectedColor) {
                    drawCircle(
                        color = borderColor,
                        radius = itemRadiusPx + 4.dp.toPx(),
                        center = Offset(x, y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}

/** Δημιουργεί παλέτα χρωμάτων καλύπτοντας όλο το φάσμα αποχρώσεων */
fun generateColorPalette(shades: Int): List<Color> {
    return List(shades) { index ->
        val hue = index * 360f / shades
        val hsv = floatArrayOf(hue, 1f, 1f)
        Color(AndroidColor.HSVToColor(hsv))
    }
}

/** Επιστρέφει την HEX μορφή ενός χρώματος */
fun Color.toHex(): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/** Επιστρέφει την RGB μορφή ενός χρώματος */
fun Color.toRgb(): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return "rgb($r,$g,$b)"
}

/** Μετατρέπει μια συμβολοσειρά HEX σε [Color] αν είναι έγκυρη */
fun String.toColorOrNull(): Color? = try {
    Color(AndroidColor.parseColor(this))
} catch (_: Exception) {
    null
}
