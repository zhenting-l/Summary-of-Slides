package com.lzt.summaryofslides.data

import android.content.Context
import androidx.room.Room
import com.lzt.summaryofslides.data.db.AppDatabase
import com.lzt.summaryofslides.data.repo.EntryRepository
import com.lzt.summaryofslides.settings.SettingsStore

object AppContainer {
    lateinit var appContext: Context
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var entryRepository: EntryRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        settingsStore = SettingsStore(appContext)
        db =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "academic_report_assistant.db")
                .fallbackToDestructiveMigration()
                .build()
        entryRepository = EntryRepository(db, appContext)
    }
}
