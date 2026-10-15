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
    val classes = listOf(DriverWidget::class.java, ConstructorWidget::class.java, DriverStandingsWidget::class.java, ConstructorStandingsWidget::class.java, WeekendWidget::class.java, CountdownWidget::class.java, ResultsWidget::class.java, EditorialWidget::class.java, GlassWidget::class.java, VinylWidget::class.java, BrutalistWidget::class.java, MatrixWidget::class.java, CassetteWidget::class.java, CompactWidget::class.java, ClockWidget::class.java, DateWidget::class.java, BatteryWidget::class.java, AgendaWidget::class.java, SwissWidget::class.java)
    fun component(context: Context, kind: WidgetKind) = ComponentName(context, classes[kind.ordinal])
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
class DriverWidget: LabProvider()
class ConstructorWidget: LabProvider()
class DriverStandingsWidget: LabProvider()
class ConstructorStandingsWidget: LabProvider()
class WeekendWidget: LabProvider()
class CountdownWidget: LabProvider()
class ResultsWidget: LabProvider()
class EditorialWidget: LabProvider()
class GlassWidget: LabProvider()
class VinylWidget: LabProvider()
class BrutalistWidget: LabProvider()
class MatrixWidget: LabProvider()
class CassetteWidget: LabProvider()
class CompactWidget: LabProvider()
class ClockWidget: LabProvider()
class DateWidget: LabProvider()
class BatteryWidget: LabProvider()
class AgendaWidget: LabProvider()
class SwissWidget: LabProvider()

