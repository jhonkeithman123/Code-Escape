package codeescape.android.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.BufferedInputStream
import java.util.concurrent.ConcurrentHashMap

object SpriteAssets {
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    private val walkFrameCache = ConcurrentHashMap<String, List<Bitmap>>()

    /**
     * Loads a bundled image, cached by name. Prefer [loadSampledBitmap] for large
     * artwork so the decoded size matches what is actually displayed.
     */
    fun loadBitmap(context: Context, name: String): Bitmap? {
        return bitmapCache.getOrPut(name) { decode(context, name, 0, 0) }
    }

    /**
     * Loads a bundled image downsampled to roughly the requested dimensions.
     * Uses BitmapFactory's inSampleSize so full-size artwork never sits in memory.
     */
    fun loadSampledBitmap(context: Context, name: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (reqWidth <= 0 || reqHeight <= 0) return loadBitmap(context, name)
        val key = "$name@${reqWidth}x${reqHeight}"
        return bitmapCache.getOrPut(key) { decode(context, name, reqWidth, reqHeight) }
    }

    private fun decode(context: Context, name: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            if (reqWidth <= 0 || reqHeight <= 0) {
                context.assets.open(name).use { input ->
                    BitmapFactory.decodeStream(BufferedInputStream(input))
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(name).use { input ->
                    BitmapFactory.decodeStream(BufferedInputStream(input), null, bounds)
                }
                val options = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.assets.open(name).use { input ->
                    BitmapFactory.decodeStream(BufferedInputStream(input), null, options)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun computeSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var sample = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / sample >= reqHeight && halfWidth / sample >= reqWidth) {
                sample *= 2
            }
        }
        return sample
    }

    fun runnerOutfitFile(isGirl: Boolean, variantIndex: Int): String {
        val gender = if (isGirl) "girl" else "boy"
        val variant = (variantIndex + 1).coerceIn(1, 4)
        // Prefer color-specific run sheet; fallback to generic walk/variant handled by caller if needed.
        return "modern_${gender}_run_v${variant}.png"
    }

    fun standingOrVariantFile(isGirl: Boolean, variantIndex: Int): String {
        val gender = if (isGirl) "girl" else "boy"
        val variant = (variantIndex + 1).coerceIn(1, 4)
        return "modern_${gender}_variant${variant}.png"
    }

    fun victoryPoseFile(isGirl: Boolean, variantIndex: Int): String {
        val gender = if (isGirl) "girl" else "boy"
        val colors = arrayOf("green", "blue", "purple", "orange")
        val idx = variantIndex.coerceIn(0, 3)
        return "victory_${gender}_${colors[idx]}.png"
    }

    /**
     * Port of desktop loadIsolatedWalkFrames: trims each authored pose to alpha bounds
     * and places it on a shared bottom-aligned transparent canvas.
     *
     * Frames are cached per source file and the alpha-bound scan uses a single
     * getPixels pass instead of per-pixel getPixel calls (much faster on device).
     */
    fun loadIsolatedWalkFrames(context: Context, file: String): List<Bitmap>? {
        if (!Regex("modern_(boy|girl)_(walk(_v[1-4])?|run_v[1-4])\\.png").matches(file)) return null
        walkFrameCache[file]?.let { return it }

        val source = loadBitmap(context, file) ?: return null
        val count = when {
            source.width == 2240 && source.height == 380 -> 8
            source.width == 384 && (source.height == 365 || source.height == 390) -> 4
            else -> return null
        }
        val cellWidth = source.width / count
        val bounds = arrayOfNulls<Rect>(count)
        var canvasWidth = 1
        var canvasHeight = 1

        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

        for (i in 0 until count) {
            var minX = cellWidth; var minY = source.height
            var maxX = -1; var maxY = -1
            val startX = i * cellWidth
            for (y in 0 until source.height) {
                val rowOffset = y * source.width
                for (x in startX until (i + 1) * cellWidth) {
                    val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
                    if (alpha > 8) {
                        val lx = x - startX
                        if (lx < minX) minX = lx
                        if (y < minY) minY = y
                        if (lx > maxX) maxX = lx
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (maxX < minX || maxY < minY) return null
            val b = Rect(minX, minY, maxX, maxY)
            bounds[i] = b
            canvasWidth = maxOf(canvasWidth, b.width() + 1)
            canvasHeight = maxOf(canvasHeight, b.height() + 1)
        }

        val out = ArrayList<Bitmap>(count)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        for (i in 0 until count) {
            val b = bounds[i]!!
            val frame = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            frame.eraseColor(Color.TRANSPARENT)
            val c = Canvas(frame)
            val src = Rect(i * cellWidth + b.left, b.top, i * cellWidth + b.right + 1, b.bottom + 1)
            val dx = (canvasWidth - src.width()) / 2
            val dy = canvasHeight - src.height()
            val dst = Rect(dx, dy, dx + src.width(), dy + src.height())
            c.drawBitmap(source, src, dst, paint)
            out.add(frame)
        }
        walkFrameCache[file] = out
        return out
    }
}
