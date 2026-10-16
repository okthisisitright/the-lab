package dev.widgetlab.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import dev.widgetlab.R
import dev.widgetlab.core.*
import dev.widgetlab.data.*
import dev.widgetlab.media.MediaHub
import dev.widgetlab.ui.MainActivity
import dev.widgetlab.ui.WidgetConfigureActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ProviderCatalog {
    val classes = ProviderTypes.classes
    fun component(context: Context, kind: WidgetKind) = ComponentName(context, classes.getValue(kind))
    fun kind(context: Context, id: Int): WidgetKind? {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider ?: return null
        return WidgetKind.entries.firstOrNull { component(context,it)==provider }
    }
    fun installed(context: Context): List<Pair<Int, WidgetKind>> = WidgetKind.entries.flatMap { k -> AppWidgetManager.getInstance(context).getAppWidgetIds(component(context,k)).map { it to k } }
}

abstract class LabProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { RefreshScheduling.ensure(context) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { ConfigStore(context).delete(it) } }
    override fun onRestored(context: Context, old: IntArray, new: IntArray) {
        old.zip(new).forEach { (a,b) -> ProviderCatalog.kind(context,b)?.let { ConfigStore(context).save(b,ConfigStore(context).read(a,it)); ConfigStore(context).delete(a) } }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context,intent)
        if(intent.action in listOf(AppWidgetManager.ACTION_APPWIDGET_UPDATE,AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,AppWidgetManager.ACTION_APPWIDGET_RESTORED)) {
            val pending=goAsync()
            val ids=intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)?.toSet()
                ?: setOf(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1))
            WidgetUpdates.scope.launch { try { WidgetUpdates.updateIds(context.applicationContext,ids) } finally { pending.finish() } }
        }
    }
}

object WidgetUpdates {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex=Mutex()
    private val requests=Channel<Context>(Channel.CONFLATED)
    private val fullRequested=java.util.concurrent.atomic.AtomicBoolean(false)
    init { scope.launch { for(context in requests) { delay(180); updateIds(context,null,musicOnly=!fullRequested.getAndSet(false)) } } }
    fun request(context: Context, musicOnly: Boolean = false) { if(!musicOnly)fullRequested.set(true); requests.trySend(context.applicationContext) }
    suspend fun updateAll(context: Context) = updateIds(context,null)
    suspend fun updateIds(context: Context,ids: Set<Int>?,musicOnly:Boolean=false) = mutex.withLock {
        val installed=ProviderCatalog.installed(context).filter { (ids==null || it.first in ids) && (!musicOnly||it.second.isMusic) }
        if(installed.isEmpty()) return@withLock
        val snapshot=RacingRepository(context).snapshot()
        val battery=DeviceData.battery(context)
        val agenda=if(installed.any { it.second.hasAgenda }) DeviceData.agenda(context) else AgendaData(false, emptyList())
        installed.forEach { (id, kind) ->
            try {
                val config=ConfigStore(context).read(id,kind).copy(kind=kind)
                val media=MediaHub.info(config.selection)
                val data=RenderData(snapshot,media,battery,agenda,MediaHub.permitted(context),focus=readFocus(context))
                update(context,id,config,data)
            } catch (e: Exception) { android.util.Log.w("WidgetLab","Widget $id update failed: ${e.javaClass.simpleName}") }
        }
    }
    fun update(context: Context,id: Int,cfg: WidgetConfig,data: RenderData) {
        val manager=AppWidgetManager.getInstance(context)
        val options=manager.getAppWidgetOptions(id)
        val portrait=android.util.SizeF(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300).coerceAtLeast(100).toFloat(),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,cfg.kind.height*300/420).coerceAtLeast(54).toFloat())
        val landscape=android.util.SizeF(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,portrait.width.toInt()).coerceAtLeast(100).toFloat(),options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,portrait.height.toInt()).coerceAtLeast(54).toFloat())
        val density=context.resources.displayMetrics.density.coerceIn(1f,2.5f)
        val views=if(android.os.Build.VERSION.SDK_INT>=31) {
            @Suppress("DEPRECATION") val sizes=options.getParcelableArrayList<android.util.SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)?.distinct()?.filter{it.width>=80 && it.height>=48}?.take(6).orEmpty().ifEmpty{listOf(portrait,landscape).distinct()}
            val map=sizes.associateWith{size->WidgetViews.build(context,id,cfg,data,size.width,size.height,density,420_000/sizes.size)}
            RemoteViews(map)
        }else{
            RemoteViews(WidgetViews.build(context,id,cfg,data,landscape.width,landscape.height,density,210_000),WidgetViews.build(context,id,cfg,data,portrait.width,portrait.height,density,210_000))
        }
        manager.updateAppWidget(id,views)
    }

}

class WidgetActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val id=intent.getIntExtra("widgetId",-1)
        val kind=ProviderCatalog.kind(context,id) ?: return
        if(!kind.isMusic) return
        val config=ConfigStore(context).read(id,kind)
        MediaHub.control(context,intent.getStringExtra("control").orEmpty(),config.selection)
    }
}
class PinReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
        val kind=ProviderCatalog.kind(context,id) ?: return
        val cfg=ConfigStore(context).consume(intent.getStringExtra("token").orEmpty()) ?: return
        if(cfg.kind!=kind) return
        val store=ConfigStore(context)
        if(!store.contains(id)) store.save(id,cfg)
        store.remember(store.read(id,kind))
        val pending=goAsync();WidgetUpdates.scope.launch {try{WidgetUpdates.updateAll(context.applicationContext)}finally{pending.finish()}}
    }
}
