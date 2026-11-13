package com.wyprompter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class HelpSection(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val steps: List<String>
)

private val helpSections = listOf(
    HelpSection(
        title = "快速开始",
        subtitle = "三步上手",
        icon = Icons.Outlined.RocketLaunch,
        steps = listOf(
            "在「编辑」页输入标题与内容，或点「导入文件」导入 .txt / .docx 文稿。",
            "点「保存」把稿件存入本地。",
            "点「开始提词」进入提词，随手调节字体与速度。"
        )
    ),
    HelpSection(
        title = "两种提词模式",
        subtitle = "悬浮窗 / 应用内",
        icon = Icons.Outlined.Layers,
        steps = listOf(
            "悬浮窗：覆盖在其他应用之上，需在「设置」开启「显示在其他应用上层」。",
            "应用内：全屏提词，顶部有返回与速度控制，适合专注场景。",
            "两种模式均支持自动滚动与手动翻页。"
        )
    ),
    HelpSection(
        title = "蓝牙遥控器",
        subtitle = "免触摸控制",
        icon = Icons.Outlined.Bluetooth,
        steps = listOf(
            "先在系统设置中与遥控器（如 LY-03）配对连接。",
            "「+」下一屏 · 「-」上一屏 · 电源键 暂停/继续。",
            "匹配成功后尽量让三个按键落在屏幕不同的纵向位置。"
        )
    ),
    HelpSection(
        title = "显示设置",
        subtitle = "所见即所得",
        icon = Icons.Outlined.FormatSize,
        steps = listOf(
            "字体大小、滚动速度、字幕透明度、字体颜色均实时生效。",
            "开启「自动滚动」后按速度持续滚动，关闭后手动翻屏。",
            "背景颜色与悬浮窗大小在「滚动与悬浮窗」区域调整。"
        )
    ),
    HelpSection(
        title = "稿件管理",
        subtitle = "本地存档",
        icon = Icons.Outlined.FolderOpen,
        steps = listOf(
            "「稿件」页存放全部已保存稿件，点击进入编辑。",
            "删除稿件需二次确认，删除后无法恢复。"
        )
    )
)

private data class PrivacySection(
    val title: String,
    val body: String? = null,
    val items: List<String> = emptyList()
)

private val privacySections = listOf(
    PrivacySection(
        title = "数据存储",
        body = "您输入或导入的稿件数据仅保存在设备本地（SQLite）数据库中，不会上传任何服务器，也不会向任何第三方共享。"
    ),
    PrivacySection(
        title = "权限说明",
        items = listOf(
            "「显示在其他应用上层」：仅用于悬浮窗提词显示。",
            "「通知/前台服务」：维持悬浮窗提词持续运行。",
            "「忽略电池优化」白名单：防止提词过程被系统清理中断。",
            "「蓝牙」：仅与本机遥控器（如 LY-03）配对用于提词控制，不收集任何信息。"
        )
    ),
    PrivacySection(
        title = "网络与广告",
        body = "本应用不含广告 SDK、统计 SDK，不会在后台访问网络或上传任何数据。"
    ),
    PrivacySection(
        title = "权限撤销",
        body = "你可在系统设置中随时关闭相关权限，或直接卸载本应用。"
    ),
    PrivacySection(
        title = "政策变更",
        body = "如政策内容发生变更，我们将在此页面更新。"
    )
)

@Composable
fun HelpScreen() {
    var showPrivacy by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HelpHero() }
        items(helpSections) { section ->
            HelpCard(section)
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { showPrivacy = true }) {
                    Icon(
                        Icons.Outlined.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("隐私政策")
                }
            }
        }
    }

    if (showPrivacy) {
        PrivacyDialog(onDismiss = { showPrivacy = false })
    }
}

@Composable
private fun HelpHero() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "无忧提词器",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "手机端的悬浮提词与遥控控制",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun HelpCard(section: HelpSection) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        section.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            section.steps.forEach { step ->
                Row {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        title = {
            Column {
                Text("隐私政策", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "版本 1.0 · 本地应用，数据不出设备",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                privacySections.forEachIndexed { index, section ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    section.items.forEach { item ->
                        Row {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    section.body?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("我知道了") }
        }
    )
}