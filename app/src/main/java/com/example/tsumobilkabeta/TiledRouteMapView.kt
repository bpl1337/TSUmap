package com.example.tsumobilkabeta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.toColorInt
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class TiledRouteMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private companion object {
        val START_COLOR = "#2E7D32".toColorInt()
        val END_COLOR = "#C62828".toColorInt()
        const val IDLE_REDRAW_DELAY_MS = 110L
        const val INTERACTION_DECODE_THROTTLE_MS = 40L
        const val PREVIEW_IN_SAMPLE_SIZE = 8
        const val TILE_DISK_CACHE_DIR = "map_tile_cache"
        const val TILE_DISK_CACHE_MAX_BYTES = 96L * 1024L * 1024L
    }

    private data class DecodeRequest(
        val region: Rect,
        val sample: Int,
        val generation: Int,
    )

    var startPoint: PointF? = null
        set(value) {
            field = value
            invalidate()
        }

    var endPoint: PointF? = null
        set(value) {
            field = value
            invalidate()
        }

    var onMapTap: ((PointF) -> Unit)? = null

    private var decoder: BitmapRegionDecoder? = null
    private var mapWidth: Int = 1
    private var mapHeight: Int = 1

    private var scale: Float = 1f
    private var minScale: Float = 1f
    private var maxScale: Float = 8f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    private var cachedBitmap: Bitmap? = null
    private var cachedRect: Rect? = null
    private var cachedSample: Int = 1
    private var pendingRequest: DecodeRequest? = null
    private var previewBitmap: Bitmap? = null
    private var lastDecodeRequestAtMs: Long = 0L
    private var currentMapResId: Int = 0
    private var lastDiskMissKey: String? = null
    private val diskCacheLock = Any()

    private val decoderExecutor = Executors.newSingleThreadExecutor()
    private val decoderGeneration = AtomicInteger(0)
    private var isInteracting: Boolean = false

    private val redrawOnIdle = Runnable {
        isInteracting = false
        invalidate()
    }

    private val markerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val markerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 34f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val tapDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val sourcePoint = viewToSource(e.x, e.y) ?: return false
                onMapTap?.invoke(sourcePoint)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float,
            ): Boolean {
                offsetX -= distanceX
                offsetY -= distanceY
                clampOffsets()
                invalidate()
                return true
            }
        }
    )

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val previousScale = scale
                scale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)

                val focusX = detector.focusX
                val focusY = detector.focusY

                val srcX = (focusX - offsetX) / previousScale
                val srcY = (focusY - offsetY) / previousScale

                offsetX = focusX - srcX * scale
                offsetY = focusY - srcY * scale

                clampOffsets()
                invalidate()
                return true
            }
        }
    )

    fun setMapImageResource(resId: Int) {
        recycleDecoder()
        currentMapResId = resId
        resources.openRawResource(resId).use { stream ->
            @Suppress("DEPRECATION")
            decoder = BitmapRegionDecoder.newInstance(stream, false)
        }

        resources.openRawResource(resId).use { stream ->
            previewBitmap = BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inSampleSize = PREVIEW_IN_SAMPLE_SIZE
                },
            )
        }

        mapWidth = decoder?.width ?: 1
        mapHeight = decoder?.height ?: 1
        resetToFit()
        warmupInitialTile()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE,
            -> {
                isInteracting = true
                removeCallbacks(redrawOnIdle)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                removeCallbacks(redrawOnIdle)
                postDelayed(redrawOnIdle, IDLE_REDRAW_DELAY_MS)
            }
        }

        val scaleHandled = scaleDetector.onTouchEvent(event)
        val tapHandled = tapDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            performClick()
        }
        return scaleHandled || tapHandled || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetToFit()
        warmupInitialTile()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawVisibleRegion(canvas)
        drawMarker(canvas, startPoint, START_COLOR, "A")
        drawMarker(canvas, endPoint, END_COLOR, "B")
    }

    private fun drawMarker(canvas: Canvas, sourcePoint: PointF?, color: Int, label: String) {
        if (sourcePoint == null || decoder == null) return

        val viewPoint = sourceToView(sourcePoint)
        val radius = 24f

        markerCirclePaint.color = color
        canvas.drawCircle(viewPoint.x, viewPoint.y, radius, markerCirclePaint)

        val textBaseline = viewPoint.y - (markerTextPaint.ascent() + markerTextPaint.descent()) / 2f
        canvas.drawText(label, viewPoint.x, textBaseline, markerTextPaint)
    }

    private fun drawVisibleRegion(canvas: Canvas) {
        val currentDecoder = decoder ?: return
        if (width <= 0 || height <= 0) return

        drawPreview(canvas)

        val srcLeft = max(0f, -offsetX / scale)
        val srcTop = max(0f, -offsetY / scale)
        val srcRight = min(mapWidth.toFloat(), (width - offsetX) / scale)
        val srcBottom = min(mapHeight.toFloat(), (height - offsetY) / scale)

        if (srcRight <= srcLeft || srcBottom <= srcTop) return

        val desiredSample = calculateInSampleSize(scale)
        val sample = if (isInteracting) min(desiredSample * 2, 8) else desiredSample
        val desiredRegion = makeQuantizedRegion(
            left = srcLeft,
            top = srcTop,
            right = srcRight,
            bottom = srcBottom,
            sample = sample,
        )

        tryLoadTileFromDisk(desiredRegion, sample)

        if (shouldDecode(desiredRegion, sample) && shouldRequestDecodeNow()) {
            requestDecode(currentDecoder, desiredRegion, sample)
        }

        val tile = cachedBitmap ?: return
        val tileRect = cachedRect ?: return
        val dstLeft = offsetX + tileRect.left * scale
        val dstTop = offsetY + tileRect.top * scale
        val dstRight = offsetX + tileRect.right * scale
        val dstBottom = offsetY + tileRect.bottom * scale

        canvas.drawBitmap(tile, null, Rect(dstLeft.toInt(), dstTop.toInt(), dstRight.toInt(), dstBottom.toInt()), null)
    }

    private fun drawPreview(canvas: Canvas) {
        val preview = previewBitmap ?: return
        val dstLeft = offsetX
        val dstTop = offsetY
        val dstRight = offsetX + mapWidth * scale
        val dstBottom = offsetY + mapHeight * scale
        canvas.drawBitmap(preview, null, Rect(dstLeft.toInt(), dstTop.toInt(), dstRight.toInt(), dstBottom.toInt()), null)
    }

    private fun shouldRequestDecodeNow(): Boolean {
        if (!isInteracting) return true
        val now = SystemClock.uptimeMillis()
        val allowed = now - lastDecodeRequestAtMs >= INTERACTION_DECODE_THROTTLE_MS
        if (allowed) {
            lastDecodeRequestAtMs = now
        }
        return allowed
    }

    private fun tryLoadTileFromDisk(region: Rect, sample: Int) {
        val cachedMatches = cachedRect == region && cachedSample == sample && cachedBitmap?.isRecycled == false
        if (cachedMatches) return

        val key = buildTileCacheKey(region, sample)
        if (lastDiskMissKey == key) return

        val file = tileCacheFile(region, sample)
        val loaded = synchronized(diskCacheLock) {
            if (!file.exists()) {
                null
            } else {
                BitmapFactory.decodeFile(file.absolutePath)?.also {
                    file.setLastModified(System.currentTimeMillis())
                }
            }
        }

        if (loaded == null) {
            lastDiskMissKey = key
            return
        }

        cachedBitmap?.recycle()
        cachedBitmap = loaded
        cachedRect = Rect(region)
        cachedSample = sample
        lastDiskMissKey = null
    }

    private fun tileCacheFile(region: Rect, sample: Int): File {
        val dir = File(context.cacheDir, TILE_DISK_CACHE_DIR)
        return File(dir, "${buildTileCacheKey(region, sample)}.png")
    }

    private fun buildTileCacheKey(region: Rect, sample: Int): String {
        val raw = "map:$currentMapResId:${mapWidth}x${mapHeight}:${region.left},${region.top},${region.right},${region.bottom}:s$sample"
        val md5 = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return md5.joinToString("") { "%02x".format(it) }
    }

    private fun saveTileToDisk(region: Rect, sample: Int, bitmap: Bitmap) {
        synchronized(diskCacheLock) {
            val dir = File(context.cacheDir, TILE_DISK_CACHE_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val target = tileCacheFile(region, sample)
            val temp = File(target.absolutePath + ".tmp")

            runCatching {
                FileOutputStream(temp).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                if (target.exists()) {
                    target.delete()
                }
                temp.renameTo(target)
                target.setLastModified(System.currentTimeMillis())
                trimDiskCacheLocked(dir)
            }.onFailure {
                temp.delete()
            }
        }
    }

    private fun trimDiskCacheLocked(dir: File) {
        val files = dir.listFiles()?.toMutableList() ?: return
        var total = files.sumOf { it.length() }
        if (total <= TILE_DISK_CACHE_MAX_BYTES) return

        files.sortBy { it.lastModified() }
        for (file in files) {
            if (total <= TILE_DISK_CACHE_MAX_BYTES) break
            val len = file.length()
            if (file.delete()) {
                total -= len
            }
        }
    }

    private fun warmupInitialTile() {
        val currentDecoder = decoder ?: return
        if (width <= 0 || height <= 0) return

        val srcLeft = max(0f, -offsetX / scale)
        val srcTop = max(0f, -offsetY / scale)
        val srcRight = min(mapWidth.toFloat(), (width - offsetX) / scale)
        val srcBottom = min(mapHeight.toFloat(), (height - offsetY) / scale)
        if (srcRight <= srcLeft || srcBottom <= srcTop) return

        val sample = max(2, calculateInSampleSize(scale))
        val region = makeQuantizedRegion(srcLeft, srcTop, srcRight, srcBottom, sample)
        requestDecode(currentDecoder, region, sample)
    }

    private fun shouldDecode(region: Rect, sample: Int): Boolean {
        val cachedMatches = cachedRect == region && cachedSample == sample && cachedBitmap?.isRecycled == false
        if (cachedMatches) return false

        val pending = pendingRequest
        return pending == null || pending.region != region || pending.sample != sample
    }

    private fun requestDecode(currentDecoder: BitmapRegionDecoder, region: Rect, sample: Int) {
        val request = DecodeRequest(
            region = Rect(region),
            sample = sample,
            generation = decoderGeneration.get(),
        )
        pendingRequest = request

        decoderExecutor.execute {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = request.sample
            }
            val decoded = try {
                currentDecoder.decodeRegion(request.region, options)
            } catch (_: Throwable) {
                null
            }

            if (decoded != null) {
                saveTileToDisk(request.region, request.sample, decoded)
            }

            post {
                if (request.generation != decoderGeneration.get()) {
                    decoded?.recycle()
                    return@post
                }

                if (pendingRequest != request) {
                    decoded?.recycle()
                    return@post
                }

                pendingRequest = null
                if (decoded == null) return@post

                cachedBitmap?.recycle()
                cachedBitmap = decoded
                cachedRect = Rect(request.region)
                cachedSample = request.sample
                lastDiskMissKey = null
                previewBitmap?.recycle()
                previewBitmap = null
                postInvalidateOnAnimation()
            }
        }
    }

    private fun makeQuantizedRegion(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        sample: Int,
    ): Rect {
        val step = max(128, 256 * sample)
        val padding = step / 2

        var l = floorToStep(left.toInt() - padding, step)
        var t = floorToStep(top.toInt() - padding, step)
        var r = ceilToStep(right.toInt() + padding, step)
        var b = ceilToStep(bottom.toInt() + padding, step)

        l = l.coerceIn(0, mapWidth)
        t = t.coerceIn(0, mapHeight)
        r = r.coerceIn(l + 1, mapWidth)
        b = b.coerceIn(t + 1, mapHeight)

        return Rect(l, t, r, b)
    }

    private fun floorToStep(value: Int, step: Int): Int {
        if (step <= 0) return value
        return (value / step) * step
    }

    private fun ceilToStep(value: Int, step: Int): Int {
        if (step <= 0) return value
        return ((value + step - 1) / step) * step
    }

    private fun calculateInSampleSize(currentScale: Float): Int {
        val ratio = (1f / currentScale).coerceAtLeast(1f)
        val power = max(0.0, ceil(ln(ratio.toDouble()) / ln(2.0))).toInt()
        return 2.0.pow(power.toDouble()).toInt().coerceAtLeast(1)
    }

    private fun viewToSource(viewX: Float, viewY: Float): PointF? {
        if (decoder == null) return null
        val srcX = ((viewX - offsetX) / scale).coerceIn(0f, mapWidth.toFloat())
        val srcY = ((viewY - offsetY) / scale).coerceIn(0f, mapHeight.toFloat())
        return PointF(srcX, srcY)
    }

    private fun sourceToView(source: PointF): PointF {
        return PointF(
            source.x * scale + offsetX,
            source.y * scale + offsetY,
        )
    }

    private fun resetToFit() {
        if (width <= 0 || height <= 0) return
        val fitScaleX = width / mapWidth.toFloat()
        val fitScaleY = height / mapHeight.toFloat()
        minScale = min(fitScaleX, fitScaleY)
        scale = minScale.coerceAtLeast(0.01f)

        val contentW = mapWidth * scale
        val contentH = mapHeight * scale
        offsetX = (width - contentW) / 2f
        offsetY = (height - contentH) / 2f
    }

    private fun clampOffsets() {
        val contentW = mapWidth * scale
        val contentH = mapHeight * scale

        offsetX = if (contentW <= width) {
            (width - contentW) / 2f
        } else {
            offsetX.coerceIn(width - contentW, 0f)
        }

        offsetY = if (contentH <= height) {
            (height - contentH) / 2f
        } else {
            offsetY.coerceIn(height - contentH, 0f)
        }
    }

    private fun recycleDecoder() {
        decoderGeneration.incrementAndGet()
        removeCallbacks(redrawOnIdle)
        isInteracting = false
        pendingRequest = null
        lastDiskMissKey = null
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedRect = null
        previewBitmap?.recycle()
        previewBitmap = null
        decoder?.recycle()
        decoder = null
    }

    override fun onDetachedFromWindow() {
        recycleDecoder()
        decoderExecutor.shutdownNow()
        super.onDetachedFromWindow()
    }
}

