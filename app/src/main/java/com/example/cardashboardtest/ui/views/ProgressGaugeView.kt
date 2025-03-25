package com.example.cardashboardtest.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.cardashboardtest.R

/**
 * Custom view for displaying a progress gauge (fuel, temperature, etc.)
 */
class ProgressGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Dimensions
    private val cornerRadius = 16f
    private var width = 0
    private var height = 0

    // Progress
    private var progress = 0f
    private var maxProgress = 100f
    private var progressAnimator: android.animation.ValueAnimator? = null

    // Text
    private var title = "Fuel"

    // Colors
    private var backgroundColor = Color.DKGRAY
    private var progressColor = Color.GREEN
    private var textColor = Color.WHITE
    private var titleColor = Color.LTGRAY

    init {
        // Set up paints
        backgroundPaint.color = backgroundColor
        backgroundPaint.style = Paint.Style.FILL

        progressPaint.color = progressColor
        progressPaint.style = Paint.Style.FILL

        textPaint.color = textColor
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.CENTER

        titlePaint.color = titleColor
        titlePaint.textSize = 24f
        titlePaint.textAlign = Paint.Align.CENTER

        // Get attributes if provided
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ProgressGaugeView)
            maxProgress = typedArray.getFloat(R.styleable.ProgressGaugeView_maxProgress, 100f)
            title = typedArray.getString(R.styleable.ProgressGaugeView_gaugeTitle) ?: "Fuel"
            progressColor = typedArray.getColor(R.styleable.ProgressGaugeView_progressColor, Color.GREEN)
            progressPaint.color = progressColor
            typedArray.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h
        
        // Adjust text sizes based on view size
        titlePaint.textSize = height * 0.15f
        textPaint.textSize = height * 0.25f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        val backgroundRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw progress
        val progressWidth = (width * (progress / maxProgress)).coerceAtMost(width.toFloat())
        val progressRect = RectF(0f, 0f, progressWidth, height.toFloat())
        canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
        
        // Draw title
        canvas.drawText(title, width / 2f, height * 0.3f, titlePaint)
        
        // Draw progress text
        val progressText = "${progress.toInt()}%"
        canvas.drawText(progressText, width / 2f, height * 0.65f, textPaint)
    }

    /**
     * Set the current progress with animation
     */
    fun setProgress(value: Float, duration: Long = 500) {
        val targetProgress = value.coerceIn(0f, maxProgress)
        
        // Cancel any existing animation
        progressAnimator?.cancel()
        
        // Create new animation
        progressAnimator = android.animation.ValueAnimator.ofFloat(progress, targetProgress).apply {
            this.duration = duration
            addUpdateListener { animator ->
                progress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * Set gauge title
     */
    fun setTitle(newTitle: String) {
        title = newTitle
        invalidate()
    }

    /**
     * Set progress color
     */
    fun setProgressColor(color: Int) {
        progressColor = color
        progressPaint.color = color
        invalidate()
    }
}
