package com.wyprompter.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.wyprompter.MainActivity
import com.wyprompter.R
import com.wyprompter.core.NotificationHelper
import com.wyprompter.core.PermissionHelper
import com.wyprompter.data.SettingsRepository

/**
 * Foreground service that hosts the floating teleprompter overlay.
 *
 * The overlay is split into two windows so the content area stays touch-through
 * (recording controls keep working underneath) while the control bar remains
 * fully interactive:
 *
 *  - Content window: shows the scrolling text. Created with
 *    NOT_FOCUSABLE | NOT_TOUCHABLE so every tap passes through to the app below.
 *  - Control bar window: separate overlay that sits at the bottom edge of the
 *    content box and receives touches (NOT_FOCUSABLE | NOT_TOUCH_MODAL).
 *
 * Dragging moves both windows together; the resize button resizes the content
 * box and re-anchors the bar.
 */
class TeleprompterOverlayService : Service() {

    private lateinit var settings: SettingsRepository
    private lateinit var windowManager: WindowManager

    private var contentRoot: FrameLayout? = null
    private var barRoot: FrameLayout? = null
    private var controlBar: View? = null
    private var contentController: OverlayContentController? = null
    private var contentParams: WindowManager.LayoutParams? = null
    private var barParams: WindowManager.LayoutParams? = null

    private var lastX = 0
    private var lastY = 0
    private var isDraggingBar = false
    private var isResizingBar = false

