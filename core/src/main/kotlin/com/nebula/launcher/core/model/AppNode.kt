package com.nebula.launcher.core.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_nodes")
data class AppNode(
    @PrimaryKey val packageName: String,
    val label: String,
    val color: Color,
    val initialX: Float,
    val initialY: Float,
    val weight: Float = 1f,
    val clusterId: Int = -1
)
