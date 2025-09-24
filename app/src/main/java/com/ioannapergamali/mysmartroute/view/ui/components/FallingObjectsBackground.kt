package com.ioannapergamali.mysmartroute.view.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.onSizeChanged
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.isActive

private data class FallingObject(
    val symbol: String,
    val sizeSp: Float,
    val sizePx: Float,
    val x: Float,
    val y: Float,
    val speedPxPerSec: Float,
    val alpha: Float
)

@Composable
fun FallingObjectsBackground(
    modifier: Modifier = Modifier,
    objectCount: Int = 18,
    symbols: List<String> = listOf("★", "✦", "✧", "✩", "✪"),
    minSpeed: Dp = 28.dp,
    maxSpeed: Dp = 84.dp,
    minSize: TextUnit = 16.sp,
    maxSize: TextUnit = 28.sp,
    baseAlpha: Float = 0.35f,
    minObjectSpacing: Dp = 8.dp
) {
    val density = LocalDensity.current
    val random = remember { Random(System.currentTimeMillis()) }
    val objects = remember { mutableStateListOf<FallingObject>() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val safeSymbols = if (symbols.isNotEmpty()) symbols else listOf("•")

    val minSpeedPx = with(density) { minSpeed.toPx() }
    val maxSpeedPx = with(density) { maxSpeed.toPx() }
    val minSizePx = with(density) { minSize.toPx() }
    val maxSizePx = with(density) { maxSize.toPx() }
    val minSizeSp = minSize.value
    val maxSizeSp = maxSize.value
    val minSpacingPx = with(density) { minObjectSpacing.toPx() }

    val actualMinSpeed = min(minSpeedPx, maxSpeedPx)
    val actualMaxSpeed = max(minSpeedPx, maxSpeedPx)
    val actualMinSizePx = min(minSizePx, maxSizePx)
    val actualMaxSizePx = max(minSizePx, maxSizePx)
    val actualMinSizeSp = min(minSizeSp, maxSizeSp)
    val actualMaxSizeSp = max(minSizeSp, maxSizeSp)

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = it }
    ) {
        val widthPx = containerSize.width.toFloat()
        val heightPx = containerSize.height.toFloat()

        LaunchedEffect(
            widthPx,
            heightPx,
            objectCount,
            safeSymbols,
            actualMinSpeed,
            actualMaxSpeed,
            actualMinSizePx,
            actualMaxSizePx,
            actualMinSizeSp,
            actualMaxSizeSp,
            minSpacingPx
        ) {
            if (widthPx <= 0f || heightPx <= 0f) {
                objects.clear()
                return@LaunchedEffect
            }
            objects.clear()
            repeat(objectCount) {
                val newObject = createFallingObject(
                    random = random,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    symbols = safeSymbols,
                    minSizePx = actualMinSizePx,
                    maxSizePx = actualMaxSizePx,
                    minSizeSp = actualMinSizeSp,
                    maxSizeSp = actualMaxSizeSp,
                    minSpeedPx = actualMinSpeed,
                    maxSpeedPx = actualMaxSpeed,
                    minSpacingPx = minSpacingPx,
                    existingObjects = objects,
                    startAbove = false
                )
                objects += newObject
            }
        }

        LaunchedEffect(
            widthPx,
            heightPx,
            objectCount,
            safeSymbols,
            actualMinSpeed,
            actualMaxSpeed,
            actualMinSizePx,
            actualMaxSizePx,
            actualMinSizeSp,
            actualMaxSizeSp,
            minSpacingPx
        ) {
            if (widthPx <= 0f || heightPx <= 0f) return@LaunchedEffect
            var lastFrameTime = withFrameNanos { it }
            while (isActive) {
                val frameTime = withFrameNanos { it }
                val deltaSec = (frameTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = frameTime
                objects.indices.forEach { index ->
                    val obj = objects[index]
                    val newY = obj.y + obj.speedPxPerSec * deltaSec
                    val updated = if (newY - obj.sizePx > heightPx) {
                        val others = objects.filterIndexed { i, _ -> i != index }
                        createFallingObject(
                            random = random,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            symbols = safeSymbols,
                            minSizePx = actualMinSizePx,
                            maxSizePx = actualMaxSizePx,
                            minSizeSp = actualMinSizeSp,
                            maxSizeSp = actualMaxSizeSp,
                            minSpeedPx = actualMinSpeed,
                            maxSpeedPx = actualMaxSpeed,
                            minSpacingPx = minSpacingPx,
                            existingObjects = others,
                            startAbove = true
                        )
                    } else {
                        obj.copy(y = newY)
                    }
                    objects[index] = updated
                }
            }
        }

        objects.forEach { obj ->
            Text(
                text = obj.symbol,
                fontSize = obj.sizeSp.sp,
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = (obj.alpha * baseAlpha).coerceIn(0f, 1f)
                ),
                modifier = Modifier.offset {
                    IntOffset(obj.x.roundToInt(), obj.y.roundToInt())
                }
            )
        }
    }
}

