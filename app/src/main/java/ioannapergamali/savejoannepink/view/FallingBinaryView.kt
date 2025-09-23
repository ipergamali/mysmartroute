package ioannapergamali.savejoannepink.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Typeface
import kotlin.math.max
import kotlin.random.Random

private data class FallingBinaryItem(
    var x: Float,
    var y: Float,
    var speed: Float,
    var text: String,
    var width: Float
)

/**
 * Απλό custom View που ζωγραφίζει τυχαία "δυαδικά" αντικείμενα
 * τα οποία πέφτουν από την κορυφή της οθόνης μέχρι το τέλος της.
 */
class FallingBinaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8033FF33")
        textSize = 18f * resources.displayMetrics.scaledDensity
        typeface = Typeface.MONOSPACE
    }
    private val fontMetrics = Paint.FontMetrics()
    private val random = Random(System.currentTimeMillis())
    private val items = mutableListOf<FallingBinaryItem>()
    private var lastFrameNanos = 0L

    init {
        paint.getFontMetrics(fontMetrics)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lastFrameNanos = 0L
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) {
            items.clear()
            return
        }

        paint.getFontMetrics(fontMetrics)
        val area = w * h
        val density = resources.displayMetrics.density
        val baseCount = max(12, (area / (120f * density * density)).toInt())

        items.clear()
        repeat(baseCount) {
            val text = nextBinaryText()
            val textWidth = paint.measureText(text)
            val startY = random.nextFloat() * h
            items += FallingBinaryItem(
                x = randomX(textWidth, w.toFloat()),
                y = startY,
                speed = randomSpeed(h.toFloat()),
                text = text,
                width = textWidth
            )
        }
        lastFrameNanos = 0L
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || items.isEmpty()) {
            return
        }

        val now = System.nanoTime()
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now
        }
        val deltaSeconds = (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now

        val viewHeight = height.toFloat()
        val viewWidth = width.toFloat()

        for (item in items) {
            item.y += item.speed * deltaSeconds
            if (item.y + fontMetrics.top > viewHeight) {
                resetItem(item, viewWidth)
            }
            canvas.drawText(item.text, item.x, item.y, paint)
        }

        postInvalidateOnAnimation()
    }

    private fun resetItem(item: FallingBinaryItem, viewWidth: Float) {
        item.text = nextBinaryText()
        item.width = paint.measureText(item.text)
        paint.getFontMetrics(fontMetrics)
        item.x = randomX(item.width, viewWidth)
        item.y = -fontMetrics.bottom
        item.speed = randomSpeed(height.toFloat())
    }

    private fun randomX(textWidth: Float, viewWidth: Float): Float {
        val maxX = viewWidth - textWidth
        return if (maxX <= 0f) 0f else random.nextFloat() * maxX
    }

    private fun randomSpeed(viewHeight: Float): Float {
        if (viewHeight <= 0f) return 100f
        val min = viewHeight / 8f
        val maxSpeed = viewHeight / 3f
        return min + random.nextFloat() * (maxSpeed - min)
    }

    private fun nextBinaryText(): String {
        val length = random.nextInt(4, 9)
        return buildString(length) {
            repeat(length) {
                append(if (random.nextBoolean()) '1' else '0')
            }
        }
    }
}