    /** Whether auto-scroll should currently be running (toggled by play/pause). */
    private var autoScrollRequested = false

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        NotificationHelper.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationHelper.NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (PermissionHelper.canDrawOverlays(this)) {
            if (contentRoot == null) show(content = readContent())
            else {
                updateContent(content = readContent())
                contentController?.let { controller ->
                    applyStyle(controller, contentRoot!!)
                    resizeFromSettings()
                    if (settings.autoScroll) {
                        // Post so the new text is measured before computing range.
                        contentRoot?.post { startEngine(controller) }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun readContent(): String {
        return readManuscript(settings.currentManuscriptId)
    }

    private fun readManuscript(id: Long): String {
        // Read synchronously on main thread is acceptable here for small content;
        // Room is queried via runBlocking to extract text for the overlay.
        if (id <= 0) return ""
        return kotlinx.coroutines.runBlocking {
            com.wyprompter.data.AppDatabase.getInstance(this@TeleprompterOverlayService)
                .manuscriptDao().getById(id)?.content ?: ""
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun show(content: String) {
        val inflater = LayoutInflater.from(this)

        val contentRoot = inflater.inflate(R.layout.overlay_content, null) as FrameLayout
        val controller = OverlayContentController(contentRoot) { settings.scrollSpeed * 80f }
        controller.setContent(content)

        val controlBar = inflater.inflate(R.layout.overlay_control_bar, null) as LinearLayout
        val barRoot = FrameLayout(this)
        barRoot.addView(controlBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        val dm = resources.displayMetrics
        val preset = settings.overlayPresetSize
        val w: Int
        val h: Int
        if (preset >= 0) {
            w = dm.widthPixels
            h = (dm.heightPixels * presetFraction(preset)).toInt().coerceAtLeast(1)
        } else {
            w = settings.overlayWidth.takeIf { it > 0 } ?: dm.widthPixels
            h = settings.overlayHeight.takeIf { it > 0 } ?: (dm.heightPixels * 45 / 100)
        }

        val contentParams = WindowManager.LayoutParams(
            w, h,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val sx = settings.overlayX
            val sy = settings.overlayY
            if (sx != Int.MIN_VALUE) x = sx
            if (sy != Int.MIN_VALUE) y = sy
        }

        val barParams = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = contentParams.x
        }

        // Measure the bar so its bottom edge sits flush with the content box.
        controlBar.measure(
            View.MeasureSpec.makeMeasureSpec(contentParams.width, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val barHeight = if (controlBar.measuredHeight > 0) controlBar.measuredHeight
                        else (48 * dm.density).toInt()
        barParams.y = contentParams.y + contentParams.height - barHeight

        // Store the view/controller references before wiring callbacks so the
        // handlers (which dereference contentController) can find them.
        this.contentRoot = contentRoot
        this.barRoot = barRoot
        this.controlBar = controlBar
        this.contentController = controller
        this.contentParams = contentParams
        this.barParams = barParams

        applyStyle(controller, contentRoot)
        wireControlBar(controlBar)
        wireDrag(controlBar, contentRoot, contentParams, barRoot, barParams)
        wireResize(controlBar, contentRoot, contentParams, barRoot, barParams)

        try {
            windowManager.addView(contentRoot, contentParams)
            windowManager.addView(barRoot, barParams)
        } catch (e: Exception) {
            Toast.makeText(this, "无法显示悬浮窗: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        isOverlayRunning = true

        controller.engine.onScrollEnd = {
            if (autoScrollRequested) {
                autoScrollRequested = false
                updatePlayIcon()
            }
        }
        if (settings.autoScroll) startEngine(controller)
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun applyStyle(controller: OverlayContentController, root: View) {
        controller.setFontSize(settings.fontSize)
        controller.setTextColor(settings.fontColor)
        controller.setBackgroundColor(settings.backgroundColor)
        val alpha = settings.opacity
        root.alpha = alpha.coerceIn(0.2f, 1f)
    }

    private fun presetFraction(preset: Int): Float = when (preset) {
        0 -> 1f / 3f
        1 -> 1f / 2f
        else -> 2f / 3f
    }

    /** Apply the current size setting to the running overlay window. */
    private fun resizeFromSettings() {
        val contentParams = this.contentParams ?: return
        val barParams = this.barParams ?: return
        val contentRoot = this.contentRoot ?: return
        val barRoot = this.barRoot ?: return
        val dm = resources.displayMetrics
        val preset = settings.overlayPresetSize
        val w: Int
        val h: Int
        if (preset >= 0) {
            w = dm.widthPixels
            h = (dm.heightPixels * presetFraction(preset)).toInt().coerceAtLeast(1)
        } else {
            w = settings.overlayWidth.takeIf { it > 0 } ?: contentParams.width
            h = settings.overlayHeight.takeIf { it > 0 } ?: contentParams.height
        }
        contentParams.width = w
        contentParams.height = h
        windowManager.updateViewLayout(contentRoot, contentParams)
        barParams.width = w
        barParams.y = contentParams.y + contentParams.height - barRoot.height
        windowManager.updateViewLayout(barRoot, barParams)
    }

    private fun startEngine(controller: OverlayContentController) {
        autoScrollRequested = true
        controller.engine.start()
        updatePlayIcon()
    }

    private fun updatePlayIcon() {
        controlBar?.findViewById<ImageButton>(R.id.btn_play_pause)?.setImageResource(
            if (autoScrollRequested) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun wireControlBar(bar: View) {
        val controller = contentController ?: return
        bar.findViewById<ImageButton>(R.id.btn_prev).setOnClickListener {
            controller.previous()
            if (autoScrollRequested) controller.engine.start()
        }
        bar.findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            controller.next()
            if (autoScrollRequested) controller.engine.start()
        }
        bar.findViewById<ImageButton>(R.id.btn_prev_slow).setOnClickListener {
            settings.scrollSpeed = (settings.scrollSpeed - 0.2f).coerceAtLeast(0.1f)
            restartEngine(controller)
            Toast.makeText(this, "减速：${"%.1f".format(settings.scrollSpeed)}", Toast.LENGTH_SHORT).show()
        }
        bar.findViewById<ImageButton>(R.id.btn_fast).setOnClickListener {
            settings.scrollSpeed = (settings.scrollSpeed + 0.2f).coerceAtMost(5f)
            restartEngine(controller)
            Toast.makeText(this, "加速：${"%.1f".format(settings.scrollSpeed)}", Toast.LENGTH_SHORT).show()
        }
        bar.findViewById<ImageButton>(R.id.btn_play_pause).setOnClickListener {
            togglePlay(controller)
        }
        bar.findViewById<ImageButton>(R.id.btn_font_minus).setOnClickListener {
            val s = SettingsRepository(this)
            s.fontSize = (s.fontSize - 2f).coerceAtLeast(10f)
            controller.setFontSize(s.fontSize)
            if (autoScrollRequested) controller.engine.start()
        }
        bar.findViewById<ImageButton>(R.id.btn_font_plus).setOnClickListener {
            val s = SettingsRepository(this)
            s.fontSize = (s.fontSize + 2f).coerceAtMost(120f)
            controller.setFontSize(s.fontSize)
            if (autoScrollRequested) controller.engine.start()
        }
        bar.findViewById<ImageButton>(R.id.btn_close).setOnClickListener { stopSelf() }
    }

    private fun wireDrag(
        bar: View,
        contentRoot: View,
        contentParams: WindowManager.LayoutParams,
        barRoot: View,
        barParams: WindowManager.LayoutParams
    ) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        bar.findViewById<ImageButton>(R.id.btn_drag).setOnTouchListener { _, event ->
            var pressX = 0
            var pressY = 0
            var dragged = false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    pressX = lastX
                    pressY = lastY
                    isDraggingBar = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingBar) {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        if (!dragged &&
                            (kotlin.math.abs(event.rawX.toInt() - pressX) > slop ||
                                kotlin.math.abs(event.rawY.toInt() - pressY) > slop)
                        ) {
                            dragged = true
                        }
                        if (dragged) {
                            contentParams.x += dx
                            contentParams.y += dy
                            windowManager.updateViewLayout(contentRoot, contentParams)
                            barParams.x = contentParams.x
                            barParams.y = contentParams.y + contentParams.height - barRoot.height
                            windowManager.updateViewLayout(barRoot, barParams)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingBar) {
                        if (dragged) {
                            settings.overlayX = contentParams.x
                            settings.overlayY = contentParams.y
                        } else {
                            Toast.makeText(
                                this,
                                "按住此按钮拖动可移动悬浮窗位置",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    isDraggingBar = false
                }
            }
            true
        }
    }

    private fun wireResize(
        bar: View,
        contentRoot: View,
        contentParams: WindowManager.LayoutParams,
        barRoot: View,
        barParams: WindowManager.LayoutParams
    ) {
        val dm = resources.displayMetrics
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val minWidth = (180 * dm.density).toInt()
        val minHeight = (120 * dm.density).toInt()
        bar.findViewById<ImageButton>(R.id.btn_resize).setOnTouchListener { _, event ->
            var pressX = 0
            var pressY = 0
            var resized = false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    pressX = lastX
                    pressY = lastY
                    isResizingBar = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isResizingBar) {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        if (kotlin.math.abs(event.rawX.toInt() - pressX) > slop ||
                            kotlin.math.abs(event.rawY.toInt() - pressY) > slop
                        ) {
                            resized = true
                        }
                        if (resized) {
                            contentParams.width = (contentParams.width + dx).coerceIn(minWidth, dm.widthPixels)
                            contentParams.height = (contentParams.height + dy).coerceIn(minHeight, dm.heightPixels)
                            windowManager.updateViewLayout(contentRoot, contentParams)
                            barParams.width = contentParams.width
                            barParams.y = contentParams.y + contentParams.height - barRoot.height
                            windowManager.updateViewLayout(barRoot, barParams)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isResizingBar) {
                        if (resized) {
                            settings.overlayWidth = contentParams.width
                            settings.overlayHeight = contentParams.height
                            settings.overlayPresetSize = -1
                        } else {
                            // Tap: cycle between the size presets.
                            val next = when (settings.overlayPresetSize) {
                                0 -> 1
                                1 -> 2
                                else -> 0
                            }
                            settings.overlayPresetSize = next
                            resizeFromSettings()
                        }
                    }
                    isResizingBar = false
                }
            }
            true
        }
    }

private fun restartEngine(controller: OverlayContentController) {
    // Start from the current scroll position so the new speed applies
    // immediately without jumping back to the top. Only while actively running;
    // when paused the stored speed takes effect on the next play.
    if (controller.engine.isRunning) controller.engine.start()
}

    private fun togglePlay(controller: OverlayContentController) {
        if (autoScrollRequested) {
            autoScrollRequested = false
            controller.engine.pause()
        } else {
            autoScrollRequested = true
            controller.engine.start()
        }
        updatePlayIcon()
    }

    private fun updateContent(content: String) {
        contentController?.setContent(content)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TeleprompterOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("提词器运行中")
            .setContentText("点击打开设置")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    override fun onDestroy() {
        isOverlayRunning = false
        autoScrollRequested = false
        contentController?.release()
        contentRoot?.let { runCatching { windowManager.removeView(it) } }
        barRoot?.let { runCatching { windowManager.removeView(it) } }
        contentController = null
        contentRoot = null
        barRoot = null
        controlBar = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @Volatile
        var isOverlayRunning = false
            private set

        const val ACTION_STOP = "com.wyprompter.action.STOP"
        const val ACTION_SHOW = "com.wyprompter.action.SHOW"
        const val TAG = "wyRemote"

        fun start(context: Context) {
            val intent = Intent(context, TeleprompterOverlayService::class.java).setAction(ACTION_SHOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TeleprompterOverlayService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}