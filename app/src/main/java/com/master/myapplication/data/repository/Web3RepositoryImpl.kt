package com.master.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.master.myapplication.data.model.TransactionRequest
import com.master.myapplication.data.model.TransactionResult
import com.master.myapplication.data.model.WalletInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3RepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DynamicRepository {

    private val TAG = "Web3Repository"
    
    private val SEPOLIA_RPC_URL = ""

    private var web3j: Web3j? = null
    private var credentials: Credentials? = null
    private val _isAuthenticated = MutableStateFlow(false)
    
    private var userEmail: String? = null
    private var privateKey: String? = null

    override suspend fun initializeSdk() {
        try {
            web3j = Web3j.build(HttpService(SEPOLIA_RPC_URL))
        } catch (e: Exception) {
        }
    }

    override suspend fun sendOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            userEmail = email
            delay(1000)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOtp(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (code.length != 6) {
                return@withContext Result.failure(Exception("Invalid OTP code"))
            }
            
            delay(1000)
            credentials = generateOrRetrieveWallet(userEmail ?: "default")
            _isAuthenticated.value = true
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAuthenticatedFlow(): Flow<Boolean> {
        return _isAuthenticated.asStateFlow()
    }

    override suspend fun getWalletInfo(): Result<WalletInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentCredentials = credentials 
                ?: return@withContext Result.failure(Exception("No wallet found"))
            
            val web3 = web3j 
                ?: return@withContext Result.failure(Exception("Web3j not initialized"))
            
            val balanceWei = web3.ethGetBalance(
                currentCredentials.address,
                DefaultBlockParameterName.LATEST
            ).send().balance
            
            val balanceEth = Convert.fromWei(
                balanceWei.toBigDecimal(),
                Convert.Unit.ETHER
            )
            
            val walletInfo = WalletInfo(
                address = currentCredentials.address,
                balance = balanceEth.setScale(4, BigDecimal.ROUND_HALF_UP).toString(),
                network = "Sepolia",
                chainId = 11155111
            )
            
            Result.success(walletInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun switchNetwork(chainId: Long): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendTransaction(request: TransactionRequest): Result<TransactionResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentCredentials = credentials 
                ?: return@withContext Result.failure(Exception("No wallet found"))
            
            val web3 = web3j 
                ?: return@withContext Result.failure(Exception("Web3j not initialized"))
            
            val amountWei = Convert.toWei(request.amount, Convert.Unit.ETHER).toBigInteger()
            
            val gasPrice = web3.ethGasPrice().send().gasPrice
            
            val nonce = web3.ethGetTransactionCount(
                currentCredentials.address,
                DefaultBlockParameterName.LATEST
            ).send().transactionCount
            
            val transactionManager = RawTransactionManager(
                web3,
                currentCredentials,
                11155111
            )
            
            val gasLimit = BigInteger.valueOf(21000)
            
            val transactionReceipt = transactionManager.sendTransaction(
                gasPrice,
                gasLimit,
                request.recipientAddress,
                "",
                amountWei
            )
            
            val txHash = transactionReceipt.transactionHash

            val result = TransactionResult(
                txHash = txHash,
                success = true
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        credentials = null
        privateKey = null
        userEmail = null
        _isAuthenticated.value = false
        Log.d(TAG, "User logged out")
    }

    private fun generateOrRetrieveWallet(identifier: String): Credentials {
        val keyPair = ECKeyPair.create(identifier.hashCode().toLong().toBigInteger())
        return Credentials.create(keyPair)
    }
}
