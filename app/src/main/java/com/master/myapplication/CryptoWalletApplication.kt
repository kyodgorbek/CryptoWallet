package com.master.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CryptoWalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()

    }
}
