package com.nebula.launcher.feature.home.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.Color
import com.nebula.launcher.core.model.AppNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.random.Random

class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun loadInstalledApps(screenWidth: Float, screenHeight: Float): List<AppNode> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        
        return resolveInfos.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            
            // Generate unique color based on package name hashcode
            val hash = packageName.hashCode()
            val color = Color(
                red = ((hash shr 16) and 0xFF) / 255f,
                green = ((hash shr 8) and 0xFF) / 255f,
                blue = (hash and 0xFF) / 255f,
                alpha = 1f
            )
            
            // Generate random position within screen bounds (with some padding)
            val padding = 150f
            val safeWidth = if (screenWidth > padding * 2) screenWidth - padding * 2 else screenWidth
            val safeHeight = if (screenHeight > padding * 2) screenHeight - padding * 2 else screenHeight
            
            val x = Random.nextFloat() * safeWidth + padding
            val y = Random.nextFloat() * safeHeight + padding
            
            AppNode(
                packageName = packageName,
                label = label,
                color = color,
                initialX = x,
                initialY = y
            )
        }
    }
}
