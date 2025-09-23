package com.ioannapergamali.mysmartroute.view.ui.animation

import androidx.compose.animation.core.withFrameNanos
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.isActive

/**
 * Απλή σύνθεση που σχεδιάζει «ψηφία» τα οποία πέφτουν συνεχώς στην οθόνη
 * χωρίς να εξαφανίζονται πριν φτάσουν στο κάτω άκρο.
 */
@Composable
fun FallingBinaryBackground(
    modifier: Modifier = Modifier,
    objectCount: Int = 28,
    speedRange: ClosedFloatingPointRange<Float> = 70f..150f,
    sizeRange: ClosedFloatingPointRange<Float> = 14f..24f,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
) {
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f || objectCount <= 0) {
            return@BoxWithConstraints
        }

        val items = remember { mutableStateListOf<FallingBinaryItem>() }

        LaunchedEffect(
            objectCount,
            widthPx,
            heightPx,
            speedRange.start,
            speedRange.endInclusive,
            sizeRange.start,
            sizeRange.endInclusive
        ) {
            if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f || objectCount <= 0) {
                return@LaunchedEffect
            }

            items.clear()
            repeat(objectCount) {
                items += createItem(widthPx, heightPx, speedRange, sizeRange)
            }

            var lastFrame = withFrameNanos { it }
            while (isActive) {
                val frameTime = withFrameNanos { it }
                val deltaSeconds = (frameTime - lastFrame) / 1_000_000_000f
                lastFrame = frameTime

                items.forEach { item ->
                    item.y += item.speed * deltaSeconds
                    if (item.y - item.fontSizePx > heightPx) {
                        item.y = -item.fontSizePx
                        item.x = Random.nextFloat() * widthPx
                        item.speed = speedRange.randomValue()
                        item.fontSizePx = sizeRange.randomValue()
                        item.char = randomBinaryChar()
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val fontSize = with(density) { item.fontSizePx.toSp() }
                Text(
                    text = item.char.toString(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    modifier = Modifier.offset {
                        IntOffset(item.x.roundToInt(), item.y.roundToInt())
                    }
                )
            }
        }
    }
}

private class FallingBinaryItem(
    x: Float,
    y: Float,
    speed: Float,
    fontSizePx: Float,
    char: Char
) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var speed by mutableStateOf(speed)
    var fontSizePx by mutableStateOf(fontSizePx)
    var char by mutableStateOf(char)
}

private fun createItem(
    width: Float,
    height: Float,
    speedRange: ClosedFloatingPointRange<Float>,
    sizeRange: ClosedFloatingPointRange<Float>
): FallingBinaryItem {
    val x = Random.nextFloat() * width
    val y = Random.nextFloat() * height
    val speed = speedRange.randomValue()
    val size = sizeRange.randomValue()
    return FallingBinaryItem(x, y, speed, size, randomBinaryChar())
}

private fun ClosedFloatingPointRange<Float>.randomValue(): Float {
    val lower = start
    val upper = endInclusive
    return if (upper <= lower) {
        lower
    } else {
        lower + Random.nextFloat() * (upper - lower)
    }
}

private fun randomBinaryChar(): Char = if (Random.nextBoolean()) '0' else '1'
