package codeescape.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import codeescape.core.animation.RunnerAnimationState
import codeescape.android.render.SpriteAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Asset-backed runner transition using shared timing/easing/frame sequencing.
 * Scales character and scene to available screen size while preserving animation behavior.
 */
@Composable
fun AssetRunningTransition(
    isGirl: Boolean,
    variantIndex: Int,
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    var frames by remember(isGirl, variantIndex) { mutableStateOf<List<Bitmap>?>(null) }

    LaunchedEffect(isGirl, variantIndex) {
        frames = withContext(Dispatchers.IO) {
            val runFile = SpriteAssets.runnerOutfitFile(isGirl, variantIndex)
            SpriteAssets.loadIsolatedWalkFrames(context, runFile)
                ?: SpriteAssets.loadIsolatedWalkFrames(context, if (isGirl) "modern_girl_walk.png" else "modern_boy_walk.png")
        }
    }

    val frameList = frames
    val frameCount = max(1, frameList?.size ?: 1)
    val state = remember(frameCount, isGirl, variantIndex) { RunnerAnimationState(frameCount) }
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    var finishedNotified by remember { mutableStateOf(false) }

    LaunchedEffect(state, frameList) {
        val start = System.nanoTime()
        state.start(start)
        finishedNotified = false
        while (true) {
            now = System.nanoTime()
            if (state.finished(now)) {
                if (!finishedNotified) { finishedNotified = true; onFinished() }
                break
            }
            delay(16L)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF132B48), size = size)

        val pose = frameList?.getOrNull(state.frame(now) % frameCount)
        val srcW = (pose?.width ?: 100).toFloat()
        val srcH = (pose?.height ?: 180).toFloat()
        val aspect = srcW / srcH

        // Scaled to viewport (parity with desktop: compact runner, proportional to available space)
        val maxH = min(h * 0.38f, 190f)
        val minH = max(h * 0.18f, 110f)
        val displayHeight = max(minH, min(maxH, h / 3.2f))
        val displayWidth = max(48f, displayHeight * aspect)

        val x = state.x(now, w.toDouble(), displayWidth.toDouble()).toFloat()
        val y = (h - displayHeight - (h * 0.04f)).coerceAtLeast(0f)
        val progressEased = state.easedProgress(now).toFloat()

        // Shadow
        drawOval(
            color = Color(0x5A030D1C),
            topLeft = Offset(x + (displayWidth * 0.17f), h - (h * 0.045f)),
            size = Size(displayWidth * 0.66f, (h * 0.016f).coerceAtLeast(5f))
        )

        if (pose != null) {
            val img = pose.asImageBitmap()
            drawImage(
                image = img,
                dstOffset = IntOffset(x.toInt(), y.toInt()),
                dstSize = IntSize(displayWidth.toInt().coerceAtLeast(1), displayHeight.toInt().coerceAtLeast(1))
            )
        } else {
            drawRoundRect(
                color = Color(0xFF69E5F5),
                topLeft = Offset(x, y),
                size = Size(displayWidth, displayHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(displayWidth * 0.2f, displayWidth * 0.2f)
            )
        }

        val barH = (h * 0.012f).coerceIn(6f, 10f)
        val barY = h - barH - (h * 0.02f)
        val pad = w * 0.06f
        val barW = (w - pad * 2).coerceAtLeast(10f)
        drawRoundRect(color = Color(0x30FFFFFF), topLeft = Offset(pad, barY), size = Size(barW, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH, barH))
        drawRoundRect(color = Color(0xFF5AF5E6), topLeft = Offset(pad, barY), size = Size(barW * progressEased, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH, barH))
    }
}
