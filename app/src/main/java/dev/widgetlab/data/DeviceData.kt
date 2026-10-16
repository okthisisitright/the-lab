package dev.widgetlab.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.provider.CalendarContract
import java.time.Instant

data class BatteryInfo(val percent: Int?, val charging: Boolean)
data class AgendaEvent(val id: Long, val title: String, val begin: Instant, val end: Instant, val allDay: Boolean, val color: Int)
data class AgendaData(val permitted: Boolean, val events: List<AgendaEvent>, val error: Boolean = false)

object DeviceData {
    fun battery(context: Context): BatteryInfo {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return BatteryInfo(if (level >= 0 && scale > 0) ((100f * level) / scale).toInt().coerceIn(0, 100) else null,
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }
    fun agenda(context: Context, now: Instant = Instant.now()): AgendaData {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return AgendaData(false, emptyList())
        return try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, now.minusSeconds(86400).toEpochMilli())
            android.content.ContentUris.appendId(builder, now.plusSeconds(7 * 86400).toEpochMilli())
            val columns = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR)
            val events = mutableListOf<AgendaEvent>()
            context.contentResolver.query(builder.build(), columns, "(${CalendarContract.Instances.END} > ? OR ${CalendarContract.Instances.ALL_DAY} = 1) AND ${CalendarContract.Instances.VISIBLE} = 1", arrayOf(now.toEpochMilli().toString()), "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext() && events.size < 32) {
                    events += AgendaEvent(cursor.getLong(0), cursor.getString(1)?.ifBlank { "Untitled event" } ?: "Untitled event", Instant.ofEpochMilli(cursor.getLong(2)), Instant.ofEpochMilli(cursor.getLong(3)), cursor.getInt(4) == 1, cursor.getInt(5))
                }
            }
            AgendaData(true, events)
        } catch (_: SecurityException) { AgendaData(false, emptyList()) }
          catch (_: Exception) { AgendaData(true, emptyList(), true) }
    }
}

data class FocusInfo(val filter:Int=0) {
    val title get()=when(filter){1->"Available";2->"Priority only";3->"Do not disturb";4->"Alarms only";else->"Status unavailable"}
    val active get()=filter in 2..4
}
fun readFocus(context:Context):FocusInfo = runCatching {FocusInfo(context.getSystemService(android.app.NotificationManager::class.java).currentInterruptionFilter)}.getOrDefault(FocusInfo())

/** All-day calendar boundaries are floating UTC dates, not instants in the viewer's timezone. */
fun AgendaData.forZone(now:Instant,zone:java.time.ZoneId):AgendaData {
    val today=now.atZone(zone).toLocalDate()
    return copy(events=events.filter{event->
        if(event.allDay)event.end.atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(today)
        else event.end.isAfter(now)
    }.take(5))
}
