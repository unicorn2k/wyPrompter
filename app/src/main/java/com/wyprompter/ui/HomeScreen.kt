package com.wyprompter.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wyprompter.core.BatteryOptimizationHelper
import kotlin.math.roundToInt
import com.wyprompter.core.PermissionHelper
import com.wyprompter.core.TextImporter
import com.wyprompter.data.Manuscript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PrompterViewModel,
    onStartPrompter: (title: String, content: String) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onNotificationPermissionRequest: () -> Unit,
    onOpenBluetoothSettings: () -> Unit
) {
    var tab by remember { mutableStateOf<HomeTab>(HomeTab.Editor) }
    var pendingDelete by remember { mutableStateOf<Manuscript?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val manuscripts by viewModel.manuscripts.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch
            val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
            val text = when (ext) {
                "docx" -> TextImporter.fromDocx(bytes)
                "doc" -> "不支持旧版 .doc 格式，请另存为 .docx"
                else -> TextImporter.fromTxt(bytes, "UTF-8")
            }
            viewModel.editingContent = text
            viewModel.editingId = 0L
            tab = HomeTab.Editor
        }
    }

    val canOverlay = PermissionHelper.canDrawOverlays(context)

    AppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("无忧提词器", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HomeTabRow(tab = tab, onTabChange = { tab = it })
                when (tab) {
                    HomeTab.Editor -> EditorPane(
                        title = viewModel.editingTitle,
                        content = viewModel.editingContent,
                        onTitleChange = { viewModel.editingTitle = it },
                        onContentChange = { viewModel.editingContent = it },
                        onImport = { importLauncher.launch(arrayOf("application/octet-stream", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                        onSave = {
                            viewModel.save(viewModel.editingId, viewModel.editingTitle, viewModel.editingContent) { id ->
                                viewModel.editingId = id
                                viewModel.setCurrent(id)
                            }
                        },
                        onStart = {
                            viewModel.save(viewModel.editingId, viewModel.editingTitle, viewModel.editingContent) { id ->
                                viewModel.editingId = id
                                viewModel.setCurrent(id)
                                onStartPrompter(viewModel.editingTitle, viewModel.editingContent)
                            }
                        },
                        heightValue = viewModel.fontSize, onHeightChange = { viewModel.updateFontSize(it) },
                        speedValue = viewModel.scrollSpeed, onSpeedChange = { viewModel.updateScrollSpeed(it) },
                        opacityValue = viewModel.opacity, onOpacityChange = { viewModel.updateOpacity(it) },
                        fontColor = viewModel.fontColor, onFontColorChange = { viewModel.updateFontColor(it) },
                        autoScroll = viewModel.autoScroll, onAutoScroll = { viewModel.updateAutoScroll(it) },
                        backgroundColor = viewModel.backgroundColor, onBackgroundChange = { viewModel.updateBackgroundColor(it) },
                        overlaySize = viewModel.overlaySize, onOverlaySizeChange = { viewModel.updateOverlaySize(it) },
                        floatingWindow = viewModel.floatingWindow,
                        onFloatingWindowChange = { viewModel.updateFloatingWindow(it) }
                    )
                    HomeTab.List -> ManuscriptList(
                        manuscripts = manuscripts,
                        onSelect = { m ->
                            viewModel.editingId = m.id
                            viewModel.editingTitle = m.title
                            viewModel.editingContent = m.content
                            viewModel.setCurrent(m.id)
                            tab = HomeTab.Editor
                        },
                        onDelete = { pendingDelete = it }
                    )
                    HomeTab.Settings -> SettingsPane(
                        canOverlay = canOverlay,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestBatteryExemption = onRequestBatteryExemption,
                        onOpenAppSettings = onOpenAppSettings,
                        onNotificationPermissionRequest = onNotificationPermissionRequest,
                        onOpenBluetoothSettings = onOpenBluetoothSettings
                    )
                    HomeTab.Help -> HelpScreen()
                }
            }
        }

        pendingDelete?.let { m ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("删除稿件", fontWeight = FontWeight.Bold) },
                text = { Text("确定要删除「${m.title.ifBlank { "未命名" }}」吗？\n删除后无法恢复。") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.delete(m)
                            pendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        }
    }
}

private enum class HomeTab(val label: String) {
    Editor("编辑"), List("稿件"), Settings("设置"), Help("说明")
}

