package com.mystudytracker.app

import android.app.Application
import com.mystudytracker.app.data.AppDatabase
import com.mystudytracker.app.data.ProgressRepository

/** Holds the single, app-wide database/repository instance - no DI framework needed for an app this small. */
class MyStudyTrackerApplication : Application() {
    val repository: ProgressRepository by lazy {
        val db = AppDatabase.getInstance(this)
        ProgressRepository(db.dailyProgressDao(), db.dailyAttachmentDao())
    }
}
