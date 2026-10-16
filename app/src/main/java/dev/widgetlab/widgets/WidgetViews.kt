package dev.widgetlab.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import dev.widgetlab.R
import dev.widgetlab.core.*
import dev.widgetlab.data.LaunchApps
import dev.widgetlab.data.forZone
import dev.widgetlab.ui.WidgetConfigureActivity
import kotlin.math.*

/** Native interactive layer and self-updating clock layer share the Canvas layout geometry. */
object WidgetViews {
    fun build(context:Context,id:Int,cfg:WidgetConfig,data:RenderData,w:Float,h:Float,density:Float=2f,pixelBudget:Int=420_000):RemoteViews {
        val type=when(cfg.kind){
            WidgetKind.THIN->"thin";WidgetKind.SWISS->"bold";WidgetKind.CINEMATIC,WidgetKind.VERTICAL,WidgetKind.BRUTAL_CLOCK->"condensed"
            WidgetKind.SERIF,WidgetKind.DATE->"serif";WidgetKind.DIGITAL,WidgetKind.DUAL->"technical";WidgetKind.DOT_CLOCK->"dots";WidgetKind.TERMINAL->"mono";else->"sans"
        }.let{if(cfg.typography !in listOf("design","editorial"))cfg.typography else it}
        val layout=context.resources.getIdentifier("surface_${type}_${if(cfg.light)"light" else "dark"}","layout",context.packageName)
        val views=RemoteViews(context.packageName,layout.takeIf{it!=0} ?: R.layout.widget_surface)
        val rasterScale=min(density,sqrt(pixelBudget/(w*h)))
        views.setImageViewBitmap(R.id.surface,WidgetRenderer(context).render(cfg,data,(w*rasterScale).toInt(),(h*rasterScale).toInt(),false,w))
        val edit=Intent(context,WidgetConfigureActivity::class.java).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(Uri.parse("widgetlab://configure/$id"))
        val configure=PendingIntent.getActivity(context,id,edit,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val root=when{
            cfg.kind.isMusic && data.mediaAccess->data.media.open ?: data.media.packageName.takeIf{it.isNotEmpty()}?.let{context.packageManager.getLaunchIntentForPackage(it)}?.let{PendingIntent.getActivity(context,id+10000,it,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)} ?: configure
            cfg.kind==WidgetKind.FOCUS->runCatching{PendingIntent.getActivity(context,id,Intent("android.settings.ZEN_MODE_SETTINGS").let{if(it.resolveActivity(context.packageManager)!=null)it else Intent(Settings.ACTION_SOUND_SETTINGS)},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)}.getOrDefault(configure)
            else->configure
        }
        views.setOnClickPendingIntent(R.id.surface,root);views.setOnClickPendingIntent(R.id.widget_root,root)
        val description=when{
            cfg.kind.isMusic->"${cfg.kind.title}. ${data.media.title}. ${data.media.artist}. ${if(data.media.playing)"Playing" else "Paused"}"
            cfg.kind.hasAgenda->if(data.agenda.permitted)data.agenda.forZone(data.now,cfg.zone()).events.joinToString(". "){it.title} else "Calendar access required. Tap to configure."
            cfg.kind==WidgetKind.BATTERY||cfg.kind==WidgetKind.BATTERY_EDITORIAL->"Battery ${data.battery.percent ?: "unknown"} percent. ${if(data.battery.charging)"Charging" else "On battery"}"
            cfg.kind==WidgetKind.FOCUS->data.focus.title
            cfg.kind.hasLabel->cfg.label
            else->cfg.kind.title
        }
        views.setContentDescription(R.id.surface,description)
        val ink=if(cfg.light)Color.rgb(24,27,28) else Color.rgb(245,244,238)
        val muted=if(cfg.light)0xFF686A67.toInt() else 0xFFA4ABAD.toInt()
        if(cfg.kind.isClock||cfg.kind.hasAgenda){
            if(cfg.kind==WidgetKind.ANALOG){
                views.setViewVisibility(R.id.analog,View.VISIBLE)
                val cap=android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888)
                android.graphics.Canvas(cap).drawCircle(32f,32f,32f,android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=Color.parseColor(cfg.accent)})
                views.setViewVisibility(R.id.analog_cap,View.VISIBLE)
                views.setImageViewBitmap(R.id.analog_cap,cap)
                if(Build.VERSION.SDK_INT>=31)views.setString(R.id.analog,"setTimeZone",if(cfg.timezone=="device")null else cfg.zone().id)
                views.setOnClickPendingIntent(R.id.analog,configure)
                views.setContentDescription(R.id.analog,"Analog clock, ${if(Build.VERSION.SDK_INT>=31)cfg.zone().id else "device timezone"}")
            }else{
                val slots=listOf(R.id.clock_slot0,R.id.clock_slot1,R.id.clock_slot2,R.id.clock_slot3)
                ClockDesign.plan(cfg,w,h).take(4).forEachIndexed{i,t->
                    val b=t.bounds
                    fun px(v:Float)=(v*context.resources.displayMetrics.density).roundToInt().coerceAtLeast(0)
                    views.setViewVisibility(slots[i],View.VISIBLE)
                    views.setViewPadding(slots[i],px(b.x),px(b.y),px(w-b.x-b.w),px(h-b.y-b.h))
                    views.removeAllViews(slots[i])
                    val alignment=t.alignment.takeIf{it in listOf("start","center","end")} ?: "start"
                    val childId=context.resources.getIdentifier("tick_${t.face}_${t.weight}_$alignment","layout",context.packageName)
                    val child=RemoteViews(context.packageName,childId.takeIf{it!=0} ?: R.layout.tick_sans_500_start)
                    val followsDevice=if(t.role=="second")cfg.secondaryZone=="device" else cfg.timezone=="device"
                    child.setString(R.id.clock_text,"setTimeZone",if(followsDevice)null else t.zone)
                    child.setCharSequence(R.id.clock_text,"setFormat12Hour",t.format);child.setCharSequence(R.id.clock_text,"setFormat24Hour",t.format)
                    val sample=if(t.role in listOf("time","second","minutes"))if(t.format.contains(':'))"23:58" else "88" else data.now.atZone(java.time.ZoneId.of(t.zone)).format(java.time.format.DateTimeFormatter.ofPattern(t.format))
                    val size=ClockDesign.fittedSize(context,t,sample)
                    child.setTextViewTextSize(R.id.clock_text,TypedValue.COMPLEX_UNIT_DIP,size)
                    child.setTextColor(R.id.clock_text,if(cfg.accentText&&t.role in listOf("time","second","minutes","day"))accentTextColor(cfg) else if(t.role=="minutes"&&cfg.kind==WidgetKind.BRUTAL_CLOCK)Color.parseColor(cfg.accent) else if(t.role=="date")muted else ink)
                    child.setOnClickPendingIntent(R.id.clock_text,configure)
                    views.addView(slots[i],child)
                }
            }
        }
        if(cfg.kind.isMusic){
            views.setViewVisibility(R.id.transport,View.VISIBLE)
            if(musicIsCompact(cfg.kind,h))views.setViewVisibility(R.id.previous,View.GONE)
            listOf(R.id.previous to "previous",R.id.toggle to "toggle",R.id.next to "next").forEach{(view,action)->
                val supported=data.mediaAccess&&when(action){"previous"->data.media.previous;"next"->data.media.next;else->data.media.toggle}
                val intent=Intent(context,WidgetActionReceiver::class.java).setAction("dev.widgetlab.MEDIA").setData(Uri.parse("widgetlab://media/$id/$action")).putExtra("widgetId",id).putExtra("control",action)
                views.setOnClickPendingIntent(view,if(supported)PendingIntent.getBroadcast(context,id*10+view,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else configure)
                val color=if(supported)if(cfg.kind==WidgetKind.ARTWORK)Color.WHITE else ink else muted
                val resource=when(action){"previous"->R.drawable.control_previous;"next"->R.drawable.control_next;else->if(data.media.playing)R.drawable.control_pause else R.drawable.control_play}
                val icon=context.getDrawable(resource)!!.mutate().apply{setTint(color);setBounds(0,0,64,64)}
                val bitmap=android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888)
                icon.draw(android.graphics.Canvas(bitmap))
                views.setImageViewBitmap(view,bitmap)
                views.setContentDescription(view,if(supported)when(action){"previous"->"Previous track";"next"->"Next track";else->if(data.media.playing)"Pause" else "Play"} else "$action unavailable. Open settings")
            }
        }
        if(cfg.kind.hasApps&&cfg.apps.isNotEmpty()){
            val plan=shortcutPlan(cfg,w,h);val b=plan.bounds
            fun px(v:Float)=(v*context.resources.displayMetrics.density).roundToInt().coerceAtLeast(0)
            views.setViewVisibility(R.id.shortcut_host,View.VISIBLE)
            views.setViewPadding(R.id.shortcut_host,px(b.x),px(b.y),px(w-b.x-b.w),px(h-b.y-b.h))
            views.removeAllViews(R.id.shortcut_host)
            repeat(plan.rows){rowIndex->
                val row=RemoteViews(context.packageName,R.layout.shortcut_row)
                repeat(plan.columns){column->
                    val index=rowIndex*plan.columns+column
                    val cell=RemoteViews(context.packageName,R.layout.shortcut_cell)
                    val selected=cfg.apps.getOrNull(index)
                    val launch=Intent(context,dev.widgetlab.ui.ShortcutLaunchActivity::class.java)
                        .setData(Uri.parse("widgetlab://launch/$id/$index"))
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("index",index)
                    val pending=if(selected!=null)PendingIntent.getActivity(context,id*100+index,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else configure
                    cell.setOnClickPendingIntent(R.id.shortcut_click,pending)
                    cell.setContentDescription(R.id.shortcut_click,selected?.let{LaunchApps.label(context,it)} ?: "Choose application")
                    row.addView(R.id.shortcut_row,cell)
                }
                views.addView(R.id.shortcut_host,row)
            }
        }
        return views
    }
}
