package com.nebula.launcher.feature.home

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.launcher.core.model.AppCluster
import com.nebula.launcher.core.model.AppNode
import com.nebula.launcher.core.model.RadialAction
import com.nebula.launcher.core.model.WeatherData
import com.nebula.launcher.feature.home.data.AppRepository
import com.nebula.launcher.feature.home.data.ShortcutRepository
import com.nebula.launcher.feature.home.domain.AppClassifier
import com.nebula.launcher.feature.home.domain.LayoutEngine
import com.nebula.launcher.feature.home.domain.UsageRanker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.nebula.launcher.core.data.SettingsRepository
import com.nebula.launcher.core.database.AppNodeDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val usageRanker: UsageRanker,
    private val appClassifier: AppClassifier,
    private val layoutEngine: LayoutEngine,
    private val shortcutRepository: ShortcutRepository,
    private val settingsRepository: SettingsRepository,
    private val appNodeDao: AppNodeDao
) : ViewModel() {

    val isFocusModeEnabled: StateFlow<Boolean> = settingsRepository.isFocusModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isDynamicColorEnabled: StateFlow<Boolean> = settingsRepository.isDynamicColorEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val hasCompletedOnboarding: StateFlow<Boolean> = settingsRepository.hasCompletedOnboarding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val physicsStrength: StateFlow<Float> = settingsRepository.physicsStrength
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val showAppList: StateFlow<Boolean> = settingsRepository.showAppList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setHasCompletedOnboarding(true)
        }
    }

    private val _appNodes = MutableStateFlow<List<AppNode>>(emptyList())
    val appNodes: StateFlow<List<AppNode>> = _appNodes.asStateFlow()

    private val _appClusters = MutableStateFlow<List<AppCluster>>(emptyList())
    val appClusters: StateFlow<List<AppCluster>> = _appClusters.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    private val _selectedNodeMenu = MutableStateFlow<String?>(null)
    val selectedNodeMenu: StateFlow<String?> = _selectedNodeMenu.asStateFlow()

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    init {
        // Mock weather data
        _weatherData.value = WeatherData(
            temperature = 28,
            condition = "Sunny",
            city = "Jakarta",
            description = "Cerah Berawan"
        )
    }

    private var physicsJob: kotlinx.coroutines.Job? = null

    fun checkPermissionAndLoadApps(screenWidth: Float, screenHeight: Float) {
        val granted = checkUsageStatsPermission()
        _hasUsagePermission.value = granted
        
        viewModelScope.launch {
            val savedNodes = appNodeDao.getAllNodes()
            if (savedNodes.isNotEmpty()) {
                // Load from DB
                _appNodes.value = savedNodes
                // We still need clusters for gravity. Let's re-classify just to get clusters, 
                // but keep saved positions.
                val (_, clusters) = appClassifier.classify(savedNodes, screenWidth, screenHeight)
                _appClusters.value = clusters
            } else {
                // First time or empty DB
                var nodes = appRepository.loadInstalledApps(screenWidth, screenHeight)
                if (granted) {
                    nodes = usageRanker.rank(nodes)
                }
                
                val (classifiedNodes, clusters) = appClassifier.classify(nodes, screenWidth, screenHeight)
                _appNodes.value = classifiedNodes
                _appClusters.value = clusters
            }
            
            startPhysicsSimulation()
        }
    }

    fun saveNodes() {
        viewModelScope.launch {
            val currentNodes = _appNodes.value
            if (currentNodes.isNotEmpty()) {
                appNodeDao.upsertAll(currentNodes)
            }
        }
    }

    fun stopPhysicsSimulation() {
        physicsJob?.cancel()
        physicsJob = null
    }

    fun resumePhysicsSimulation() {
        if (physicsJob == null || physicsJob?.isActive == false) {
            startPhysicsSimulation()
        }
    }

    private fun startPhysicsSimulation() {
        physicsJob?.cancel()
        physicsJob = viewModelScope.launch {
            while (isActive) {
                val currentNodes = _appNodes.value
                val currentClusters = _appClusters.value
                val strength = physicsStrength.value
                
                if (currentNodes.isNotEmpty() && currentClusters.isNotEmpty() && strength > 0.05f) {
                    val baseRadiusPx = 40f * context.resources.displayMetrics.density
                    val updatedNodes = layoutEngine.updatePositions(currentNodes, currentClusters, baseRadiusPx, strength)
                    
                    _appNodes.value = updatedNodes
                }
                
                // Adjust delay based on strength to save battery
                val frameDelay = if (strength < 0.5f) 32L else 16L
                delay(frameDelay)
            }
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openMenu(packageName: String) {
        _selectedNodeMenu.value = packageName
    }

    fun closeMenu() {
        _selectedNodeMenu.value = null
    }

    fun getShortcuts(packageName: String): List<RadialAction> {
        return shortcutRepository.getShortcutsForPackage(packageName)
    }
}
