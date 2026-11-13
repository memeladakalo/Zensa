package com.zensa.app

import android.app.Application
import android.util.Log
import com.zensa.app.di.ServiceLocator
import com.zensa.app.notifications.NotificationHelper
import com.zensa.app.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * ZensaApplication - runs once when the process starts.
 *
 * Offline mode: besides the WorkManager periodic job (SyncScheduler.schedulePeriodic) and the
 * one-shot job queued after every offline write, this class listens to connectivity. The moment
 * the phone goes from offline to online it requests an immediate sync, so pending changes are
 * uploaded as soon as the connection returns.
 */
class ZensaApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        NotificationHelper.createChannel(this)
        SyncScheduler.schedulePeriodic(this)

        // Sync as soon as connectivity returns.
        appScope.launch {
            ServiceLocator.networkMonitor().isOnlineFlow
                .drop(1)               // ignore the initial value emitted at start-up
                .filter { it }         // only "became online"
                .collect {
                    Log.i(TAG, "Connectivity restored - requesting sync")
                    SyncScheduler.requestImmediateSync(this@ZensaApplication)
                }
        }
    }

    private companion object {
        const val TAG = "ZensaApplication"
    }
}