@Composable
private fun HomeTabRow(tab: HomeTab, onTabChange: (HomeTab) -> Unit) {
    TabRow(
        selectedTabIndex = tab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        HomeTab.entries.forEach { t ->
            Tab(
                selected = tab == t,
                onClick = { onTabChange(t) },
                text = {
                    Text(
                        t.label,
                        fontWeight = if (tab == t) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun EditorPane(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    onStart: () -> Unit,
    heightValue: Float,
    onHeightChange: (Float) -> Unit,
    speedValue: Float,
    onSpeedChange: (Float) -> Unit,
    opacityValue: Float,
    onOpacityChange: (Float) -> Unit,
    fontColor: Int,
    onFontColorChange: (Int) -> Unit,
    autoScroll: Boolean,
    onAutoScroll: (Boolean) -> Unit,
    backgroundColor: Int,
    onBackgroundChange: (Int) -> Unit,
    overlaySize: Int,
    onOverlaySizeChange: (Int) -> Unit,
    floatingWindow: Boolean,
    onFloatingWindowChange: (Boolean) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("标题") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text("提词内容（空行分段）") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(190.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onImport,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("导入文件", maxLines = 1) }
            FilledTonalButton(
                onClick = onSave,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("保存", maxLines = 1) }
            Button(
                onClick = onStart,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("开始提词", maxLines = 1) }
        }
        Spacer(Modifier.height(16.dp))
        SettingsCard("显示设置") {
            SliderRow("字体大小", heightValue, 14f..120f, onHeightChange) { "${it.toInt()}sp" }
            Spacer(Modifier.height(6.dp))
            SliderRow("滚动速度", speedValue, 0.1f..5f, onSpeedChange) { "%.1f".format(it) }
            Spacer(Modifier.height(6.dp))
            SliderRow("透明度", opacityValue, 0.2f..1f, onOpacityChange) { "${(it * 100).toInt()}%" }
            Spacer(Modifier.height(6.dp))
            FontColorRow(fontColor, onFontColorChange)
        }
        Spacer(Modifier.height(12.dp))
        SettingsCard("滚动与悬浮窗") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "悬浮窗提词",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = floatingWindow, onCheckedChange = onFloatingWindowChange)
            }
            Text(
                if (floatingWindow) {
                    "开：提词显示为悬浮窗，覆盖其他应用（不支持蓝牙遥控）"
                } else {
                    "关：提词在应用内全屏显示（支持蓝牙遥控）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "自动滚动",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = autoScroll, onCheckedChange = onAutoScroll)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SegmentField("背景颜色", listOf("透明", "黑色", "白色"), backgroundIndex(backgroundColor)) { index ->
                onBackgroundChange(
                    when (index) {
                        1 -> android.graphics.Color.BLACK
                        2 -> android.graphics.Color.WHITE
                        else -> android.graphics.Color.TRANSPARENT
                    }
                )
            }
            Spacer(Modifier.height(14.dp))
            SegmentField("悬浮窗大小", listOf("1/3屏", "1/2屏", "2/3屏"), sizeIndex(overlaySize), onOverlaySizeChange)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "提示：悬浮窗模式需开启「显示在其他应用上层」；非悬浮窗模式在应用内提词，可用蓝牙遥控的 + / - 翻屏、确认键暂停/继续",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    format: (Float) -> String
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    format(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        StyledSlider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    val thumbSize = 20.dp
    val trackHeight = 6.dp
    val thumbPx = with(LocalDensity.current) { thumbSize.toPx() }

    var widthPx by remember { mutableStateOf(0) }

    fun valueForX(x: Float): Float {
        val trackW = (widthPx - thumbPx).coerceAtLeast(1f)
        return min + (((x - thumbPx / 2f) / trackW).coerceIn(0f, 1f)) * (max - min)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { onValueChange(valueForX(it.x)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onValueChange(valueForX(change.position.x))
                    }
                )
            }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(((widthPx - thumbPx) * fraction).roundToInt(), 0) }
                .size(thumbSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(3.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun SegmentField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun backgroundIndex(color: Int): Int = when (color) {
    android.graphics.Color.BLACK -> 1
    android.graphics.Color.WHITE -> 2
    else -> 0
}

private fun sizeIndex(preset: Int): Int = when (preset) {
    0 -> 0
    2 -> 2
    else -> 1
}

private val FONT_COLOR_PRESETS: List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFF5252.toInt(),
    0xFF4CAF50.toInt(),
    0xFF00BCD4.toInt()
)

@Composable
private fun FontColorRow(fontColor: Int, onFontColorChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("字体颜色", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        FONT_COLOR_PRESETS.forEach { c ->
            val selected = c == fontColor
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
                        shape = CircleShape
                    )
                    .clickable { onFontColorChange(c) }
            )
        }
    }
}

@Composable
private fun ManuscriptList(
    manuscripts: List<Manuscript>,
    onSelect: (Manuscript) -> Unit,
    onDelete: (Manuscript) -> Unit
) {
    if (manuscripts.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("还没有稿件", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(manuscripts, key = { it.id }) { m ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (m.title.isBlank()) "未命名" else m.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            m.content.take(40),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    TextButton(onClick = { onSelect(m) }) { Text("编辑") }
                    TextButton(onClick = { onDelete(m) }) { Text("删除") }
                }
            }
        }
    }
}

@Composable
private fun SettingsPane(
    canOverlay: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onNotificationPermissionRequest: () -> Unit,
    onOpenBluetoothSettings: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            SettingRow(
                title = "蓝牙遥控器",
                desc = "先与手机配对并连接，再按 + / - 控制滚屏",
                action = "去连接",
                onClick = onOpenBluetoothSettings
            )
        }
        item {
            SettingRow(
                title = "显示在其他应用上层",
                desc = if (canOverlay) "已开启" else "需要开启才能悬浮提词",
                action = "去开启",
                onClick = onRequestOverlayPermission
            )
        }
        item {
            SettingRow(
                title = "电池白名单",
                desc = BatteryOptimizationHelper.oemHint(),
                action = "设置",
                onClick = onRequestBatteryExemption
            )
        }
        item {
            SettingRow(
                title = "通知权限",
                desc = "用于前台悬浮通知",
                action = "设置",
                onClick = onNotificationPermissionRequest
            )
        }
        item {
            SettingRow(
                title = "应用详情",
                desc = "管理权限与后台",
                action = "打开",
                onClick = onOpenAppSettings
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, desc: String, action: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onClick) { Text(action) }
        }
    }
}