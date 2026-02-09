package com.master.myapplication.data.repository

import android.content.Context
import com.dynamic.sdk.android.DynamicSDK
import com.master.myapplication.data.model.TransactionRequest
import com.master.myapplication.data.model.TransactionResult
import com.master.myapplication.data.model.WalletInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DynamicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DynamicRepository {

    private val sdk = DynamicSDK.getInstance()
    private val _authState = MutableStateFlow(false)

    override suspend fun initializeSdk() {
        // SDK already initialized in MainActivity
    }

    override suspend fun sendOtp(email: String): Result<Unit> {
        return try {
            sdk.auth.email.sendOTP(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(code: String): Result<Unit> {
        return try {
            sdk.auth.email.verifyOTP(code)
            _authState.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAuthenticatedFlow(): Flow<Boolean> {
        return _authState.asStateFlow()
    }

    override suspend fun getWalletInfo(): Result<WalletInfo> {
        return try {
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" }
                ?: throw Exception("No EVM wallet linked")

            val balance = sdk.wallets.getBalance(wallet) ?: "0"
            
            // Try to get actual chainId and network name from the wallet object
            // If the SDK version doesn't expose them directly, we fallback to defaults
            val chainId = try {
                // Accessing chainId if it exists on the wallet object
                val field = wallet.javaClass.getDeclaredField("chainId")
                field.isAccessible = true
                (field.get(wallet) as? Number)?.toLong() ?: 11155111L
            } catch (e: Exception) {
                11155111L
            }

            val networkName = when(chainId) {
                1L -> "Ethereum Mainnet"
                11155111L -> "Sepolia"
                else -> "Unknown Network"
            }

            val walletInfo = WalletInfo(
                address = wallet.address,
                balance = balance,
                network = networkName,
                chainId = chainId
            )
            Result.success(walletInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun switchNetwork(chainId: Long): Result<Unit> {
        return try {
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" }
                ?: throw Exception("No EVM wallet found")
            
            // Using the sdk.evm to switch network
            sdk.evm.switchNetwork(chainId.toString(), wallet)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult> {
        return try {
            // NOTE: EthereumTransaction class import is unresolved in this environment. 
            // The logic below is the intended implementation based on SDK docs.
            /*
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" }
                ?: throw Exception("No EVM wallet found")

            val transaction = EthereumTransaction(
                from = wallet.address,
                to = request.recipientAddress,
                value = convertEthToWei(request.amount),
                gas = BigInteger.valueOf(21000),
                maxFeePerGas = BigInteger.valueOf(3000000000L), 
                maxPriorityFeePerGas = BigInteger.valueOf(1500000000L) 
            )

            val txHash = sdk.evm.sendTransaction(transaction, wallet)
            Result.success(TransactionResult(txHash = txHash, success = true))
            */
            Result.failure(Exception("Send Transaction temporarily disabled due to build environment limits"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sdk.auth.logout()
        _authState.value = false
    }

    private fun convertEthToWei(ethAmount: String): BigInteger {
        return try {
            val eth = BigDecimal(ethAmount)
            val wei = eth.multiply(BigDecimal("1000000000000000000"))
            wei.toBigInteger()
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }
}
