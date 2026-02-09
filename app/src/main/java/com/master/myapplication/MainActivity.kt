package com.master.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.core.ClientProps
import com.dynamic.sdk.android.core.LoggerLevel
import com.master.myapplication.ui.theme.CryptoWalletTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val props = ClientProps(
            environmentId = "3e219b76-dcf1-40ab-aad6-652c4dfab4cc",
            appLogoUrl = "https://demo.dynamic.xyz/favicon-32x32.png",
            appName = "Crypto Wallet",
            redirectUrl = "cryptowallet://",
            appOrigin = "https://demo.dynamic.xyz",
            logLevel = LoggerLevel.DEBUG,
        )
        DynamicSDK.initialize(props, applicationContext, this)
        android.util.Log.d("MainActivity", "Dynamic SDK initialized")

        setContent {
            CryptoWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CryptoWalletApp()
                }
            }
        }
    }
}
