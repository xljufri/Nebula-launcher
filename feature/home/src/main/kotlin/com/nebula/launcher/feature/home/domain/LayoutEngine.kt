package com.nebula.launcher.feature.home.domain

import com.nebula.launcher.core.model.AppCluster
import com.nebula.launcher.core.model.AppNode
import javax.inject.Inject
import kotlin.math.sqrt

class LayoutEngine @Inject constructor() {
    
    fun updatePositions(nodes: List<AppNode>, clusters: List<AppCluster>, baseRadiusPx: Float): List<AppNode> {
        val updatedNodes = nodes.toMutableList()
        
        for (i in updatedNodes.indices) {
            val node = updatedNodes[i]
            val cluster = clusters.find { it.id == node.clusterId } ?: continue
            
            var newX = node.initialX
            var newY = node.initialY
            
            // Calculate node center
            val nodeRadius = baseRadiusPx * node.weight
            val nodeCenterX = newX + nodeRadius
            val nodeCenterY = newY + nodeRadius
            
            // 1. Gravity towards cluster center
            val dx = cluster.centerX - nodeCenterX
            val dy = cluster.centerY - nodeCenterY
            
            // Move 2% towards the center each tick
            newX += dx * 0.02f
            newY += dy * 0.02f
            
            // Update node center for repulsion
            val updatedNodeCenterX = newX + nodeRadius
            val updatedNodeCenterY = newY + nodeRadius
            
            // 2. Repulsion from other nodes to prevent overlap
            for (j in updatedNodes.indices) {
                if (i == j) continue
                
                val otherNode = updatedNodes[j]
                val otherNodeRadius = baseRadiusPx * otherNode.weight
                val otherNodeCenterX = otherNode.initialX + otherNodeRadius
                val otherNodeCenterY = otherNode.initialY + otherNodeRadius
                
                val diffX = updatedNodeCenterX - otherNodeCenterX
                val diffY = updatedNodeCenterY - otherNodeCenterY
                val distanceSq = diffX * diffX + diffY * diffY
                
                // Calculate minimum distance based on weights
                val minDistance = nodeRadius + otherNodeRadius + 10f // 10f padding
                val minDistanceSq = minDistance * minDistance
                
                if (distanceSq < minDistanceSq && distanceSq > 0.1f) {
                    val distance = sqrt(distanceSq)
                    val overlap = minDistance - distance
                    
                    // Push apart proportionally to overlap
                    val pushFactor = (overlap / distance) * 0.1f
                    newX += diffX * pushFactor
                    newY += diffY * pushFactor
                }
            }
            
            updatedNodes[i] = node.copy(initialX = newX, initialY = newY)
        }
        
        return updatedNodes
    }
}
