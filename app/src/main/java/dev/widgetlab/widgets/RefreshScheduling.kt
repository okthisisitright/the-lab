package dev.widgetlab.widgets

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.*
import dev.widgetlab.core.AppPreferences
import dev.widgetlab.data.RacingRepository
import dev.widgetlab.media.MediaHub
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LabApplication: Application() {
    private val batteryChanges=object: BroadcastReceiver() { override fun onReceive(context: Context,intent: Intent) {WidgetUpdates.request(context)} }
    override fun onCreate() {
        super.onCreate()
        registerReceiver(batteryChanges,IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        })
        RefreshScheduling.ensure(this)
        MediaHub.connect(this)
    }
}
object RefreshScheduling {
    private val initialized=java.util.concurrent.atomic.AtomicBoolean(false)
    fun ensure(context: Context) {
        if(!initialized.compareAndSet(false,true))return
        try{
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("widget-system-v1",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<SystemRefreshWorker>(30,TimeUnit.MINUTES).build())
            WidgetUpdates.scope.launch { racing(context,AppPreferences(context).automatic.first()) }
        }catch(error:RuntimeException){initialized.set(false);throw error}
    }
    fun racing(context: Context,enabled: Boolean) {
        val manager=WorkManager.getInstance(context)
        if(!enabled) { manager.cancelUniqueWork("racing-sync-v1");return }
        val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        manager.enqueueUniquePeriodicWork("racing-sync-v1",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<RacingRefreshWorker>(6,TimeUnit.HOURS).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES).build())
    }
}
class SystemRefreshWorker(context: Context,parameters: WorkerParameters): CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result {WidgetUpdates.updateAll(applicationContext);return Result.success()}
}
class RacingRefreshWorker(context: Context,parameters: WorkerParameters): CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result {
        if(!AppPreferences(applicationContext).automatic.first()) return Result.success()
        val errors=RacingRepository(applicationContext).refresh()
        WidgetUpdates.updateAll(applicationContext)
        return if(errors.isEmpty() || errors.first().startsWith("Please wait")) Result.success() else Result.retry()
    }
}
class SystemReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,Intent.ACTION_POWER_CONNECTED,Intent.ACTION_POWER_DISCONNECTED)) return
        RefreshScheduling.ensure(context)
        val pending=goAsync();WidgetUpdates.scope.launch {try{WidgetUpdates.updateAll(context.applicationContext)}finally{pending.finish()}}
    }
}
