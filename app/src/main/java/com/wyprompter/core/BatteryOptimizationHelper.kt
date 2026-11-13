package com.wyprompter.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openIgnoringBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            if (context is Activity) {
                context.startActivityForResult(intent, REQUEST_IGNORE_BATTERY)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    /**
     * Returns a consumer-friendly hint string describing common OEM paths for
     * whitelisting the app. Used by the guide screen.
     */
    fun oemHint(): String = when {
        Build.MANUFACTURER.equals("huawei", true) ->
            "华为：设置 > 电池 > 应用启动管理 > 手动管理，允许自启动/关联启动/后台活动"
        Build.MANUFACTURER.equals("xiaomi", true) || Build.MANUFACTURER.equals("redmi", true) ->
            "小米：设置 > 应用设置 > 应用管理 > 提词器 > 省电策略 > 无限制"
        Build.MANUFACTURER.equals("oppo", true) ->
            "OPPO：设置 > 电池 > 应用耗电管理 > 提词器 > 允许后台运行"
        Build.MANUFACTURER.equals("vivo", true) ->
            "vivo：i管家 > 应用管理 > 权限管理 > 后台耗电 > 提词器 > 允许后台高耗电"
        else ->
            "设置 > 电池 > 忽略电池优化 或 允许后台运行，避免提词器被系统清理"
    }

    const val REQUEST_IGNORE_BATTERY = 1001
}