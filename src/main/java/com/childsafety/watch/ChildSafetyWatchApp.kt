package com.childsafety.watch

import android.app.Application
import com.childsafety.watch.data.local.AppDatabase
import com.childsafety.watch.data.remote.WatchApiService
import com.childsafety.watch.data.sync.SyncRepository

data class WatchSafetyStatus(
    val childStatus: String = "SAFE",
    val dispatchStatus: String = "NONE", // "NONE", "PENDING", "ACKNOWLEDGED", "RESPONDING", "RESOLVED"
    val responderName: String? = null
)

class ChildSafetyWatchApp : Application() {

    companion object {
        val safetyStatusFlow = kotlinx.coroutines.flow.MutableStateFlow(WatchSafetyStatus())
    }

    lateinit var database: AppDatabase
        private set

    lateinit var apiService: WatchApiService
        private set

    lateinit var syncRepository: SyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        val savedBaseUrl = prefs.getString("server_url", null) ?: "http://10.0.2.2:8000/api/v1"
        database = AppDatabase.getInstance(this)
        apiService = WatchApiService(savedBaseUrl)
        syncRepository = SyncRepository(database.pendingEventDao(), apiService)
    }
}

