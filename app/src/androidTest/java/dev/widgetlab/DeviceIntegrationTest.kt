package dev.widgetlab

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.TextClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.widgetlab.core.*
import dev.widgetlab.data.*
import dev.widgetlab.media.*
import dev.widgetlab.widgets.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeviceIntegrationTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun shell(command:String)=instrumentation.uiAutomation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8) }
    private fun until(predicate:()->Boolean) {val end=System.currentTimeMillis()+10_000;while(!predicate() && System.currentTimeMillis()<end)Thread.sleep(50);assertTrue(predicate())}
    @Test fun sessionSelectionCallbacksAndTransportControlsUseThePlayingToken() {
        val component="${context.packageName}/${MediaListener::class.java.name}"
        shell("cmd notification allow_listener $component")
        until{MediaHub.permitted(context)}
        lateinit var first:MediaSession;lateinit var second:MediaSession
        val next=CountDownLatch(1);val pause=CountDownLatch(1);var wrongSession=false;var unsupported=false
        instrumentation.runOnMainSync {
            first=MediaSession(context,"paused-probe").apply {
                setCallback(object:MediaSession.Callback(){override fun onSkipToNext(){wrongSession=true}},Handler(Looper.getMainLooper()))
                setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Paused probe").build())
                setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED,0,0f).setActions(PlaybackState.ACTION_PLAY).build());isActive=true
            }
            second=MediaSession(context,"playing-probe").apply {
                setCallback(object:MediaSession.Callback(){override fun onSkipToNext(){next.countDown()};override fun onPause(){pause.countDown()};override fun onSkipToPrevious(){unsupported=true}},Handler(Looper.getMainLooper()))
                setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Playing probe").putString(MediaMetadata.METADATA_KEY_ARTIST,"Widget Lab test").build())
                setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING,0,1f).setActions(PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT).build());isActive=true
            }
            MediaHub.connect(context)
        }
        try {
            until{MediaHub.info().title=="Playing probe"}
            assertTrue(MediaHub.info().next);assertFalse(MediaHub.info().previous);assertTrue(MediaHub.info().toggle)
            instrumentation.runOnMainSync{MediaHub.control(context,"next","");MediaHub.control(context,"previous","");MediaHub.control(context,"toggle","")}
            assertTrue(next.await(5,TimeUnit.SECONDS));assertTrue(pause.await(5,TimeUnit.SECONDS));assertFalse(wrongSession);assertFalse(unsupported)
            instrumentation.runOnMainSync {second.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Metadata changed").build())}
            until{MediaHub.info().title=="Metadata changed"}
            shell("cmd notification disallow_listener $component")
            until{!MediaHub.permitted(context)}
            instrumentation.runOnMainSync{MediaHub.connect(context)}
            assertEquals("Nothing playing",MediaHub.info().title)
        } finally {instrumentation.runOnMainSync{first.release();second.release()};shell("cmd notification disallow_listener $component")}
    }
    @Test fun everyProviderBindsUpdatesAndInflatesWithNativeClockAndControlViews()=runBlocking {
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        val manager=AppWidgetManager.getInstance(context);val host=AppWidgetHost(context,9101)
        val ids=mutableListOf<Int>()
        instrumentation.runOnMainSync{host.startListening()}
        try {
            for(kind in WidgetKind.entries) {
                val id=host.allocateAppWidgetId();ids+=id
                assertTrue("Bind ${kind.title}",manager.bindAppWidgetIdIfAllowed(id,ProviderCatalog.component(context,kind)))
                ConfigStore(context).save(id,WidgetConfig(kind))
                manager.updateAppWidgetOptions(id,Bundle().apply{putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,kind.height*280/420)})
            }
            WidgetUpdates.updateAll(context)
            instrumentation.runOnMainSync {
                for((index,kind)in WidgetKind.entries.withIndex()) {
                    val id=ids[index];val info=manager.getAppWidgetInfo(id)
                    val view=host.createView(context,id,info)
                    assertNotNull("Host view ${kind.title}",view)
                    // Validate that the real RemoteViews layout is accepted by Android's inflater.
                    val layout=RemoteViews(context.packageName,R.layout.widget_surface).apply(context,view)
                    assertNotNull(layout.findViewById<android.view.View>(R.id.surface))
                    assertTrue(layout.findViewById<TextClock>(R.id.time) is TextClock)
                }
            }
        } finally {ids.forEach{host.deleteAppWidgetId(it);ConfigStore(context).delete(it)};instrumentation.runOnMainSync{host.stopListening()};host.deleteHost()}
    }
}
