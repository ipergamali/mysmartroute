package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Υπολογίζει δυναμικά το μέγεθος του λογότυπου ώστε να προσαρμόζεται
 * σε διαφορετικές φυσικές διαστάσεις οθονών.
 *
 * @param referenceDiagonalInches Το μέγεθος οθόνης (σε ίντσες) στο οποίο
 * προορίζεται να είναι [sizeAtReferencePx]. Προεπιλογή 7 ίντσες.
 * @param sizeAtReferencePx Το μέγεθος του λογότυπου σε pixel όταν η διαγώνιος
 * είναι [referenceDiagonalInches]. Προεπιλογή 40 px.
 */
@Composable
fun rememberAdaptiveLogoSize(
    referenceDiagonalInches: Float = 7f,
    sizeAtReferencePx: Float = 40f
): Dp {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels.toFloat()
        val heightPx = metrics.heightPixels.toFloat()
        val diagonalPx = sqrt(widthPx.pow(2) + heightPx.pow(2))
        val density = metrics.density
        val diagonalInches = diagonalPx / metrics.densityDpi.toFloat()
        val baseDp = sizeAtReferencePx / density
        val targetDp = when {
            diagonalInches in 7f..9f -> baseDp
            diagonalInches < 7f -> baseDp * (diagonalInches / 7f)
            else -> baseDp * (diagonalInches / 9f)
        }
        targetDp.dp
    }
}

/**
 * Απλούστερη παραλλαγή που επιστρέφει μέγεθος 960px για οθόνες 7-9 ιντσών
 * και προσαρμόζει γραμμικά για μικρότερες ή μεγαλύτερες συσκευές.
 */
@Composable
fun rememberLogoSize(): Dp {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels.toFloat()
        val heightPx = metrics.heightPixels.toFloat()
        val diagonalPx = sqrt(widthPx.pow(2) + heightPx.pow(2))
        val density = metrics.density
        val diagonalInches = diagonalPx / metrics.densityDpi.toFloat()
        // 10x αύξηση του βασικού μεγέθους (96 * 10 = 960px)
        val baseDp = 960f / density

        val targetDp = when {
            diagonalInches in 7f..9f -> baseDp
            diagonalInches < 7f -> baseDp * (diagonalInches / 7f)
            else -> baseDp * (diagonalInches / 9f)
        }

        targetDp.dp
    }
}
