package com.lzt.summaryofslides.data

import android.content.Context
import com.lzt.summaryofslides.daily.DailyRepository
import com.lzt.summaryofslides.daily.DailySettingsStore

object AppContainer {
    lateinit var appContext: Context
        private set
    lateinit var dailySettingsStore: DailySettingsStore
        private set
    lateinit var dailyRepository: DailyRepository
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        dailySettingsStore = DailySettingsStore(appContext)
        dailyRepository = DailyRepository(appContext)
    }
}
