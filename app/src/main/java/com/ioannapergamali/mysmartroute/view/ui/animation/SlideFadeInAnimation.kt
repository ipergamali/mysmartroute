package com.ioannapergamali.mysmartroute.view.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberSlideFadeInAnimation(durationMillis: Int = 1000): Pair<Dp, Float> {
    val offset = remember { Animatable(-40f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        offset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
    }

    return Pair(offset.value.dp, alpha.value)
}
