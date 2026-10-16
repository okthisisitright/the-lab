package dev.widgetlab.widgets

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Local troubleshooting only. No analytics, external reporting or media/calendar content. */
object WidgetDiagnostics {
    fun success(context:Context,id:Int) {context.getSharedPreferences("widget-health",0).edit()
        .putLong("success",System.currentTimeMillis()).putInt("id",id).apply()}
    fun failure(context:Context,id:Int,error:Throwable) {context.getSharedPreferences("widget-health",0).edit()
        .putLong("failure",System.currentTimeMillis()).putInt("failed-id",id)
        .putString("error","${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}").apply()}
    fun summary(context:Context):String {
        val p=context.getSharedPreferences("widget-health",0)
        val success=p.getLong("success",0);val failure=p.getLong("failure",0)
        fun time(value:Long)=Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, HH:mm:ss"))
        val updated=if(success>0)"Last widget delivered: ${time(success)}." else "No widget updates delivered yet. Add a widget, then tap Refresh installed widgets if it stays blank."
        return updated+if(failure>0)"\nLast update error: ${time(failure)} · ${p.getString("error","")}" else ""
    }
}
