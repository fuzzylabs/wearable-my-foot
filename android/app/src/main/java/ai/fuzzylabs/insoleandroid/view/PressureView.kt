package ai.fuzzylabs.insoleandroid.view

import ai.fuzzylabs.insoleandroid.R
import ai.fuzzylabs.insoleandroid.model.PressureSensorEvent
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Handler
import java.util.concurrent.ConcurrentLinkedQueue

class PressureView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
    private val LIFETIME: Int = 1000
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

    private val queue: ConcurrentLinkedQueue<Pair<Long, PressureSensorEvent>> = ConcurrentLinkedQueue()
    private val foot_overlay_resource = BitmapFactory.decodeResource(resources, R.drawable.foot_overlay)

    init {
        Handler().postDelayed({
            updateCircles()
        }, 100  )
    }

    private fun updateCircles() {
        val t = System.nanoTime()
        for (e in queue) {
            if (t - e.first > 0.8e9) {
                queue.remove(e)
                this.invalidate()
            }
        }
        Handler().postDelayed({
            updateCircles()
        }, 100)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val image = Bitmap.createScaledBitmap(foot_overlay_resource, canvas.width, canvas.height, true)
        canvas.drawBitmap(image, 0f, 0f, null)

        queue.map {
            drawPressureCircle(canvas, it.second.x, it.second.y, it.second.pressure * 2, it.second.sensor)
        }
    }

    private fun drawPressureCircle(canvas: Canvas, x: Int, y: Int, pressure: Float, n: Int) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), pressure, paint)
        canvas.drawText(n.toString(), x.toFloat(), y.toFloat(), textPaint)
    }

    fun react(event: PressureSensorEvent) {
        queue.offer(Pair(System.nanoTime(), event))
        this.invalidate()
    }
}