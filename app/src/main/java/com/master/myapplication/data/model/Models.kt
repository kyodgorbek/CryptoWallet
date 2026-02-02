package com.master.myapplication.data.model

data class WalletInfo(
    val address: String,
    val balance: String,
    val network: String = "Sepolia",
    val chainId: Long = 11155111
)

data class TransactionRequest(
    val recipientAddress: String,
    val amount: String
)

data class TransactionResult(
    val txHash: String,
    val success: Boolean,
    val error: String? = null
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class WalletState {
    object Loading : WalletState()
    data class Success(val walletInfo: WalletInfo) : WalletState()
    data class Error(val message: String) : WalletState()
}

sealed class TransactionState {
    object Idle : TransactionState()
    object Loading : TransactionState()
    data class Success(val result: TransactionResult) : TransactionState()
    data class Error(val message: String) : TransactionState()
}
