package codeescape.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import codeescape.core.animation.RunnerAnimationState
import kotlinx.coroutines.delay

/**
 * Android animation-parity renderer: uses the same shared timing/easing/frame sequencing
 * as desktop and scales the character/scene proportionally to the available screen size.
 */
@Composable
fun RunningTransitionCanvas(
    frameCount: Int = 4,
    backgroundColor: Color = Color(0xFF132B48),
    drawCharacterFrame: (DrawScope.(frame: Int, x: Float, y: Float, w: Float, h: Float) -> Unit)? = null,
    onFinished: () -> Unit = {}
) {
    val state = remember(frameCount) { RunnerAnimationState(frameCount) }
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    var finishedNotified by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        val start = System.nanoTime()
        state.start(start)
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
        drawRect(color = backgroundColor, size = size)

        // Scale character relative to viewport (same behavior intent as desktop, scaled to screen).
        val displayHeight = minOf(h * 0.32f, 190f * (h / 480f).coerceAtLeast(0.6f)).coerceIn(90f, h * 0.45f)
        val aspect = 0.55f // nominal sprite aspect; real renderer should use frame bitmap aspect
        val displayWidth = (displayHeight * aspect).coerceAtLeast(48f)
        val progressEased = state.easedProgress(now).toFloat()
        val x = state.x(now, w.toDouble(), displayWidth.toDouble()).toFloat()
        val y = (h - displayHeight - (h * 0.03f)).coerceAtLeast(0f)
        val frame = state.frame(now)

        // Shadow
        drawOval(
            color = Color(0x5A030D1C),
            topLeft = Offset(x + (displayWidth * 0.17f), h - (h * 0.05f)),
            size = Size(displayWidth * 0.66f, (h * 0.015f).coerceAtLeast(4f))
        )

        if (drawCharacterFrame != null) {
            drawCharacterFrame(frame, x, y, displayWidth, displayHeight)
        } else {
            // Placeholder body so animation motion/timing/scale can be verified without assets wired.
            drawRoundRect(
                color = Color(0xFF69E5F5),
                topLeft = Offset(x, y),
                size = Size(displayWidth, displayHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(displayWidth * 0.2f, displayWidth * 0.2f)
            )
        }

        // Progress bar synced to eased progress (same as desktop fix)
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
