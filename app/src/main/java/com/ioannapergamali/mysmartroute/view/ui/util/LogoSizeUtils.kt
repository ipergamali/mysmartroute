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
 * προορίζεται να είναι [sizeAtReferenceDp]. Προεπιλογή 7 ίντσες.
 * @param sizeAtReferenceDp Το μέγεθος του λογότυπου σε dp όταν η διαγώνιος
 * είναι [referenceDiagonalInches]. Προεπιλογή 40 dp.
 */
@Composable
fun rememberAdaptiveLogoSize(
    referenceDiagonalInches: Float = 7f,
    sizeAtReferenceDp: Float = 40f
): Dp {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels.toFloat()
        val heightPx = metrics.heightPixels.toFloat()
        val diagonalPx = sqrt(widthPx.pow(2) + heightPx.pow(2))
        val diagonalInches = diagonalPx / metrics.densityDpi.toFloat()
        val targetDp = when {
            diagonalInches in 7f..9f -> sizeAtReferenceDp
            diagonalInches < 7f -> sizeAtReferenceDp * (diagonalInches / 7f)
            else -> sizeAtReferenceDp * (diagonalInches / 9f)
        }
        targetDp.dp
    }
}

/**
 * Απλούστερη παραλλαγή που επιστρέφει 40 dp για οθόνες 7-9 ιντσών
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
        val diagonalInches = diagonalPx / metrics.densityDpi.toFloat()

        val targetDp = when {
            diagonalInches in 7f..9f -> 40f
            diagonalInches < 7f -> 40f * (diagonalInches / 7f)
            else -> 40f * (diagonalInches / 9f)
        }

        targetDp.dp
    }
}
