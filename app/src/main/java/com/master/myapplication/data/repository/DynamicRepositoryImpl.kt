package com.master.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.Network
import com.master.myapplication.data.model.TransactionRequest
import com.master.myapplication.data.model.TransactionResult
import com.master.myapplication.data.model.WalletInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import com.dynamic.sdk.android.Chains.EVM.EthereumTransaction
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DynamicRepository {

    private val TAG = "DynamicRepository"
    private val sdk get() = DynamicSDK.getInstance()
    private val _authState = MutableStateFlow(false)

    private var currentChainId = 11155111L
    private var currentNetworkName = "Sepolia"

    override suspend fun initializeSdk() {
        // SDK already initialized in MainActivity
    }

    override suspend fun sendOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Sending OTP to $email")
            withTimeout(15000L) {
                sdk.auth.email.sendOTP(email)
            }
            Log.d(TAG, "OTP sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending OTP", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Verifying OTP code: $code")
            withTimeout(15000L) {
                sdk.auth.email.verifyOTP(code)
            }
            _authState.value = true
            Log.d(TAG, "OTP verification successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying OTP", e)
            Result.failure(e)
        }
    }

    override fun isAuthenticatedFlow(): Flow<Boolean> {
        return _authState.asStateFlow()
    }

    override suspend fun getWalletInfo(): Result<WalletInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching wallet info...")
            
            // Reduced to 3 attempts with shorter fixed delays to avoid long loading states
            val wallet = (0 until 3).firstNotNullOfOrNull { attempt ->
                if (attempt > 0) {
                    Log.d(TAG, "Quick retry for wallet (attempt ${attempt + 1}/3)...")
                    kotlinx.coroutines.delay(500L) 
                }
                
                val wallets = try {
                    withTimeout(5000L) { sdk.wallets.userWallets }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching userWallets: ${e.message}")
                    emptyList()
                }

                wallets.firstOrNull { 
                    it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
                }
            } ?: throw Exception("No EVM wallet found. If this persists, please try to log in again.")

            Log.d(TAG, "EVM wallet found: ${wallet.address}")
            
            val detectedChainId = try {
                val field = wallet.javaClass.getDeclaredField("chainId")
                field.isAccessible = true
                (field.get(wallet) as? Number)?.toLong() ?: currentChainId
            } catch (e: Exception) {
                try {
                    val method = wallet.javaClass.getMethod("getChainId")
                    (method.invoke(wallet) as? Number)?.toLong() ?: currentChainId
                } catch (e2: Exception) {
                    currentChainId
                }
            }
            
            Log.d(TAG, "Detected Chain ID from wallet: $detectedChainId")

            var balance = "0.00"
            var retryCount = 0
            val maxBalanceRetries = 3 // Reduced from 5
            
            while (retryCount < maxBalanceRetries) {
                try {
                    val result = withTimeout(15000L) {
                        sdk.wallets.getBalance(wallet)
                    }
                    if (result != null) {
                        balance = result
                        Log.d(TAG, "Balance fetched successfully: $balance")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting balance (attempt ${retryCount + 1}/$maxBalanceRetries): ${e.message}")
                    retryCount++
                    if (retryCount < maxBalanceRetries) {
                        kotlinx.coroutines.delay(1000L * retryCount)
                    }
                }
            }
            
            val networkName = when(detectedChainId) {
                1L -> "Ethereum Mainnet"
                11155111L -> "Sepolia"
                else -> "Network $detectedChainId"
            }

            Result.success(WalletInfo(
                address = wallet.address,
                balance = balance,
                network = networkName,
                chainId = detectedChainId
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure in getWalletInfo", e)
            Result.failure(e)
        }
    }

    override suspend fun switchNetwork(chainId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Switching network to chainId: $chainId")
            
            val wallets = sdk.wallets.userWallets
            val wallet = wallets.firstOrNull { it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM" }
                ?: throw Exception("No EVM wallet linked for switching")

            val evmNetworks = sdk.networks.evm
            Log.d(TAG, "Available EVM networks: ${evmNetworks.map { "${it.name}(${it.chainId})" }}")

            val genericNetwork = evmNetworks.firstOrNull { gn ->
                gn.chainId.jsonPrimitive.longOrNull == chainId
            } ?: throw Exception("Network $chainId not found in SDK's available EVM networks. Please check Dynamic dashboard.")

            val networkJson = buildJsonObject {
                put("chainId", chainId)
                put("networkId", chainId)
                put("id", chainId.toString())
                genericNetwork.name?.let { put("name", it) }
                genericNetwork.chainName?.let { put("chainName", it) }
                genericNetwork.vanityName?.let { put("vanityName", it) }
            }

            Log.d(TAG, "Requesting switch with JSON: $networkJson")
            sdk.wallets.switchNetwork(wallet, Network(networkJson))

            currentChainId = chainId
            currentNetworkName = genericNetwork.name ?: genericNetwork.vanityName ?: "Chain $chainId"

            Log.d(TAG, "Network switch success reported by SDK")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching network: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Preparing to send transaction: $request")
            
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM" }
                ?: throw Exception("No EVM wallet found. Please reconnect.")

            // Verify current network context
            val walletChainId = try {
                val field = wallet.javaClass.getDeclaredField("chainId")
                field.isAccessible = true
                (field.get(wallet) as? Number)?.toLong()
            } catch (e: Exception) {
                try {
                    val method = wallet.javaClass.getMethod("getChainId")
                    (method.invoke(wallet) as? Number)?.toLong()
                } catch (e2: Exception) {
                    null
                }
            }

            Log.d(TAG, "Transaction wallet address: ${wallet.address}, current chainId: $walletChainId, expected: $currentChainId")

            // If chainId mismatch, attempt a quick switch
            if (walletChainId != null && walletChainId != currentChainId) {
                Log.w(TAG, "Network mismatch detected (Wallet: $walletChainId, App: $currentChainId). Attempting auto-switch...")
                switchNetwork(currentChainId).getOrThrow()
                // Wait a bit for SDK to catch up
                kotlinx.coroutines.delay(1000L)
            }

            // Optional: Re-fetch balance to verify funds before attempting
            val currentBalance = try {
                sdk.wallets.getBalance(wallet) ?: "0"
            } catch (e: Exception) {
                "Unknown"
            }
            Log.d(TAG, "Verified balance for transaction: $currentBalance ETH")

            val transaction = EthereumTransaction(
                from = wallet.address,
                to = request.recipientAddress,
                value = convertEthToWei(request.amount),
                gas = BigInteger.valueOf(21000),
                // Slightly higher priority fees for Sepolia reliability
                maxFeePerGas = BigInteger.valueOf(5000000000L), // 5 gwei
                maxPriorityFeePerGas = BigInteger.valueOf(2000000000L) // 2 gwei
            )

            Log.d(TAG, "Dispatching transaction to SDK...")
            val txHash = withTimeout(45000L) {
                sdk.evm.sendTransaction(transaction, wallet)
            }
            
            Log.d(TAG, "Transaction broadcast successful. Hash: $txHash")
            Result.success(TransactionResult(txHash = txHash, success = true))
        } catch (e: Exception) {
            Log.e(TAG, "Transaction failed", e)
            val errorMessage = when {
                e.message?.contains("insufficient funds", ignoreCase = true) == true -> {
                    "Insufficient funds: You need more ETH on $currentNetworkName to cover the amount and gas fees."
                }
                e.message?.contains("user rejected", ignoreCase = true) == true -> {
                    "Transaction cancelled by user"
                }
                else -> e.message ?: "An unexpected error occurred during transaction"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                sdk.auth.logout()
                _authState.value = false
                Log.d(TAG, "Logout successful")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
            }
        }
    }

    private fun convertEthToWei(ethAmount: String): BigInteger {
        return try {
            val eth = BigDecimal(ethAmount.replace(",", "."))
            val wei = eth.multiply(BigDecimal("1000000000000000000"))
            wei.toBigInteger()
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }
}

