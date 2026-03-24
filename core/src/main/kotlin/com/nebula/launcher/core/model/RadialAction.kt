package com.nebula.launcher.core.model

import androidx.compose.ui.graphics.vector.ImageVector

data class RadialAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)
