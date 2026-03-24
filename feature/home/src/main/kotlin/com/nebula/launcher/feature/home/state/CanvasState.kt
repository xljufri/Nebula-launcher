package com.nebula.launcher.feature.home.state

import androidx.compose.ui.geometry.Offset

data class CanvasState(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
)
