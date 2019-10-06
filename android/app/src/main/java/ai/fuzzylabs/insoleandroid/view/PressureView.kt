package ai.fuzzylabs.insoleandroid.view

import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class PressureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
    private val paint =
        Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.FILL_AND_STROKE
        }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = 35f
    }

    private val capacity = 3
    private val queue: Queue<PressureSensorEvent> = LinkedBlockingQueue<PressureSensorEvent>(capacity)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        queue.map {
            drawPressureCircle(canvas, it.x, it.y, it.pressure, it.sensor)
        }
    }

    private fun drawPressureCircle(canvas: Canvas, x: Int, y: Int, pressure: Float, n: Int) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), pressure * 10, paint)
        canvas.drawText(n.toString(), x.toFloat(), y.toFloat(), textPaint)
    }

    fun react(event: PressureSensorEvent) {
        if (queue.size == capacity)
            queue.remove()
        queue.add(event)
        this.invalidate()
    }
}