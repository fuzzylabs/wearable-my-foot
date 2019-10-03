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

    private val queue: Queue<PressureSensorEvent> = LinkedBlockingQueue<PressureSensorEvent>(10)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //val d = resources.getDrawable(R.drawable.ic_foot_overlay)
        //d.setBounds(left, top, right, bottom)
        //d.draw(canvas)

        queue.map {
            drawPressureCircle(canvas, it.x, it.y, it.pressure)
        }
    }

    private fun drawPressureCircle(canvas: Canvas, x: Int, y: Int, pressure: Float) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), pressure * 10, paint)
    }

    fun react(event: PressureSensorEvent) {
        queue.offer(event)
    }
}