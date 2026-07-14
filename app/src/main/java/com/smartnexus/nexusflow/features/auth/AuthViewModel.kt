package com.smartnexus.nexusflow.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnexus.nexusflow.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _authSuccessEvent = MutableSharedFlow<Unit>()
    val authSuccessEvent = _authSuccessEvent.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _authSuccessEvent.emit(Unit)
                }
                .onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Google Sign-In failed"
                }
            _isLoading.value = false
        }
    }
}
