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

    init {
        loadWalletInfo()
    }

    fun loadWalletInfo() {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            repository.getWalletInfo()
                .onSuccess { walletInfo ->
                    _walletState.value = WalletState.Success(walletInfo)
                }
                .onFailure { error ->
                    _walletState.value = WalletState.Error(
                        error.message ?: "Failed to load wallet information"
                    )
                }
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

    fun switchNetwork(chainId: Int) {
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
}
