package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Υπολογίζει δυναμικά το μέγεθος του λογότυπου ώστε να προσαρμόζεται
 * σε διαφορετικές φυσικές διαστάσεις οθονών.
 *
 * @param referenceDiagonalInches Το μέγεθος οθόνης (σε ίντσες) στο οποίο
 * προορίζεται να είναι [sizeAtReferencePx]. Προεπιλογή 7 ίντσες.
 * @param sizeAtReferencePx Το μέγεθος του λογότυπου σε pixels όταν η διαγώνιος
 * είναι [referenceDiagonalInches]. Προεπιλογή 40 px.
 */
@Composable
fun rememberAdaptiveLogoSize(
    referenceDiagonalInches: Float = 7f,
    sizeAtReferencePx: Float = 40f
): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels.toFloat()
        val heightPx = metrics.heightPixels.toFloat()
        val diagonalPx = sqrt(widthPx.pow(2) + heightPx.pow(2))
        val diagonalInches = diagonalPx / metrics.densityDpi.toFloat()
        val targetPx = sizeAtReferencePx * (diagonalInches / referenceDiagonalInches)
        with(density) { targetPx.toDp() }
    }
}
