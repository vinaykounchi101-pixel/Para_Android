package com.paradox.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParadoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
