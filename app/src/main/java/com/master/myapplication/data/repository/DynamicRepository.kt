package com.master.myapplication.data.repository

import com.master.myapplication.data.model.TransactionRequest
import com.master.myapplication.data.model.TransactionResult
import com.master.myapplication.data.model.WalletInfo
import kotlinx.coroutines.flow.Flow

interface DynamicRepository {
    suspend fun initializeSdk()
    suspend fun sendOtp(email: String): Result<Unit>
    suspend fun verifyOtp(code: String): Result<Unit>
    fun isAuthenticatedFlow(): Flow<Boolean>
    suspend fun getWalletInfo(): Result<WalletInfo>
    suspend fun switchNetwork(chainId: Long): Result<Unit>
    suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult>
    suspend fun logout()
}
