package com.master.myapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.master.myapplication.data.model.AuthState
import com.master.myapplication.data.repository.DynamicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: DynamicRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isAuthenticated = repository.isAuthenticatedFlow()

    init {
        viewModelScope.launch {
            repository.initializeSdk()
        }
    }

    fun sendOtp(email: String) {
        if (email.isBlank() || !isValidEmail(email)) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.sendOtp(email)
                .onSuccess {
                    _authState.value = AuthState.OtpSent
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(
                        error.message ?: "Failed to send OTP"
                    )
                }
        }
    }

    fun verifyOtp(code: String) {
        if (code.isBlank() || code.length != 6) {
            _authState.value = AuthState.Error("Please enter a valid 6-digit code")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.verifyOtp(code)
                .onSuccess {
                    _authState.value = AuthState.Authenticated
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(
                        error.message ?: "Invalid OTP code"
                    )
                }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