object WidgetUpdates {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex=Mutex()
    private val requests=Channel<Context>(Channel.CONFLATED)
    init { scope.launch { for(context in requests) { delay(180); updateAll(context) } } }
    fun request(context: Context, @Suppress("UNUSED_PARAMETER") musicOnly: Boolean = false) { requests.trySend(context.applicationContext) }
    suspend fun updateAll(context: Context) = updateIds(context,null)
    suspend fun updateIds(context: Context,ids: Set<Int>?) = mutex.withLock {
        val installed=ProviderCatalog.installed(context).filter { ids==null || it.first in ids }
        if(installed.isEmpty()) return@withLock
        val snapshot=RacingRepository(context).snapshot()
        val battery=DeviceData.battery(context)
        val agenda=if(installed.any { it.second==WidgetKind.AGENDA }) DeviceData.agenda(context) else AgendaData(false, emptyList())
        installed.forEach { (id, kind) ->
            try {
                val config=ConfigStore(context).read(id,kind).copy(kind=kind)
                val media=MediaHub.info(config.selection)
                val data=RenderData(snapshot,media,battery,agenda,MediaHub.permitted(context))
                update(context,id,config,data)
            } catch (e: Exception) { android.util.Log.w("WidgetLab","Widget $id update failed: ${e.javaClass.simpleName}") }
        }
    }
    fun update(context: Context,id: Int,cfg: WidgetConfig,data: RenderData) {
        val manager=AppWidgetManager.getInstance(context)
        val options=manager.getAppWidgetOptions(id)
        val dpWidth=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280).coerceAtLeast(180)
        val dpHeight=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,cfg.kind.height*280/420).coerceAtLeast(64)
        val density=context.resources.displayMetrics.density.coerceAtMost(2.5f)
        val width=(dpWidth*density).toInt().coerceIn(280,720)
        val height=(width*dpHeight/dpWidth).coerceAtLeast(90).coerceAtMost(420_000/width)
        val renderer=WidgetRenderer(context)
        val views=RemoteViews(context.packageName,R.layout.widget_surface)
        views.setImageViewBitmap(R.id.surface,renderer.render(cfg,data,width,height,preview=false))
        val edit=Intent(context,WidgetConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(android.net.Uri.parse("widgetlab://configure/$id"))
        val configure=PendingIntent.getActivity(context,id,edit,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        var rootAction=configure
        if(cfg.kind.isMusic && data.mediaAccess) {
            val launch=data.media.packageName.takeIf { it.isNotBlank() }?.let { context.packageManager.getLaunchIntentForPackage(it) }
            rootAction=data.media.open ?: launch?.let { PendingIntent.getActivity(context,id+10000,it,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) } ?: configure
        }
        views.setOnClickPendingIntent(R.id.widget_root,rootAction)
        views.setOnClickPendingIntent(R.id.surface,rootAction)
        val readable=when { cfg.kind.isMusic -> "${cfg.kind.title}. ${data.media.title}. ${data.media.artist}. ${if(data.media.playing) "Playing" else "Paused"}. Tap to open player or configure."
            cfg.kind==WidgetKind.BATTERY -> "Battery ${data.battery.percent?:"unknown"} percent. ${if(data.battery.charging) "Charging" else "On battery"}."
            cfg.kind==WidgetKind.AGENDA -> if(data.agenda.permitted) data.agenda.events.joinToString(". "){ it.title } else "Calendar access required. Tap to configure."
            else -> cfg.kind.title+". Tap to configure."
        }
        views.setContentDescription(R.id.surface,readable)
        val ink=if(cfg.light) Color.rgb(21,23,23) else Color.rgb(245,244,237)
        if(cfg.kind.isMusic) {
            views.setViewVisibility(R.id.transport,View.VISIBLE)
            if(cfg.kind==WidgetKind.COMPACT) views.setViewVisibility(R.id.previous,View.GONE)
            listOf(R.id.previous to "previous",R.id.toggle to "toggle",R.id.next to "next").forEach { (viewId,action) ->
                val supported=data.mediaAccess && when(action) { "previous" -> data.media.previous; "next" -> data.media.next; else -> data.media.toggle }
                val intent=Intent(context,WidgetActionReceiver::class.java).setAction("dev.widgetlab.MEDIA").setData(android.net.Uri.parse("widgetlab://media/$id/$action")).putExtra("widgetId",id).putExtra("control",action)
                views.setOnClickPendingIntent(viewId,if(supported) PendingIntent.getBroadcast(context,id*10+viewId,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else configure)
                views.setTextColor(viewId,if(supported) { if(cfg.kind==WidgetKind.GLASS) Color.WHITE else ink } else if(cfg.light) Color.GRAY else Color.DKGRAY)
                views.setContentDescription(viewId,if(supported) when(action) { "previous"->"Previous track";"next"->"Next track";else->if(data.media.playing)"Pause" else "Play" } else "${action.replaceFirstChar{it.uppercase()}} unavailable. Open configuration.")
            }
            views.setTextViewText(R.id.toggle,if(data.media.playing) "Ⅱ" else "▶")
        }
        if(cfg.kind.isClock) {
            views.setViewVisibility(R.id.clock_group,View.VISIBLE)
            val format=if(cfg.hour24) "HH:mm" else "h:mm"
            listOf(R.id.time,R.id.clock_date).forEach { v -> views.setString(v,"setTimeZone",cfg.zone().id);views.setTextColor(v,ink);views.setOnClickPendingIntent(v,configure) }
            views.setCharSequence(R.id.time,"setFormat12Hour",format);views.setCharSequence(R.id.time,"setFormat24Hour",format)
            val date=if(cfg.hour24) "EEEE, dd MMMM" else "a · EEEE, dd MMMM"
            views.setCharSequence(R.id.clock_date,"setFormat12Hour",date);views.setCharSequence(R.id.clock_date,"setFormat24Hour",date)
            views.setTextViewTextSize(R.id.time,TypedValue.COMPLEX_UNIT_SP,(dpWidth*.22f).coerceIn(38f,88f))
        }
        if(cfg.kind==WidgetKind.DATE) {
            views.setViewVisibility(R.id.date_group,View.VISIBLE)
            listOf(R.id.date_day,R.id.date_month).forEach { v -> views.setString(v,"setTimeZone",cfg.zone().id);views.setTextColor(v,ink);views.setOnClickPendingIntent(v,configure) }
            views.setTextViewTextSize(R.id.date_day,TypedValue.COMPLEX_UNIT_SP,(dpWidth*.26f).coerceIn(42f,100f))
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
