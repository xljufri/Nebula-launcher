package com.nebula.launcher.feature.home.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.nebula.launcher.core.model.AppCluster
import com.nebula.launcher.core.model.AppNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

class AppClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun classify(apps: List<AppNode>, screenWidth: Float, screenHeight: Float): Pair<List<AppNode>, List<AppCluster>> {
        val pm = context.packageManager
        
        // Group apps by category
        val groupedApps = apps.groupBy { node ->
            try {
                val appInfo = pm.getApplicationInfo(node.packageName, 0)
                val categoryTitle = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val category = appInfo.category
                    ApplicationInfo.getCategoryTitle(context, category)
                } else {
                    null
                }
                categoryTitle?.toString() ?: "Other"
            } catch (e: PackageManager.NameNotFoundException) {
                "Other"
            }
        }

        val clusters = mutableListOf<AppCluster>()
        val updatedNodes = mutableListOf<AppNode>()
        
        // Arrange clusters in a circle around the center of the screen
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        val radius = minOf(screenWidth, screenHeight) * 0.8f
        
        val numClusters = groupedApps.size
        var angle = 0f
        val angleStep = (2 * Math.PI / numClusters).toFloat()

        var clusterIdCounter = 0
        
        for ((categoryName, categoryNodes) in groupedApps) {
            val clusterId = clusterIdCounter++
            
            // Calculate cluster center
            val cx = centerX + radius * cos(angle)
            val cy = centerY + radius * sin(angle)
            
            clusters.add(
                AppCluster(
                    id = clusterId,
                    name = categoryName,
                    centerX = cx,
                    centerY = cy
                )
            )
            
            // Assign clusterId to nodes
            updatedNodes.addAll(categoryNodes.map { it.copy(clusterId = clusterId) })
            
            angle += angleStep
        }

        return Pair(updatedNodes, clusters)
    }
}
