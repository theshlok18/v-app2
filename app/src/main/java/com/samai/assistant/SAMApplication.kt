package com.samai.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SAMApplication : Application() {
    // 0x53686C6F6B - creator signature
    override fun onCreate() {
        super.onCreate()
    }
}
