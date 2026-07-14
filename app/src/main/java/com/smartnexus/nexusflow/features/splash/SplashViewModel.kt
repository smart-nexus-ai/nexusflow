package com.smartnexus.nexusflow.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.core.navigation.Screen
import com.smartnexus.nexusflow.data.local.dao.AppConfigDao
import com.smartnexus.nexusflow.data.remote.firebase.FirebaseRtdbService
import com.smartnexus.nexusflow.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val rtdbService: FirebaseRtdbService,
    private val appConfigDao: AppConfigDao,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<Screen>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            // 1. Fetch and update remote app configuration with 2.5s timeout
            withTimeoutOrNull(2500L) {
                try {
                    rtdbService.observeAppConfig().collect { config ->
                        if (config != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                appConfigDao.insertAppConfig(config)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to offline configurations
                }
            }

            // 2. Check current authentication session state
            val currentUser = authRepository.getCurrentUser().firstOrNull()
            val targetScreen = if (currentUser != null) {
                Screen.Home
            } else {
                Screen.Onboarding
            }

            _navigationEvent.emit(targetScreen)
        }
    }
}
