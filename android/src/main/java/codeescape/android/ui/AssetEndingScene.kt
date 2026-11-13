package codeescape.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import codeescape.android.render.SpriteAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Asset-backed ending scene: victory pose + clickable key -> flight -> chest opening sequence.
 * Timing/sequence mirrors desktop EndingScene intent (720ms flight, 760ms opening, 550ms settle).
 * Screen-scaled rendering; choreography/dialogs are composed by parent UI.
 */
@Composable
fun AssetEndingScene(
    isGirl: Boolean,
    variantIndex: Int,
    initialChestOpened: Boolean = false,
    onOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    var victory by remember { mutableStateOf<Bitmap?>(null) }
    var keyBmp by remember { mutableStateOf<Bitmap?>(null) }
    var closedChest by remember { mutableStateOf<Bitmap?>(null) }
    var chestBody by remember { mutableStateOf<Bitmap?>(null) }
    var chestLid by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(isGirl, variantIndex) {
        withContext(Dispatchers.IO) {
            victory = SpriteAssets.loadBitmap(context, SpriteAssets.victoryPoseFile(isGirl, variantIndex))
                ?: SpriteAssets.loadBitmap(context, SpriteAssets.standingOrVariantFile(isGirl, variantIndex))
            keyBmp = SpriteAssets.loadBitmap(context, "golden_key.png")
            closedChest = SpriteAssets.loadBitmap(context, "treasure_chest_real.png")
            chestBody = SpriteAssets.loadBitmap(context, "treasure_chest_body.png")
            chestLid = SpriteAssets.loadBitmap(context, "treasure_chest_lid.png")
        }
    }

    // stages: 0 waiting, 1 flying, 2 opening, 3 opened
    var stage by remember { mutableIntStateOf(if (initialChestOpened) 3 else 0) }
    var stageStartNanos by remember { mutableLongStateOf(System.nanoTime()) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var keyProgress by remember { mutableFloatStateOf(if (initialChestOpened) 1f else 0f) }
    var lidAngle by remember { mutableFloatStateOf(if (initialChestOpened) 1f else 0f) }
    var openedNotified by remember { mutableStateOf(initialChestOpened) }
    var keyBoundsState by remember { mutableStateOf(Rect.Zero) }
    var keyStart by remember { mutableStateOf(Offset.Zero) }
    var lockTarget by remember { mutableStateOf(Offset.Zero) }
    val openedCb by rememberUpdatedState(onOpened)

    LaunchedEffect(stage) {
        stageStartNanos = System.nanoTime()
        if (stage == 1) keyProgress = 0f
        if (stage == 2) lidAngle = 0f
        while (stage == 0 || stage == 1 || stage == 2) {
            frameTick = System.nanoTime()
            val elapsed = (frameTick - stageStartNanos) / 1_000_000.0
            when (stage) {
                0 -> { /* animate key pop via frameTick */ }
                1 -> {
                    val t = min(1.0, elapsed / 720.0).toFloat()
                    keyProgress = t
                    if (t >= 1f) stage = 2
                }
                2 -> {
                    val t = min(1.0, elapsed / 760.0)
                    lidAngle = (t * t * (3.0 - 2.0 * t)).toFloat()
                    if (lidAngle >= 1f) stage = 3
                }
            }
            delay(16L)
        }
        if (stage == 3 && !openedNotified) {
            openedNotified = true
            // parity timing: desktop fires onTreasureOpen ~550ms after chest fully opens
            delay(550)
            try { openedCb() } catch (_: Exception) {}
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { pos ->
            if (stage == 0 && keyBoundsState.contains(Offset(pos.x, pos.y))) {
                keyStart = Offset(keyBoundsState.left, keyBoundsState.top)
                stage = 1
            }
        }
    }) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF0B1D3A), size = size)

        // Reserve top space for overlay title/instructions so chest does not cover them.
        val topSafe = max(120f, h * 0.24f)

        val chestImg = closedChest
        val vw = min(max(140f, w * 0.24f), max(140f, w - 48f))
        val vh = if (chestImg == null) max(120f, h * 0.28f) else max(120f, vw * chestImg.height / chestImg.width.toFloat())
        val vx = (w - vw) / 2f
        val vy = topSafe
        val lockX = vx + vw / 2f
        val lockY = vy + vh * 0.49f
        lockTarget = Offset(lockX - 18f, lockY - 12f)

        fun drawBmp(b: Bitmap?, x: Float, y: Float, dw: Float, dh: Float) {
            if (b == null) return
            drawImage(b.asImageBitmap(), dstOffset = IntOffset(x.toInt(), y.toInt()), dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)))
        }

        if (stage >= 2) {
            val body = chestBody
            if (body != null) drawBmp(body, vx, vy + vh * 0.12f, vw, vh * 0.88f) else drawBmp(closedChest, vx, vy, vw, vh)
            val openingY = vy + vh * 0.17f
            val openingH = max(16f, vh * 0.22f)
            drawRoundRect(Color(0xEB2D150D), topLeft = Offset(vx + vw * 0.13f, openingY), size = Size(vw * 0.74f, openingH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
            if (chestLid != null) {
                val lidH = max(35f, vh * 0.42f)
                val lift = lidAngle * lidH * 0.82f
                // Desktop parity: lid lifts and rotates up to -9 degrees around its hinge point.
                withTransform({
                    rotate(-9f * lidAngle, pivot = Offset(vx + vw / 2f, vy + lidH * 0.78f))
                }) {
                    drawBmp(chestLid, vx, vy - lift, vw, lidH)
                }
            }
            val glowA = (35 + 90 * lidAngle).toInt().coerceIn(0, 255)
            drawOval(Color(255, 211, 65, glowA), topLeft = Offset(vx - 12f, vy - 8f), size = Size(vw + 24f, vh + 36f))
            val sparkCount = 14
            for (i in 0 until sparkCount) {
                val a = i * Math.PI / 7.0
                val sx = lockX + (Math.cos(a) * (25 + lidAngle * 55)).toFloat()
                val sy = vy + vh / 3f + (Math.sin(a) * (18 + lidAngle * 36)).toFloat()
                drawCircle(Color(0xFFFFE77D), radius = 3f, center = Offset(sx, sy))
            }
        } else {
            drawBmp(closedChest, vx, vy, vw, vh)
        }

        val v = victory
        val charMaxW = max(90f, w * 0.22f)
        val charMaxH = max(140f, h * 0.46f)
        val (charW, charH) = if (v != null) {
            val scale = min(charMaxW / v.width, charMaxH / v.height)
            max(1f, v.width * scale) to max(1f, v.height * scale)
        } else {
            charMaxW to charMaxH
        }
        val charX = (w - charW - w * 0.06f).coerceAtLeast(w * 0.52f)
        val charY = h - charH - h * 0.03f
        if (v != null) {
            // Smaller character placed bottom-right so it does not cover the centered chest.
            drawBmp(v, charX, charY, charW, charH)
            for (i in 0 until 12) {
                val a = i * Math.PI / 6.0
                val sx = charX + charW * 0.5f + (Math.cos(a) * charW * 0.52).toFloat()
                val sy = charY + charH / 3f + (Math.sin(a) * charH * 0.20).toFloat()
                drawCircle(Color(255, 226, 125, 210), radius = 2.2f, center = Offset(sx, sy))
            }
        }

        if (stage == 0 || stage == 1) {
            val kw = max(42f, w * 0.075f)
            val kh = max(26f, h * 0.075f)
            // Place key on the character (near upper body/hand area) so it is clearly held/carried.
            val kxBase = (charX + charW * 0.18f).coerceIn(8f, (w - kw - 8f).coerceAtLeast(8f))
            val popT = min(1.0, ((frameTick - stageStartNanos).coerceAtLeast(0L) / 420_000_000.0)).toFloat()
            val pop = if (stage == 0) (sin(popT * Math.PI / 2.0).toFloat() * min(18f, h * 0.04f)) else 0f
            val kyBase = (charY + charH * 0.28f - pop).coerceIn(8f, (h - kh - 8f).coerceAtLeast(8f))
            val eased = keyProgress * keyProgress * (3f - 2f * keyProgress)
            val kx = if (stage == 1) keyStart.x + (lockTarget.x - keyStart.x) * eased else kxBase
            val ky = if (stage == 1) keyStart.y + (lockTarget.y - keyStart.y) * eased else kyBase
            val kb = Rect(kxBase, kyBase, kxBase + kw, kyBase + kh)
            keyBoundsState = if (stage == 0) kb else Rect.Zero
            val kbmp = keyBmp
            if (kbmp != null) {
                // Desktop drawImageFit parity: preserve the key's aspect ratio within the tap bounds.
                val fitScale = min(kw / kbmp.width.toFloat(), kh / kbmp.height.toFloat())
                val dw = max(1f, kbmp.width * fitScale)
                val dh = max(1f, kbmp.height * fitScale)
                drawBmp(kbmp, kx + (kw - dw) / 2f, ky + (kh - dh) / 2f, dw, dh)
            } else {
                drawCircle(Color(0xFFFFD700), radius = kw / 3f, center = Offset(kx + kw / 2f, ky + kh / 2f))
            }
            if (stage == 0) {
                drawRoundRect(Color(0xFFFFDC5A), topLeft = Offset(kb.left - 4f, kb.top - 4f), size = Size(kb.width + 8f, kb.height + 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f), style = Stroke(width = 2f))
            }
        }
    }
}
