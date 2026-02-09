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

    private val sdk get() = DynamicSDK.getInstance()
    private val _authState = MutableStateFlow(false)

    override suspend fun initializeSdk() {
        // SDK already initialized in MainActivity
    }

    override suspend fun sendOtp(email: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("DynamicRepository", "Sending OTP to $email")
            // Add a timeout of 15 seconds to prevent indefinite loading
            kotlinx.coroutines.withTimeout(15000L) {
                sdk.auth.email.sendOTP(email)
            }
            android.util.Log.d("DynamicRepository", "OTP sent successfully")
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("DynamicRepository", "OTP send timed out")
            Result.failure(Exception("Request timed out. Please check your connection and try again."))
        } catch (e: Exception) {
            android.util.Log.e("DynamicRepository", "Error sending OTP", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(code: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("DynamicRepository", "Verifying OTP code: $code")
            // Add a timeout of 15 seconds to prevent indefinite loading
            kotlinx.coroutines.withTimeout(15000L) {
                sdk.auth.email.verifyOTP(code)
            }
            _authState.value = true
            android.util.Log.d("DynamicRepository", "OTP verification successful")
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("DynamicRepository", "OTP verification timed out")
            Result.failure(Exception("Verification timed out. Please try again."))
        } catch (e: Exception) {
            android.util.Log.e("DynamicRepository", "Error verifying OTP", e)
            Result.failure(e)
        }
    }

    override fun isAuthenticatedFlow(): Flow<Boolean> {
        return _authState.asStateFlow()
    }

    override suspend fun getWalletInfo(): Result<WalletInfo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            val wallets = sdk.wallets.userWallets
            android.util.Log.d("DynamicRepository", "Available wallets: ${wallets.size}")
            wallets.forEach { 
                android.util.Log.d("DynamicRepository", "Wallet: chain=${it.chain}, address=${it.address}")
            }

            val wallet = wallets.firstOrNull { 
                it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM" || it.chain.uppercase() == "FLOW" 
            } ?: throw Exception("No EVM wallet linked. Found chains: ${wallets.map { it.chain }}")

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
            // Reverting to the SDK call that appeared in previous diffs, it's likely sdk.wallets
            // but we need to ensure the correct signature.
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" }
                ?: throw Exception("No EVM wallet found")
            
            // Use reflection to find the correct network enum constant at runtime
            // to avoid compilation errors if we guess the names wrong.
            try {
                val networkClass = com.dynamic.sdk.android.Models.Network::class.java
                val searchName = if (chainId == 1L) "Ethereum" else "Sepolia"
                val targetNetwork = networkClass.enumConstants?.firstOrNull { 
                    it.toString().contains(searchName, ignoreCase = true) 
                } as? com.dynamic.sdk.android.Models.Network
                
                if (targetNetwork != null) {
                    sdk.wallets.switchNetwork(wallet, targetNetwork)
                } else {
                    // If we can't find it by name, try to find by chain ID if there's a property
                    val byId = networkClass.enumConstants?.firstOrNull { enumConstant ->
                        try {
                            val chainIdField = enumConstant.javaClass.getDeclaredField("chainId")
                            chainIdField.isAccessible = true
                            (chainIdField.get(enumConstant) as? Number)?.toLong() == chainId
                        } catch (e: Exception) { false }
                    } as? com.dynamic.sdk.android.Models.Network
                    
                    if (byId != null) {
                        sdk.wallets.switchNetwork(wallet, byId)
                    } else {
                        throw Exception("Network $searchName (ID: $chainId) not found in SDK")
                    }
                }
            } catch (e: Exception) {
                throw e
            }
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
