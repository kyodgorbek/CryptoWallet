package com.master.myapplication.ui.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.master.myapplication.data.model.TransactionRequest
import com.master.myapplication.data.model.TransactionState
import com.master.myapplication.data.repository.DynamicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    private val repository: DynamicRepository
) : ViewModel() {

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    fun sendTransaction(recipientAddress: String, amount: String) {
        val cleanAddress = recipientAddress.trim()
        val cleanAmount = amount.trim().replace(",", ".")

        // Validate inputs
        if (cleanAddress.isEmpty()) {
            _transactionState.value = TransactionState.Error("Please enter a recipient address")
            return
        }

        if (!isValidAddress(cleanAddress)) {
            _transactionState.value = TransactionState.Error("Invalid recipient address. Must be '0x' followed by 40 hex characters.")
            return
        }

        if (cleanAmount.isEmpty()) {
            _transactionState.value = TransactionState.Error("Please enter an amount")
            return
        }

        if (!isValidAmount(cleanAmount)) {
            _transactionState.value = TransactionState.Error("Invalid amount format")
            return
        }

        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            
            val request = TransactionRequest(
                recipientAddress = if (cleanAddress.startsWith("0x")) cleanAddress else "0x$cleanAddress",
                amount = cleanAmount
            )

            repository.sendTransaction(request)
                .onSuccess { result ->
                    _transactionState.value = TransactionState.Success(result)
                }
                .onFailure { error ->
                    _transactionState.value = TransactionState.Error(
                        error.message ?: "Transaction failed"
                    )
                }
        }
    }

    fun resetTransactionState() {
        _transactionState.value = TransactionState.Idle
    }

    private fun isValidAddress(address: String): Boolean {
        val normalized = if (address.startsWith("0x", ignoreCase = true)) address.substring(2) else address
        return normalized.matches(Regex("^[a-fA-F0-9]{40}$"))
    }

    private fun isValidAmount(amount: String): Boolean {
        return try {
            val value = amount.toDouble()
            value > 0
        } catch (e: Exception) {
            false
        }
    }
}
