package com.wyprompter.ui

import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Color as AndroidColor
import com.wyprompter.scroll.AutoScrollEngine

private const val SPEED_SCALE = 80f

/**
 * In-app teleprompter (非悬浮窗模式).
 *
 * The script shows inside a centered box (提词框) of a fraction of the screen —
 * not fullscreen — with fixed control bars on top and bottom so buttons never
 * cover the text. Reuses the same scroll engine as the overlay. Bluetooth
 * remote keys are routed here by [MainActivity.dispatchKeyEvent] through the
 * [InAppPrompterController.instance] singleton — no accessibility/focus needed
 * because the teleprompter activity is the focused window.
 */
@Composable
fun InAppPrompterScreen(
    title: String,
    content: String,
    fontSize: Float,
    scrollSpeed: Float,
    fontColor: Int,
    backgroundColor: Int,
    autoScroll: Boolean,
    onExit: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    var controller by remember { mutableStateOf<InAppPrompterController?>(null) }

    BackHandler(onBack = onExit)

    DisposableEffect(Unit) {
        onDispose { controller?.release() }
    }

    val boxBg = if (backgroundColor == AndroidColor.TRANSPARENT) AndroidColor.BLACK else backgroundColor
    val screenRunning = controller?.running ?: false
    val progress = controller?.progressPercent ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onExit) { Text("返回") }
            Spacer(Modifier.width(8.dp))
            Text(
                title.ifBlank { "无忧提词器" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 提词框: fills the whole area between the top bar and the bottom
        // controls (edge-to-edge, no whitespace around it).
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(Color(boxBg))
        ) {
            AndroidView(
                    factory = { ctx ->
                        val textView = TextView(ctx).apply {
                            setTextColor(fontColor)
                            textSize = fontSize
                        }
                        val scroll = ScrollView(ctx).apply {
                            setPadding(0, (10 * ctx.resources.displayMetrics.density).toInt(), 0, 0)
                            addView(
                                textView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            )
                            setVerticalScrollBarEnabled(true)
                        }
                        val ctrl = InAppPrompterController(textView, scroll) { controller?.speed ?: (scrollSpeed * SPEED_SCALE) }
                        ctrl.speed = scrollSpeed * SPEED_SCALE
                        ctrl.setContent(content)
                        if (autoScroll) ctrl.start()
                        controller = ctrl
                        InAppPrompterController.instance = ctrl
                        scroll
                    },
                    modifier = Modifier.fillMaxSize()
                )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { controller?.previous() },
                    modifier = Modifier.weight(1f)
                ) { Text("上一屏") }
                Button(
                    onClick = { controller?.toggle() },
                    modifier = Modifier.weight(1f)
                ) { Text(if (screenRunning) "暂停" else "继续") }
                OutlinedButton(
                    onClick = { controller?.next() },
                    modifier = Modifier.weight(1f)
                ) { Text("下一屏") }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val raw = (controller?.speed ?: (scrollSpeed * SPEED_SCALE)) /
                            SPEED_SCALE
                        val s = raw.coerceAtLeast(0.1f + 0.2f) - 0.2f
                        controller?.speed = s * SPEED_SCALE
                        onSpeedChange(s)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("减速") }
                OutlinedButton(
                    onClick = {
                        val raw = (controller?.speed ?: (scrollSpeed * SPEED_SCALE)) /
                            SPEED_SCALE
                        val s = raw.coerceAtMost(5f - 0.2f) + 0.2f
                        controller?.speed = s * SPEED_SCALE
                        onSpeedChange(s)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("加速") }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$progress%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Drives the in-app teleprompter view. The singleton
 * [instance] is set while this screen is on screen so that
 * [MainActivity.dispatchKeyEvent] can route Bluetooth remote keys here.
 */
class InAppPrompterController(
    private val textView: TextView,
    private val viewport: ScrollView,
    speedProvider: () -> Float
) {
    val engine = AutoScrollEngine(textView, viewport, speedProvider)

    var speed: Float = 1f
    var running by mutableStateOf(false)
        private set
    var progressPercent by mutableIntStateOf(0)
        private set

    init {
        engine.onScrollEnd = { running = false }
        engine.onProgress = { p -> progressPercent = (p * 100).toInt() }
    }

    fun setContent(content: String) {
        textView.text = content
        viewport.scrollTo(0, 0)
        progressPercent = 0
    }

    fun toggle() {
        if (running) {
            running = false
            engine.pause()
        } else {
            running = true
            engine.start()
        }
    }

    fun start() {
        running = true
        engine.start()
    }

    fun next() {
        val wasRunning = running
        engine.stop(reportEnd = false)
        val max = engine.maxScrollOffset().toInt()
        viewport.scrollTo(0, snapToLine(viewport.scrollY + viewport.height, max))
        updateProgress()
        if (wasRunning) engine.start()
    }

    fun previous() {
        val wasRunning = running
        engine.stop(reportEnd = false)
        val max = engine.maxScrollOffset().toInt()
        viewport.scrollTo(0, snapToLine(viewport.scrollY - viewport.height, max))
        updateProgress()
        if (wasRunning) engine.start()
    }

    /** Align the scroll target so the new top edge is a whole line, never a cut line. */
    private fun snapToLine(target: Int, max: Int): Int {
        val lineH = textView.lineHeight
        if (lineH <= 0) return target.coerceIn(0, max)
        return ((target / lineH) * lineH).coerceIn(0, max)
    }

    private fun updateProgress() {
        val max = engine.maxScrollOffset()
        progressPercent = if (max > 0f) {
            ((viewport.scrollY / max) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    fun release() {
        engine.release()
        if (instance === this) instance = null
    }

    companion object {
        @Volatile
        var instance: InAppPrompterController? = null
    }
}