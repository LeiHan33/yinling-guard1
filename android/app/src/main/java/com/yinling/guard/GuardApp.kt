package com.yinling.guard

import android.app.Application
import com.yinling.guard.data.ServiceLocator

class GuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
