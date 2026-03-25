package com.nebula.launcher.feature.home

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nebula.launcher.core.model.AppNode
import com.nebula.launcher.feature.home.state.CanvasState
import com.nebula.launcher.feature.home.ui.RadialMenu
import com.nebula.launcher.feature.home.ui.OnboardingOverlay
import com.nebula.launcher.feature.home.ui.WeatherWidget
import kotlin.math.roundToInt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveNodes()
                viewModel.stopPhysicsSimulation()
            } else if (event == Lifecycle.Event.ON_START) {
                viewModel.resumePhysicsSimulation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val appNodes by viewModel.appNodes.collectAsState()
    val appClusters by viewModel.appClusters.collectAsState()
    val hasPermission by viewModel.hasUsagePermission.collectAsState()
    val selectedNodeMenu by viewModel.selectedNodeMenu.collectAsState()
    val isFocusModeEnabled by viewModel.isFocusModeEnabled.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val showAppList by viewModel.showAppList.collectAsState()
    val weatherData by viewModel.weatherData.collectAsState()
    
    var onboardingStep by remember { mutableStateOf(1) }
    
    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding) {
            onboardingStep = 0
        }
    }
    
    var canvasState by remember { mutableStateOf(CanvasState()) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = isSearchVisible || selectedNodeMenu != null) {
        if (selectedNodeMenu != null) {
            viewModel.closeMenu()
        } else if (isSearchVisible) {
            isSearchVisible = false
            searchQuery = ""
        }
    }

    val searchResults = remember(searchQuery, appNodes) {
        if (searchQuery.isBlank()) emptyList()
        else appNodes.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(screenWidthPx, screenHeightPx) {
        viewModel.checkPermissionAndLoadApps(screenWidthPx, screenHeightPx)
    }

    LaunchedEffect(isFocusModeEnabled) {
        if (isFocusModeEnabled) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    if (!hasPermission) {
        // ... permission UI ...
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Usage Access Required for Ranking", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    viewModel.checkPermissionAndLoadApps(screenWidthPx, screenHeightPx)
                }) {
                    Text("Refresh")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(selectedNodeMenu, isFocusModeEnabled, showAppList) {
                if (showAppList) return@pointerInput
                if (isFocusModeEnabled) {
                    detectTransformGestures { _, pan, _, _ ->
                        if (pan.y > 20f) {
                            isSearchVisible = true
                        } else if (pan.y < -20f) {
                            isSearchVisible = false
                        }
                    }
                    return@pointerInput
                }
                if (selectedNodeMenu != null) {
                    detectTapGestures(
                        onTap = { viewModel.closeMenu() }
                    )
                } else {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (onboardingStep == 1 && (pan.getDistance() > 10f || zoom != 1f)) {
                            onboardingStep = 2
                        }

                        if (pan.y > 20f && zoom == 1f) {
                            isSearchVisible = true
                        } else if (pan.y < -20f && zoom == 1f) {
                            isSearchVisible = false
                        }
                        
                        val oldScale = canvasState.scale
                        val newScale = (oldScale * zoom).coerceIn(0.5f, 3f)
                        val fractionalScale = newScale / oldScale
                        val newOffset = (canvasState.offset + centroid - centroid * fractionalScale) + pan
                        
                        canvasState = canvasState.copy(
                            scale = newScale,
                            offset = newOffset
                        )
                    }
                }
            }
    ) {
        // Scrim layer for better contrast against live wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        if (!showAppList && !isFocusModeEnabled) {
            WeatherWidget(
                weather = weatherData,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 100.dp)
            )
        }

        if (showAppList) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp)
            ) {
                items(appNodes.sortedBy { it.label }) { node ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                val intent = context.packageManager.getLaunchIntentForPackage(node.packageName)
                                intent?.let { context.startActivity(it) }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = node.label.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = node.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            AnimatedVisibility(
                visible = !isFocusModeEnabled,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val scale = canvasState.scale
                val offset = canvasState.offset

                // Render cluster labels if zoomed in enough
                if (scale > 1.2f) {
                    appClusters.forEach { cluster ->
                        val cx = (cluster.centerX * scale) + offset.x
                        val cy = (cluster.centerY * scale) + offset.y
                        
                        Text(
                            text = cluster.name,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 24.sp * scale,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .offset { 
                                    val halfSizePx = with(density) { 100.dp.toPx() }.roundToInt()
                                    IntOffset(cx.roundToInt() - halfSizePx, cy.roundToInt() - halfSizePx) 
                                }
                                .size(200.dp)
                        )
                    }
                }

                appNodes.forEach { node ->
                    // Animate position changes for smooth physics
                    val animatedX by animateFloatAsState(
                        targetValue = node.initialX,
                        animationSpec = tween(durationMillis = 16),
                        label = "x_anim_${node.packageName}"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = node.initialY,
                        animationSpec = tween(durationMillis = 16),
                        label = "y_anim_${node.packageName}"
                    )

                    val x = (animatedX * scale) + offset.x
                    val y = (animatedY * scale) + offset.y
                    val nodeScale = scale * node.weight

                    val colorScheme = MaterialTheme.colorScheme
                    val nodeColor = remember(node.packageName) {
                        val colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.secondaryContainer,
                            colorScheme.tertiaryContainer,
                            colorScheme.errorContainer
                        )
                        colors[kotlin.math.abs(node.packageName.hashCode()) % colors.size]
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .size(80.dp * nodeScale)
                            .combinedClickable(
                                onClick = {
                                    if (onboardingStep > 0) return@combinedClickable
                                    if (selectedNodeMenu != null) {
                                        viewModel.closeMenu()
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val intent = context.packageManager.getLaunchIntentForPackage(node.packageName)
                                        intent?.let { context.startActivity(it) }
                                    }
                                },
                                onLongClick = {
                                    if (onboardingStep == 2) {
                                        onboardingStep = 3
                                    }
                                    if (onboardingStep == 1) return@combinedClickable
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.openMenu(node.packageName)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp * nodeScale)
                                .clip(CircleShape)
                                .background(nodeColor)
                        )
                        Text(
                            text = node.label,
                            color = Color.White,
                            fontSize = 10.sp * nodeScale,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.offset(y = 40.dp * nodeScale)
                        )
                    }
                }

                // Render Radial Menu
                selectedNodeMenu?.let { packageName ->
                    val selectedNode = appNodes.find { it.packageName == packageName }
                    if (selectedNode != null) {
                        val nodeScale = scale * selectedNode.weight
                        val nodeX = (selectedNode.initialX * scale) + offset.x
                        val nodeY = (selectedNode.initialY * scale) + offset.y
                        
                        val nodeCenterX = nodeX + with(density) { (40.dp * nodeScale).toPx() }
                        val nodeCenterY = nodeY + with(density) { (40.dp * nodeScale).toPx() }
                        
                        val menuTopLeftX = nodeCenterX - with(density) { 100.dp.toPx() }
                        val menuTopLeftY = nodeCenterY - with(density) { 100.dp.toPx() }
                        
                        val actions = remember(packageName) { viewModel.getShortcuts(packageName) }
                        
                        RadialMenu(
                            actions = actions,
                            onDismiss = { viewModel.closeMenu() },
                            modifier = Modifier.offset { IntOffset(menuTopLeftX.roundToInt(), menuTopLeftY.roundToInt()) }
                        )
                    }
                }
            }
        }
    }

        AnimatedVisibility(
            visible = isFocusModeEnabled,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tetap Fokus. Kurangi Distraksi.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        }

        // Settings Button
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }

        // Search Bar
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari aplikasi...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchResults.isNotEmpty()) {
                                val intent = context.packageManager.getLaunchIntentForPackage(searchResults[0].packageName)
                                intent?.let { context.startActivity(it) }
                                isSearchVisible = false
                                searchQuery = ""
                            }
                        }
                    )
                )

                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                            .heightIn(max = 300.dp)
                    ) {
                        items(searchResults) { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = context.packageManager.getLaunchIntentForPackage(node.packageName)
                                        intent?.let { context.startActivity(it) }
                                        isSearchVisible = false
                                        searchQuery = ""
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(node.label.take(1), color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = node.label, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Onboarding Overlay
        if (onboardingStep > 0) {
            OnboardingOverlay(step = onboardingStep)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { 
                            viewModel.completeOnboarding()
                            onboardingStep = 0
                        }
                    ) {
                        Text("Lewati", color = Color.White.copy(alpha = 0.7f))
                    }
                    
                    if (onboardingStep == 3) {
                        Button(
                            onClick = { 
                                viewModel.completeOnboarding()
                                onboardingStep = 0
                            }
                        ) {
                            Text("Selesai")
                        }
                    }
                }
            }
        }
    }
}
