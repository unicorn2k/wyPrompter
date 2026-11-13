package com.wyprompter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.wyprompter.core.BatteryOptimizationHelper
import com.wyprompter.core.PermissionHelper
import com.wyprompter.data.AppDatabase
import com.wyprompter.data.SettingsRepository
import com.wyprompter.overlay.TeleprompterOverlayService
import com.wyprompter.remote.RemoteKeyController
import com.wyprompter.ui.HomeScreen
import com.wyprompter.ui.InAppPrompterController
import com.wyprompter.ui.InAppPrompterScreen
import com.wyprompter.ui.PrompterViewModel
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    companion object {
        private const val DEVICE_ID_VIRTUAL = -1
    }

    private val settings by lazy { SettingsRepository(this) }

    private val viewModel: PrompterViewModel by viewModels {
        PrompterViewModel.Factory(
            AppDatabase.getInstance(this).manuscriptDao(),
            settings
        )
    }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            maybeStartOverlay()
        }

    private val batteryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // nothing needed; setting is read on demand
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private var prompterTitle by mutableStateOf("")
    private var prompterContent by mutableStateOf("")
    private var showInAppPrompter by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (showInAppPrompter) {
                InAppPrompterScreen(
                    title = prompterTitle,
                    content = prompterContent,
                    fontSize = settings.fontSize,
                    scrollSpeed = settings.scrollSpeed,
                    fontColor = settings.fontColor,
                    backgroundColor = settings.backgroundColor,
                    autoScroll = settings.autoScroll,
                    onExit = { showInAppPrompter = false },
                    onSpeedChange = { settings.scrollSpeed = it }
                )
            } else {
                HomeScreen(
                    viewModel = viewModel,
                    onStartPrompter = { title, content -> startPrompter(title, content) },
                    onRequestOverlayPermission = {
                        PermissionHelper.openOverlaySettings(this)
                    },
                    onRequestBatteryExemption = {
                        BatteryOptimizationHelper.openIgnoringBatteryOptimizations(this)
                    },
                    onOpenAppSettings = { openAppSettings() },
                    onNotificationPermissionRequest = { requestNotificationPermissionIfNeeded() },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val devName = InputDevice.getDevice(event.deviceId)?.name ?: ""
        Log.d(
            "wyKey",
            "${if (event.action == KeyEvent.ACTION_DOWN) "down" else "up"} " +
                "code=${event.keyCode} scan=${event.scanCode} dev=${event.deviceId} name=$devName"
        )
        // LY-03 remote is controlled via its virtual-touch ABS_Y channel
        // (shown in dispatchTouchEvent); its keyCode is an undifferentiated 188.
        if (event.action == KeyEvent.ACTION_DOWN && !devName.contains("LY-03")) {
            InAppPrompterController.instance?.let { ctrl ->
                when (RemoteKeyController.translateKey(event.keyCode)) {
                    RemoteKeyController.Action.NEXT -> {
                        ctrl.next()
                        return true
                    }
                    RemoteKeyController.Action.PREV -> {
                        ctrl.previous()
                        return true
                    }
                    RemoteKeyController.Action.TOGGLE -> {
                        ctrl.toggle()
                        return true
                    }
                    RemoteKeyController.Action.IGNORED -> Unit
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private enum class LyKey { NEXT, PREV, TOGGLE, UNKNOWN }

    private fun classifyLyKey(fraction: Float): LyKey = when {
        abs(fraction - 0.350f) < 0.035f -> LyKey.NEXT // "+"
        abs(fraction - 0.620f) < 0.035f -> LyKey.PREV // "-"
        abs(fraction - 0.400f) < 0.035f -> LyKey.TOGGLE // power
        else -> LyKey.UNKNOWN
    }

    private var virtualPointerConsuming = -1

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.deviceId == DEVICE_ID_VIRTUAL && showInAppPrompter) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val y = event.getY()
                    val fraction = y / resources.displayMetrics.heightPixels
                    Log.d("wyRemote", "guess y=$y frac=${"%.3f".format(fraction)}")
                    when (classifyLyKey(fraction)) {
                        LyKey.NEXT -> {
                            InAppPrompterController.instance?.next()
                            virtualPointerConsuming = event.getPointerId(0)
                            Log.i("wyRemote", "LY-03 NEXT")
                            return true
                        }
                        LyKey.PREV -> {
                            InAppPrompterController.instance?.previous()
                            virtualPointerConsuming = event.getPointerId(0)
                            Log.i("wyRemote", "LY-03 PREV")
                            return true
                        }
                        LyKey.TOGGLE -> {
                            InAppPrompterController.instance?.toggle()
                            virtualPointerConsuming = event.getPointerId(0)
                            Log.i("wyRemote", "LY-03 TOGGLE")
                            return true
                        }
                        LyKey.UNKNOWN -> Unit
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (virtualPointerConsuming == event.getPointerId(0)) {
                        virtualPointerConsuming = -1
                        return true
                    }
                }
                else -> if (virtualPointerConsuming == event.getPointerId(0)) return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startPrompter(title: String, content: String) {
        if (settings.floatingWindow) {
            maybeStartOverlay()
        } else {
            prompterTitle = title
            prompterContent = content
            showInAppPrompter = true
        }
    }

    private fun maybeStartOverlay() {
        if (PermissionHelper.canDrawOverlays(this)) {
            TeleprompterOverlayService.start(this)
        }
    }
}