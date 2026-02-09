package com.master.myapplication.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.master.myapplication.data.model.WalletState
import com.master.myapplication.data.repository.DynamicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: DynamicRepository
) : ViewModel() {

    private val _walletState = MutableStateFlow<WalletState>(WalletState.Loading)
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadWalletInfo()
    }

    fun loadWalletInfo(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (isRefreshing) {
                _isRefreshing.value = true
            } else {
                _walletState.value = WalletState.Loading
            }
            
            repository.getWalletInfo()
                .onSuccess { walletInfo ->
                    _walletState.value = WalletState.Success(walletInfo)
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(
                        error.message ?: "Failed to load wallet information"
                    )
                }
            _isRefreshing.value = false
        }
    }

    fun switchNetwork(chainId: Long) {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            repository.switchNetwork(chainId)
                .onSuccess {
                    loadWalletInfo()
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(
                        error.message ?: "Failed to switch network"
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
