package codeescape.android.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.BufferedInputStream

object SpriteAssets {
    fun loadBitmap(context: Context, name: String): Bitmap? {
        return try {
            context.assets.open(name).use { input ->
                BitmapFactory.decodeStream(BufferedInputStream(input))
            }
        } catch (_: Exception) {
            null
        }
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
     */
    fun loadIsolatedWalkFrames(context: Context, file: String): List<Bitmap>? {
        if (!Regex("modern_(boy|girl)_(walk(_v[1-4])?|run_v[1-4])\\.png").matches(file)) return null
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

        for (i in 0 until count) {
            var minX = cellWidth; var minY = source.height
            var maxX = -1; var maxY = -1
            val startX = i * cellWidth
            for (y in 0 until source.height) {
                for (x in startX until (i + 1) * cellWidth) {
                    val alpha = (source.getPixel(x, y) ushr 24) and 0xFF
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
        return out
    }
}
