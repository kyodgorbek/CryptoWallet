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
            val wallets = withTimeout(10000L) {
                sdk.wallets.userWallets
            }
            
            val wallet = wallets.firstOrNull { 
                it.chain.uppercase() == "EVM" || it.chain.uppercase() == "ETHEREUM"
            } ?: throw Exception("No EVM wallet linked")
            
            // Try to get actual chainId from the wallet if possible
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

            val balance = try {
                withTimeout(15000L) {
                    sdk.wallets.getBalance(wallet)
                } ?: "0"
            } catch (e: Exception) {
                Log.e(TAG, "Error getting balance", e)
                "0.00"
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
            Log.e(TAG, "Error in getWalletInfo", e)
            Result.failure(e)
        }
    }

    override suspend fun switchNetwork(chainId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Switching network to $chainId")
            val wallet = sdk.wallets.userWallets.firstOrNull { it.chain.uppercase() == "EVM" }
                ?: throw Exception("No EVM wallet linked")

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

            withTimeout(15000L) {
                sdk.wallets.switchNetwork(wallet, Network(networkJson))
            }

            currentChainId = chainId
            currentNetworkName = genericNetwork.name ?: genericNetwork.vanityName ?: "Chain $chainId"

            Log.d(TAG, "Network switch success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching network", e)
            Result.failure(e)
        }
    }

    override suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Sending transaction: $request")
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

            val txHash = withTimeout(30000L) {
                sdk.evm.sendTransaction(transaction, wallet)
            }
            Log.d(TAG, "Transaction sent. Hash: $txHash")
            Result.success(TransactionResult(txHash = txHash, success = true))
        } catch (e: Exception) {
            Log.e(TAG, "Error sending transaction", e)
            Result.failure(e)
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