private fun createFallingObject(
    random: Random,
    widthPx: Float,
    heightPx: Float,
    symbols: List<String>,
    minSizePx: Float,
    maxSizePx: Float,
    minSizeSp: Float,
    maxSizeSp: Float,
    minSpeedPx: Float,
    maxSpeedPx: Float,
    minSpacingPx: Float,
    existingObjects: List<FallingObject> = emptyList(),
    startAbove: Boolean
): FallingObject {
    val sizePx = random.between(minSizePx, maxSizePx)
    val sizeSp = random.between(minSizeSp, maxSizeSp)
    val y = if (startAbove) {
        -sizePx - random.between(0f, heightPx * 0.25f)
    } else {
        random.between(0f, heightPx)
    }
    val availableWidth = (widthPx - sizePx).coerceAtLeast(0f)
    val x = if (availableWidth == 0f) 0f else findNonOverlappingX(
        random = random,
        availableWidth = availableWidth,
        sizePx = sizePx,
        y = y,
        existingObjects = existingObjects,
        minSpacingPx = minSpacingPx
    )
    val speed = random.between(minSpeedPx, maxSpeedPx)
    val alpha = random.between(0.45f, 0.95f)
    val symbol = symbols[random.nextInt(symbols.size)]
    return FallingObject(
        symbol = symbol,
        sizeSp = sizeSp,
        sizePx = sizePx,
        x = x,
        y = y,
        speedPxPerSec = speed,
        alpha = alpha
    )
}

private fun Random.between(minValue: Float, maxValue: Float): Float {
    if (maxValue <= minValue) return minValue
    return minValue + nextFloat() * (maxValue - minValue)
}

private fun findNonOverlappingX(
    random: Random,
    availableWidth: Float,
    sizePx: Float,
    y: Float,
    existingObjects: List<FallingObject>,
    minSpacingPx: Float,
    maxAttempts: Int = 12
): Float {
    if (existingObjects.isEmpty()) {
        return random.between(0f, availableWidth)
    }
    repeat(maxAttempts) {
        val candidate = random.between(0f, availableWidth)
        val overlaps = existingObjects.any { other ->
            isOverlapping(
                candidateX = candidate,
                candidateY = y,
                candidateSize = sizePx,
                other = other,
                spacing = minSpacingPx
            )
        }
        if (!overlaps) {
            return candidate
        }
    }
    return random.between(0f, availableWidth)
}

private fun isOverlapping(
    candidateX: Float,
    candidateY: Float,
    candidateSize: Float,
    other: FallingObject,
    spacing: Float
): Boolean {
    val candidateStartX = candidateX - spacing
    val candidateEndX = candidateX + candidateSize + spacing
    val otherStartX = other.x - spacing
    val otherEndX = other.x + other.sizePx + spacing
    val horizontalOverlap = candidateStartX < otherEndX && otherStartX < candidateEndX
    if (!horizontalOverlap) return false

    val candidateStartY = candidateY - spacing
    val candidateEndY = candidateY + candidateSize + spacing
    val otherStartY = other.y - spacing
    val otherEndY = other.y + other.sizePx + spacing
    return candidateStartY < otherEndY && otherStartY < candidateEndY
}
