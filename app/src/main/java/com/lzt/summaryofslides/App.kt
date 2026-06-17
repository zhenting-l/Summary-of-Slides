package com.lzt.summaryofslides

import android.app.Application
import com.lzt.summaryofslides.data.AppContainer

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
