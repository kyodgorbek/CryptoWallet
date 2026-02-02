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
        // Validate inputs
        if (!isValidAddress(recipientAddress)) {
            _transactionState.value = TransactionState.Error("Invalid recipient address")
            return
        }

        if (!isValidAmount(amount)) {
            _transactionState.value = TransactionState.Error("Invalid amount")
            return
        }

        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            
            val request = TransactionRequest(
                recipientAddress = recipientAddress,
                amount = amount
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
        return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
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
