package com.example.cardashboardtest.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.cardashboardtest.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom view for displaying a gauge (speedometer, tachometer, etc.)
 */
class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dimensions
    private val rimWidth = 20f
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // Scale configuration
    private var minValue = 0f
    private var maxValue = 100f
    private var majorTickStep = 10f
    private var minorTickStep = 5f

    // Current value and animation
    private var currentValue = 0f
    private var targetValue = 0f
    private var valueAnimator: android.animation.ValueAnimator? = null

    // Angles
    private val startAngle = 135f
    private val sweepAngle = 270f

    // Text
    private var title = "Speed"
    private var unit = "MPH"
    private var valueText = "0"

    // Colors
    private var rimColor = Color.DKGRAY
    private var faceColor = Color.BLACK
    private var scaleColor = Color.WHITE
    private var titleColor = Color.WHITE
    private var valueColor = Color.WHITE
    private var unitColor = Color.LTGRAY
    private var needleColor = Color.RED

    init {
        // Set up paints
        rimPaint.color = rimColor
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = rimWidth

        facePaint.color = faceColor
        facePaint.style = Paint.Style.FILL

        scalePaint.color = scaleColor
        scalePaint.style = Paint.Style.STROKE
        scalePaint.strokeWidth = 2f

        titlePaint.color = titleColor
        titlePaint.textSize = 24f
        titlePaint.textAlign = Paint.Align.CENTER

        valuePaint.color = valueColor
        valuePaint.textSize = 60f
        valuePaint.textAlign = Paint.Align.CENTER

        unitPaint.color = unitColor
        unitPaint.textSize = 20f
        unitPaint.textAlign = Paint.Align.CENTER

        needlePaint.color = needleColor
        needlePaint.style = Paint.Style.FILL
        needlePaint.strokeWidth = 5f

        // Get attributes if provided
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GaugeView)
            minValue = typedArray.getFloat(R.styleable.GaugeView_minValue, 0f)
            maxValue = typedArray.getFloat(R.styleable.GaugeView_maxValue, 100f)
            title = typedArray.getString(R.styleable.GaugeView_gaugeTitle) ?: "Speed"
            unit = typedArray.getString(R.styleable.GaugeView_unit) ?: "MPH"
            needleColor = typedArray.getColor(R.styleable.GaugeView_needleColor, Color.RED)
            needlePaint.color = needleColor
            typedArray.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Calculate dimensions based on view size
        val size = min(w, h)
        centerX = w / 2f
        centerY = h / 2f
        radius = (size / 2f) - rimWidth
        
        // Adjust text sizes based on gauge size
        titlePaint.textSize = radius * 0.15f
        valuePaint.textSize = radius * 0.3f
        unitPaint.textSize = radius * 0.12f
        
        // Adjust stroke widths
        rimPaint.strokeWidth = radius * 0.05f
        scalePaint.strokeWidth = radius * 0.02f
        needlePaint.strokeWidth = radius * 0.04f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw rim
        canvas.drawCircle(centerX, centerY, radius, rimPaint)
        
        // Draw face
        canvas.drawCircle(centerX, centerY, radius - rimWidth / 2, facePaint)
        
        // Draw scale
        drawScale(canvas)
        
        // Draw title
        canvas.drawText(title, centerX, centerY - radius * 0.3f, titlePaint)
        
        // Draw value
        canvas.drawText(valueText, centerX, centerY + radius * 0.1f, valuePaint)
        
        // Draw unit
        canvas.drawText(unit, centerX, centerY + radius * 0.3f, unitPaint)
        
        // Draw needle
        drawNeedle(canvas)
    }

    private fun drawScale(canvas: Canvas) {
        // Draw arc for scale background
        val oval = RectF(
            centerX - radius + rimWidth,
            centerY - radius + rimWidth,
            centerX + radius - rimWidth,
            centerY + radius - rimWidth
        )
        
        // Draw major ticks
        val majorTickCount = ((maxValue - minValue) / majorTickStep).toInt() + 1
        for (i in 0 until majorTickCount) {
            val value = minValue + i * majorTickStep
            val angle = valueToAngle(value)
            
            // Calculate start and end points for tick
            val startX = centerX + (radius - rimWidth - 20) * cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = centerY + (radius - rimWidth - 20) * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = centerX + (radius - rimWidth) * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = centerY + (radius - rimWidth) * sin(Math.toRadians(angle.toDouble())).toFloat()
            
            // Draw tick
            canvas.drawLine(startX, startY, endX, endY, scalePaint)
            
            // Draw tick label
            val labelX = centerX + (radius - rimWidth - 40) * cos(Math.toRadians(angle.toDouble())).toFloat()
            val labelY = centerY + (radius - rimWidth - 40) * sin(Math.toRadians(angle.toDouble())).toFloat()
            
            canvas.drawText(value.toInt().toString(), labelX, labelY, scalePaint)
        }
        
        // Draw minor ticks
        if (minorTickStep > 0) {
            val minorTickCount = ((maxValue - minValue) / minorTickStep).toInt() + 1
            for (i in 0 until minorTickCount) {
                val value = minValue + i * minorTickStep
                // Skip if this is also a major tick
                if (value % majorTickStep == 0f) continue
                
                val angle = valueToAngle(value)
                
                // Calculate start and end points for tick
                val startX = centerX + (radius - rimWidth - 10) * cos(Math.toRadians(angle.toDouble())).toFloat()
                val startY = centerY + (radius - rimWidth - 10) * sin(Math.toRadians(angle.toDouble())).toFloat()
                val endX = centerX + (radius - rimWidth) * cos(Math.toRadians(angle.toDouble())).toFloat()
                val endY = centerY + (radius - rimWidth) * sin(Math.toRadians(angle.toDouble())).toFloat()
                
                // Draw tick
                canvas.drawLine(startX, startY, endX, endY, scalePaint)
            }
        }
    }

    private fun drawNeedle(canvas: Canvas) {
        val angle = valueToAngle(currentValue)
        
        // Calculate needle points
        val needleLength = radius - rimWidth - 20
        val needleX = centerX + needleLength * cos(Math.toRadians(angle.toDouble())).toFloat()
        val needleY = centerY + needleLength * sin(Math.toRadians(angle.toDouble())).toFloat()
        
        // Draw needle
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint)
        
        // Draw needle center
        canvas.drawCircle(centerX, centerY, radius * 0.05f, needlePaint)
    }

    private fun valueToAngle(value: Float): Float {
        val valueRange = maxValue - minValue
        val angleRange = sweepAngle
        
        val valuePercent = (value - minValue) / valueRange
        return startAngle + valuePercent * angleRange
    }

    /**
     * Set the current value with animation
     */
    fun speedTo(value: Float, duration: Long = 1000) {
        targetValue = value.coerceIn(minValue, maxValue)
        valueText = String.format("%.1f", targetValue)
        
        // Cancel any existing animation
        valueAnimator?.cancel()
        
        // Create new animation
        valueAnimator = android.animation.ValueAnimator.ofFloat(currentValue, targetValue).apply {
            this.duration = duration
            addUpdateListener { animator ->
                currentValue = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * Set gauge configuration
     */
    fun configure(min: Float, max: Float, majorStep: Float, minorStep: Float) {
        minValue = min
        maxValue = max
        majorTickStep = majorStep
        minorTickStep = minorStep
        invalidate()
    }

    /**
     * Set gauge title
     */
    fun setTitle(newTitle: String) {
        title = newTitle
        invalidate()
    }

    /**
     * Set gauge unit
     */
    fun setUnit(newUnit: String) {
        unit = newUnit
        invalidate()
    }
}
