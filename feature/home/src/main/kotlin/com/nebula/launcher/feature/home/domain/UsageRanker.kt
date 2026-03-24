package com.nebula.launcher.feature.home.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import com.nebula.launcher.core.model.AppNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UsageRanker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun rank(nodes: List<AppNode>): List<AppNode> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 60 * 24) // 24 hours

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        if (stats.isNullOrEmpty()) return nodes

        val usageMap = stats.associate { it.packageName to it.totalTimeInForeground }
        val maxUsage = usageMap.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

        return nodes.map { node ->
            val usage = usageMap[node.packageName] ?: 0L
            // Normalize weight between 0.8f and 2.0f
            val normalizedUsage = usage.toFloat() / maxUsage.toFloat()
            val weight = 0.8f + (normalizedUsage * 1.2f)
            node.copy(weight = weight)
        }
    }
}
