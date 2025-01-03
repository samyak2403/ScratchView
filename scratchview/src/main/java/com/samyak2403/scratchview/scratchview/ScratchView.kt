package com.samyak2403.scratchview.scratchview


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.samyak2403.scratchview.R
import com.samyak2403.scratchview.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class ScratchView : View {
    /**
     * Core Items
     */
    private val mContext: Context
    var scratchBitmap: Bitmap? = null
    private var attrs: AttributeSet? = null
    private var styleAttr = 0
    private val view: View? = null
    private var mX = 0f
    private var mY = 0f

    /**
     * Bitmap holding the scratch region.
     */
    private var mScratchBitmap: Bitmap? = null

    /**
     * Drawable canvas area through which the scratchable area is drawn.
     */
    private var mCanvas: Canvas? = null

    /**
     * Path holding the erasing path done by the user.
     */
    private var mErasePath: Path? = null

    /**
     * Path to indicate where the user have touched.
     */
    private var mTouchPath: Path? = null

    /**
     * Paint properties for drawing the scratch area.
     */
    private var mBitmapPaint: Paint? = null

    /**
     * Paint properties for erasing the scratch region.
     */
    var erasePaint: Paint? = null
        private set

    /**
     * Gradient paint properties that lies as a background for scratch region.
     */
    private var mGradientBgPaint: Paint? = null

    /**
     * Sample Drawable bitmap having the scratch pattern.
     */
    private var mDrawable: BitmapDrawable? = null


    /**
     * Listener object callback reference to send back the callback when the text has been revealed.
     */
    private var mRevealListener: IRevealListener? = null

    /**
     * Reveal percent value.
     */
    private var mRevealPercent = 0f

    /**
     * Thread Count
     */
    private var mThreadCount = 0

    constructor(context: Context) : super(context) {
        this.mContext = context
        init()
    }


    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.mContext = context
        this.attrs = attrs
        init()
    }


    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.mContext = context
        this.attrs = attrs
        this.styleAttr = defStyleAttr
        init()
    }

    /**
     * Initialises the paint drawing elements.
     */
    private fun init() {
        mTouchPath = Path()

        erasePaint = Paint()
        erasePaint!!.isAntiAlias = true
        erasePaint!!.isDither = true
        erasePaint!!.color = -0x10000
        erasePaint!!.style = Paint.Style.STROKE
        erasePaint!!.strokeJoin = Paint.Join.BEVEL
        erasePaint!!.strokeCap = Paint.Cap.ROUND
        erasePaint!!.setXfermode(
            PorterDuffXfermode(
                PorterDuff.Mode.CLEAR
            )
        )
        setStrokeWidth(6)

        mGradientBgPaint = Paint()

        mErasePath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)

        val arr = mContext.obtainStyledAttributes(
            attrs, R.styleable.ScratchView,
            styleAttr, 0
        )

        val overlayImage =
            arr.getResourceId(R.styleable.ScratchView_overlay_image, R.drawable.ic_scratch_pattern)

        val overlayWidth = arr.getDimension(R.styleable.ScratchView_overlay_width, 1000f)
        val overlayHeight = arr.getDimension(R.styleable.ScratchView_overlay_height, 1000f)


        var tileMode = arr.getString(R.styleable.ScratchView_tile_mode)
        if (tileMode == null) {
            tileMode = "CLAMP"
        }
        scratchBitmap = BitmapFactory.decodeResource(resources, overlayImage)
        if (scratchBitmap == null) {
            scratchBitmap = drawableToBitmap(ContextCompat.getDrawable(context, overlayImage)!!)
        }
        scratchBitmap = Bitmap.createScaledBitmap(
            scratchBitmap!!,
            overlayWidth.toInt(),
            overlayHeight.toInt(),
            false
        )
        mDrawable = BitmapDrawable(resources, scratchBitmap)

        when (tileMode) {
            "REPEAT" -> mDrawable!!.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            "MIRROR" -> mDrawable!!.setTileModeXY(Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
            else -> mDrawable!!.setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
    }

    /**
     * Set the strokes width based on the parameter multiplier.
     *
     * @param multiplier can be 1,2,3 and so on to set the stroke width of the paint.
     */
    fun setStrokeWidth(multiplier: Int) {
        erasePaint!!.strokeWidth = multiplier * STROKE_WIDTH
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mScratchBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mScratchBitmap!!)

        val rect = Rect(0, 0, width, height)
        mDrawable!!.bounds = rect

        val startGradientColor = ContextCompat.getColor(context, R.color.scratch_start_gradient)
        val endGradientColor = ContextCompat.getColor(context, R.color.scratch_end_gradient)


        mGradientBgPaint!!.setShader(
            LinearGradient(
                0f,
                0f,
                0f,
                height.toFloat(),
                startGradientColor,
                endGradientColor,
                Shader.TileMode.MIRROR
            )
        )

        mCanvas!!.drawRect(rect, mGradientBgPaint!!)
        mDrawable!!.draw(mCanvas!!)
        //        Toast.makeText(mContext, String.valueOf(getWidth()), Toast.LENGTH_LONG).show();
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mScratchBitmap!!, 0f, 0f, mBitmapPaint)
        canvas.drawPath(mErasePath!!, erasePaint!!)
    }

    private fun touch_start(x: Float, y: Float) {
        mErasePath!!.reset()
        mErasePath!!.moveTo(x, y)
        mX = x
        mY = y
    }

    /**
     * clears the scratch area to reveal the hidden image.
     */
    fun clear() {
        val bounds = viewBounds
        var left = bounds[0]
        var top = bounds[1]
        var right = bounds[2]
        var bottom = bounds[3]

        val width = right - left
        val height = bottom - top
        val centerX = left + width / 2
        val centerY = top + height / 2

        left = centerX - width / 2
        top = centerY - height / 2
        right = left + width
        bottom = top + height

        val paint = Paint()
        paint.setXfermode(
            PorterDuffXfermode(
                PorterDuff.Mode.CLEAR
            )
        )

        mCanvas!!.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        checkRevealed()
        invalidate()
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = abs((x - mX).toDouble()).toFloat()
        val dy = abs((y - mY).toDouble()).toFloat()
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mErasePath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y

            drawPath()
        }


        mTouchPath!!.reset()
        mTouchPath!!.addCircle(mX, mY, 30f, Path.Direction.CW)
    }

    private fun drawPath() {
        mErasePath!!.lineTo(mX, mY)
        // commit the path to our offscreen
        mCanvas!!.drawPath(mErasePath!!, erasePaint!!)
        // kill this so we don't double draw
        mTouchPath!!.reset()
        mErasePath!!.reset()
        mErasePath!!.moveTo(mX, mY)

        checkRevealed()
    }

    fun reveal() {
        clear()
    }

    fun mask() {
        clear()
        mRevealPercent = 0f
        mCanvas!!.drawBitmap(scratchBitmap!!, 0f, 0f, mBitmapPaint)
        invalidate()
    }

    private fun touch_up() {
        drawPath()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }

            else -> {}
        }
        return true
    }

    val color: Int
        get() = erasePaint!!.color

    fun setEraserMode() {
        erasePaint!!.setXfermode(
            PorterDuffXfermode(
                PorterDuff.Mode.CLEAR
            )
        )
    }

    fun setRevealListener(listener: IRevealListener?) {
        this.mRevealListener = listener
    }

    val isRevealed: Boolean
        get() = mRevealPercent >= 0.33

    private fun checkRevealed() {
        if (!isRevealed && mRevealListener != null) {
            val bounds = viewBounds
            val left = bounds[0]
            val top = bounds[1]
            val width = bounds[2] - left
            val height = bounds[3] - top

            // Do not create multiple calls to compare.
            if (mThreadCount > 1) {
                Log.d("Captcha", "Count greater than 1")
                return
            }

            mThreadCount++

            fun processBitmap(left: Int, top: Int, width: Int, height: Int) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val percentRevealed = withContext(Dispatchers.Default) {
                            mThreadCount++
                            try {
                                mScratchBitmap?.let { bitmap ->
                                    val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                                    BitmapUtils.getTransparentPixelPercent(croppedBitmap)
                                } ?: 0f
                            } finally {
                                mThreadCount--
                            }
                        }

                        // Post-process on the main thread
                        if (!isRevealed) {
                            val oldValue = mRevealPercent
                            mRevealPercent = percentRevealed

                            if (oldValue != percentRevealed) {
                                mRevealListener?.onRevealPercentChangedListener(this@ScratchView, percentRevealed)
                            }

                            if (isRevealed) {
                                mRevealListener?.onRevealed(this@ScratchView)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val viewBounds: IntArray
        get() {
            val left = 0
            val top = 0
            val width = width
            val height = height
            return intArrayOf(left, top, left + width, top + height)
        }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    interface IRevealListener {
        fun onRevealed(scratchView: ScratchView?)

        fun onRevealPercentChangedListener(scratchView: ScratchView?, percent: Float)
    }

    companion object {
        const val STROKE_WIDTH: Float = 12f
        private const val TOUCH_TOLERANCE = 4f
    }
}