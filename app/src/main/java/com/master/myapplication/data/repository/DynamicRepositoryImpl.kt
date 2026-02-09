package com.master.myapplication.data.repository

import android.content.Context
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.Network
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
import com.dynamic.sdk.android.Chains.EVM.EthereumTransaction
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

@Singleton
class DynamicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DynamicRepository {

    private val sdk get() = DynamicSDK.getInstance()
    private val _authState = MutableStateFlow(false)

    private var currentChainId = 11155111L
    private var currentNetworkName = "Sepolia"

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
            kotlinx.coroutines.withTimeout(25000L) {
                var wallets = sdk.wallets.userWallets
                var retryCount = 0
                
                // Right after login, wallets might take a moment to appear
                while (wallets.isEmpty() && retryCount < 8) {
                    android.util.Log.d("DynamicRepository", "No wallets found yet, retrying... ($retryCount)")
                    kotlinx.coroutines.delay(2000L)
                    wallets = sdk.wallets.userWallets
                    retryCount++
                }

                if (wallets.isEmpty()) {
                    throw Exception("No wallets found after login. Please ensure your dashboard has Embedded Wallets enabled.")
                }

                val wallet = wallets.firstOrNull { 
                    it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
                } ?: throw Exception("No EVM wallet found. Available chains: ${wallets.map { it.chain }}")

                // Try to get balance with a shorter timeout
                var balance = try {
                    kotlinx.coroutines.withTimeout(10000L) {
                        sdk.wallets.getBalance(wallet)
                    } ?: "0"
                } catch (e: Exception) {
                    android.util.Log.e("DynamicRepository", "Error getting balance", e)
                    "0.00"
                }
                
                // Get initial chain info
                var chainId = try {
                    val field = wallet.javaClass.getDeclaredField("chainId")
                    field.isAccessible = true
                    (field.get(wallet) as? Number)?.toLong() ?: 11155111L
                } catch (e: Exception) {
                    android.util.Log.d("DynamicRepository", "Could not find chainId field, trying methods...")
                    try {
                        val method = wallet.javaClass.getMethod("getChainId")
                        (method.invoke(wallet) as? Number)?.toLong() ?: 11155111L
                    } catch (e2: Exception) {
                        11155111L
                    }
                }

                android.util.Log.d("DynamicRepository", "Detected Chain ID: $chainId")

                // Auto-switch to Sepolia if we are on Mainnet (as per task requirement)
                if (chainId == 1L) {
                    android.util.Log.d("DynamicRepository", "Auto-switching from Mainnet to Sepolia...")
                    try {
                        val switchResult = switchNetwork(11155111L)
                        if (switchResult.isSuccess) {
                            android.util.Log.d("DynamicRepository", "Switch success, re-fetching info...")
                            // Wait a bit and re-detect everything
                            kotlinx.coroutines.delay(2000L)
                            // Re-fetch wallets to get the updated state
                            wallets = sdk.wallets.userWallets
                            val updatedWallet = wallets.firstOrNull { 
                                it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
                            } ?: wallet
                            
                            // Re-fetch balance for the new network
                            balance = try {
                                kotlinx.coroutines.withTimeout(10000L) {
                                    sdk.wallets.getBalance(updatedWallet)
                                } ?: "0"
                            } catch (e: Exception) {
                                android.util.Log.e("DynamicRepository", "Error getting balance after switch", e)
                                balance
                            }
                            
                            chainId = 11155111L 
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DynamicRepository", "Auto-switch failed", e)
                    }
                }

                android.util.Log.d("DynamicRepository", "Final Balance: $balance on Chain: $chainId")

                val networkName = when(chainId) {
                    1L -> "Ethereum Mainnet"
                    11155111L -> "Sepolia"
                    else -> "Network ID: $chainId"
                }

                Result.success(WalletInfo(
                    address = wallet.address,
                    balance = balance,
                    network = networkName,
                    chainId = chainId
                ))
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("DynamicRepository", "getWalletInfo timed out")
            Result.failure(Exception("Wallet loading timed out. Please check your connection."))

        } catch (e: Exception) {
            android.util.Log.e("DynamicRepository", "Error in getWalletInfo", e)
            Result.failure(e)
        }
    }

    override suspend fun switchNetwork(chainId: Long): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            kotlinx.coroutines.withTimeout(15000L) {
                val wallet = sdk.wallets.userWallets.firstOrNull { 
                    it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
                } ?: throw Exception("No EVM wallet found")

                val genericNetwork = sdk.networks.evm.firstOrNull { gn ->
                    gn.chainId.jsonPrimitive.longOrNull == chainId
                } ?: throw Exception("Network with chainId $chainId not available")

                val networkJson = buildJsonObject {
                    put("chainId", chainId)
                    put("networkId", chainId)
                    genericNetwork.name?.let { put("name", it) }
                    genericNetwork.chainName?.let { put("chainName", it) }
                    genericNetwork.vanityName?.let { put("vanityName", it) }
                }

                sdk.wallets.switchNetwork(wallet, Network(networkJson))

                currentChainId = chainId
                currentNetworkName = genericNetwork.name ?: genericNetwork.vanityName ?: "Chain $chainId"
                
                // Wait a bit for the SDK to update its internal state
                kotlinx.coroutines.delay(2000L)
            }
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("DynamicRepository", "switchNetwork timed out")
            Result.failure(Exception("Network switch timed out. Please try again."))
        } catch (e: Exception) {
            android.util.Log.e("DynamicRepository", "Error switching network", e)
            Result.failure(e)
        }
    }

    override suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val wallet = sdk.wallets.userWallets.firstOrNull { 
                it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
            } ?: throw Exception("No EVM wallet found")

            val transaction = EthereumTransaction(
                from = wallet.address,
                to = request.recipientAddress,
                value = convertEthToWei(request.amount),
                gas = BigInteger.valueOf(21000),
                maxFeePerGas = BigInteger.valueOf(3000000000L), // 3 Gwei
                maxPriorityFeePerGas = BigInteger.valueOf(1500000000L) // 1.5 Gwei
            )

            android.util.Log.d("DynamicRepository", "Sending transaction: $request")
            val txHash = sdk.evm.sendTransaction(transaction, wallet)
            android.util.Log.d("DynamicRepository", "Transaction sent. Hash: $txHash")
            Result.success(TransactionResult(txHash = txHash, success = true))
        } catch (e: Exception) {
            android.util.Log.e("DynamicRepository", "Error sending transaction", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sdk.auth.logout()
        _authState.value = false
    }

    private fun convertEthToWei(amount: String): BigInteger {
        return try {
            val cleanAmount = amount.replace(",", ".")
            val decimalAmount = BigDecimal(cleanAmount)
            val weiFactor = BigDecimal.TEN.pow(18)
            decimalAmount.multiply(weiFactor).toBigInteger()
        } catch (e: Exception) {
            BigInteger.ZERO
        }
    }
}
