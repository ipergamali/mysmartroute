package com.ioannapergamali.mysmartroute.view.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowOrientation { Portrait, Landscape }

data class WindowInfo(val width: Dp, val height: Dp, val orientation: WindowOrientation)

@Composable
fun rememberWindowInfo(): WindowInfo {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val width = configuration.screenWidthDp.dp
        val height = configuration.screenHeightDp.dp
        val orientation = if (width < height) WindowOrientation.Portrait else WindowOrientation.Landscape
        WindowInfo(width, height, orientation)
    }
}
