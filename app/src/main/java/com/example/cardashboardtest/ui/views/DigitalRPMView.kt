package com.example.cardashboardtest.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import androidx.core.content.ContextCompat
import com.example.cardashboardtest.R
import kotlin.math.min

class DigitalRPMView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rpm: Float = 0f
    private var maxRPM: Float = 8f
    private val segments = 16
    private var activeSegments = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val segmentGap = 2f
    private val scanningOffset = 0f
    private var isScanning = false

    private val rpmColor = Color.RED
    private val backgroundColor = Color.BLACK

    init {
        paint.style = Paint.Style.FILL
    }

    fun setRPM(value: Float, animate: Boolean = true) {
        rpm = value
        activeSegments = ((rpm / maxRPM) * segments).toInt()
        if (animate) {
            startScanAnimation()
        }
        invalidate()
    }

    private fun startScanAnimation() {
        if (!isScanning) {
            isScanning = true
            // Animate through all segments quickly
            var currentSegment = 0
            val handler = android.os.Handler()
            val runnable = object : Runnable {
                override fun run() {
                    if (currentSegment <= activeSegments) {
                        invalidate()
                        currentSegment++
                        handler.postDelayed(this, 50)
                    } else {
                        isScanning = false
                    }
                }
            }
            handler.post(runnable)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val segmentWidth = (width - (segments + 1) * segmentGap) / segments
        val segmentHeight = height * 0.8f
        
        // Draw background
        canvas.drawColor(backgroundColor)
        
        // Draw RPM segments
        paint.color = rpmColor
        for (i in 0 until activeSegments) {
            val left = i * (segmentWidth + segmentGap) + segmentGap
            val top = (height - segmentHeight) / 2
            canvas.drawRect(left, top, left + segmentWidth, top + segmentHeight, paint)
        }
    }
} 