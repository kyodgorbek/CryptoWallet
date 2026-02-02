package com.master.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.master.myapplication.ui.login.LoginScreen
import com.master.myapplication.ui.login.LoginViewModel
import com.master.myapplication.ui.send.SendTransactionScreen
import com.master.myapplication.ui.wallet.WalletDetailsScreen

@Composable
fun CryptoWalletApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAuthenticated by loginViewModel.isAuthenticated.collectAsStateWithLifecycle(false,lifecycleOwner.lifecycle)

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) "wallet" else "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("wallet") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("wallet") {
            WalletDetailsScreen(
                onSendTransaction = {
                    navController.navigate("send")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("send") {
            SendTransactionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
