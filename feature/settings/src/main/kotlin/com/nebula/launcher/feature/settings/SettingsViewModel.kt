package com.nebula.launcher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nebula.launcher.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.nebula.launcher.core.database.AppNodeDao

@HiltViewModel
class SettingsViewModel @Inject constructor(
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

    fun setFocusModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFocusModeEnabled(enabled)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setPhysicsStrength(strength: Float) {
        viewModelScope.launch {
            settingsRepository.setPhysicsStrength(strength)
        }
    }

    fun setShowAppList(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAppList(show)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.setHasCompletedOnboarding(false)
        }
    }

    fun resetNodePositions() {
        viewModelScope.launch {
            appNodeDao.deleteAll()
        }
    }
}
