package com.example.cardashboardtest.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.cardashboardtest.R

class DigitalSpeedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speed: Int = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textColor = Color.RED
    private val backgroundColor = Color.BLACK
    private var textSize = 120f

    init {
        paint.apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
            style = Paint.Style.FILL
        }
    }

    fun setSpeed(value: Int) {
        speed = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Adjust text size based on view size
        textSize = h * 0.8f
        paint.textSize = textSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawColor(backgroundColor)

        // Draw speed value
        val speedText = String.format("%03d", speed)
        val xPos = width / 2f
        val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2
        
        // Draw with glow effect
        paint.setShadowLayer(15f, 0f, 0f, textColor)
        canvas.drawText(speedText, xPos, yPos, paint)
        paint.clearShadowLayer()
    }
} 