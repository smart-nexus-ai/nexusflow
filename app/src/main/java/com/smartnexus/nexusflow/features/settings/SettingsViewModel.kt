package com.smartnexus.nexusflow.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.core.theme.AppThemeConfig
import com.smartnexus.nexusflow.core.theme.ThemeMode
import com.smartnexus.nexusflow.data.local.dao.DeviceDao
import com.smartnexus.nexusflow.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileData(
    val name: String = "User",
    val email: String = "",
    val avatarInitials: String = "U",
    val memberSince: String = "July 2026",
    val pairedDevicesCount: Int = 0
)

data class SettingsUiState(
    val profile: UserProfileData = UserProfileData(),
    val isProfileDialogOpen: Boolean = false,
    val isLogoutDialogOpen: Boolean = false,
    val isDeleteAccountDialogOpen: Boolean = false,
    val deleteConfirmationText: String = "",
    val currentThemeMode: ThemeMode = ThemeMode.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceDao: DeviceDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Sync with global theme config
        _uiState.value = _uiState.value.copy(currentThemeMode = AppThemeConfig.themeMode.value)

        // Reactively load current authenticated user profile details
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { user ->
                if (user != null) {
                    val displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User"
                    val email = user.email ?: ""
                    val initials = displayName.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .take(2)
                        .uppercase()

                    val devicesCount = try {
                        deviceDao.getDevicesFlow().first().size
                    } catch (e: Exception) {
                        0
                    }

                    _uiState.value = _uiState.value.copy(
                        profile = UserProfileData(
                            name = displayName,
                            email = email,
                            avatarInitials = if (initials.isNotEmpty()) initials else "U",
                            memberSince = "July 2026",
                            pairedDevicesCount = devicesCount
                        )
                    )
                }
            }
        }
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        AppThemeConfig.setThemeMode(mode)
        _uiState.value = _uiState.value.copy(currentThemeMode = mode)
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun onOpenProfile() {
        _uiState.value = _uiState.value.copy(isProfileDialogOpen = true)
    }

    fun onCloseProfile() {
        _uiState.value = _uiState.value.copy(isProfileDialogOpen = false)
    }

    fun onOpenLogout() {
        _uiState.value = _uiState.value.copy(isLogoutDialogOpen = true)
    }

    fun onCloseLogout() {
        _uiState.value = _uiState.value.copy(isLogoutDialogOpen = false)
    }

    fun onOpenDeleteAccount() {
        _uiState.value = _uiState.value.copy(
            isDeleteAccountDialogOpen = true,
            deleteConfirmationText = ""
        )
    }

    fun onCloseDeleteAccount() {
        _uiState.value = _uiState.value.copy(isDeleteAccountDialogOpen = false)
    }

    fun onDeleteConfirmationTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(deleteConfirmationText = text)
    }
}
