package dev.widgetlab.releasecheck

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.TextClock
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.Gravity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Deliberately has no dependency on :app. Exercises the installed, optimized APK across processes. */
@RunWith(AndroidJUnit4::class)
class ReleaseWidgetTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun shell(command:String)=instrumentation.uiAutomation.executeShellCommand(command).use {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8)
    }
    private fun surface(view:View):ImageView? {
        if(view is ImageView && runCatching{view.resources.getResourceEntryName(view.id)}.getOrNull()=="surface")return view
        return if(view is ViewGroup)(0 until view.childCount).firstNotNullOfOrNull{surface(view.getChildAt(it))} else null
    }
    private fun visibleClocks(view:View):List<TextClock> = if(view is TextClock && view.isShown)listOf(view)
        else if(view is ViewGroup)(0 until view.childCount).flatMap{visibleClocks(view.getChildAt(it))} else emptyList()
    private fun awaitSurface(view:AppWidgetHostView,label:String) {
        val deadline=System.currentTimeMillis()+15_000
        var rendered=false
        while(!rendered && System.currentTimeMillis()<deadline) {
            instrumentation.runOnMainSync { rendered=surface(view)?.drawable!=null }
            if(!rendered)Thread.sleep(100)
        }
        assertTrue("Installed release delivered real bitmap content for $label",rendered)
    }
    @Test fun installedReleaseReceivesUpdatesAndRendersEveryProvider() {
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        val manager=AppWidgetManager.getInstance(context)
        val providers=manager.installedProviders.filter{it.provider.packageName=="dev.widgetlab"}
        assertEquals(50,providers.size)
        val host=AppWidgetHost(context,9201)
        val ids=mutableListOf<Int>()
        val activity=instrumentation.startActivitySync(Intent(context,HostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as HostActivity
        val density=context.resources.displayMetrics.density
        instrumentation.runOnMainSync{host.startListening()}
        try {
            providers.forEach { info ->
                val id=host.allocateAppWidgetId();ids+=id
                assertTrue(manager.bindAppWidgetIdIfAllowed(id,info.provider))
                manager.updateAppWidgetOptions(id,Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,220)
                })
                lateinit var view:AppWidgetHostView
                instrumentation.runOnMainSync{
                    view=host.createView(activity,id,info)
                    activity.container.removeAllViews()
                    val padding=AppWidgetHostView.getDefaultPaddingForWidget(activity,info.provider,null)
                    activity.container.addView(view,FrameLayout.LayoutParams((300*density).toInt()+padding.left+padding.right,(220*density).toInt()+padding.top+padding.bottom,Gravity.CENTER))
                }
                // bindAppWidgetIdIfAllowed/updateAppWidgetOptions ask the framework to send its
                // own broadcasts. APPWIDGET_UPDATE is protected; a shell forgery is not valid.
                awaitSurface(view,info.provider.className)
                val name=info.provider.className.substringAfterLast('.')
                if(name in setOf("ClockWidget","ThinWidget","SwissWidget","CinematicWidget","GlassClockWidget","BrutalClockWidget","DigitalWidget","DotClockWidget","SplitWidget","VerticalWidget","CornerWidget","DualWidget","SerifWidget","TerminalWidget")){
                    val deadline=System.currentTimeMillis()+5000
                    var visible=false
                    while(!visible&&System.currentTimeMillis()<deadline){
                        instrumentation.runOnMainSync{visible=visibleClocks(view).any{it.text.isNotBlank()&&it.width>0&&it.height>0}}
                        if(!visible)Thread.sleep(100)
                    }
                    assertTrue("Visible native clock text, not just a transparent bitmap: $name",visible)
                    // Assert actual native drawing too; nonempty TextClock.text alone cannot
                    // distinguish wallpaper-colored/blank output from a readable clock.
                    instrumentation.runOnMainSync {
                        val pixels=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                        view.draw(Canvas(pixels))
                        val buffer=IntArray(pixels.width*pixels.height)
                        pixels.getPixels(buffer,0,pixels.width,0,0,pixels.width,pixels.height)
                        assertTrue("Native clock paints varied pixels: $name",buffer.toSet().size>=8)
                        pixels.recycle()
                    }
                }
                val capture=when(name){"ClockWidget"->"clock";"CompactWidget"->"player";"WeekendWidget"->"race";else->null}
                if(capture!=null){
                    val drawn=java.util.concurrent.CountDownLatch(1)
                    instrumentation.runOnMainSync{view.postOnAnimation{view.postOnAnimation{drawn.countDown()}};view.invalidate()}
                    assertTrue("Two committed frames before screenshot",drawn.await(3,java.util.concurrent.TimeUnit.SECONDS))
                    shell("screencap -p /data/local/tmp/widgetlab-release-$capture.png")
                }
                if(info.provider.className.endsWith("ClockWidget")||info.provider.className.endsWith("CompactWidget")||info.provider.className.endsWith("WeekendWidget")){
                    // A genuine options broadcast, with no direct calls into Widget Lab.
                    val initial=context.packageManager.getResourcesForApplication("dev.widgetlab").getIdentifier("widget_surface","layout","dev.widgetlab")
                    instrumentation.runOnMainSync{view.updateAppWidget(android.widget.RemoteViews("dev.widgetlab",initial))}
                    instrumentation.runOnMainSync{
                        val padding=AppWidgetHostView.getDefaultPaddingForWidget(activity,info.provider,null)
                        view.layoutParams=FrameLayout.LayoutParams((330*density).toInt()+padding.left+padding.right,(140*density).toInt()+padding.top+padding.bottom,Gravity.CENTER)
                    }
                    manager.updateAppWidgetOptions(id,Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,330)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,330)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,140)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,140)
                    })
                    // Host callbacks and provider broadcasts are asynchronous. An already queued
                    // pre-resize update can arrive first; wait for the requested geometry itself.
                    val expected=330f/140
                    var ratio=0f
                    val resizeDeadline=System.currentTimeMillis()+15_000
                    while(kotlin.math.abs(ratio-expected)>.03f&&System.currentTimeMillis()<resizeDeadline){
                        instrumentation.runOnMainSync{
                            val bitmap=(surface(view)?.drawable as? BitmapDrawable)?.bitmap
                            ratio=if(bitmap!=null)bitmap.width.toFloat()/bitmap.height else 0f
                        }
                        if(kotlin.math.abs(ratio-expected)>.03f)Thread.sleep(100)
                    }
                    assertEquals("Resize delivers requested geometry for ${info.provider.className}; options=${manager.getAppWidgetOptions(id)}",expected,ratio,.03f)
                }
                if(info.provider.className.endsWith("ClockWidget")){
                    shell("am kill --user 0 dev.widgetlab")
                    instrumentation.runOnMainSync{view.updateAppWidget(android.widget.RemoteViews("dev.widgetlab",context.packageManager.getResourcesForApplication("dev.widgetlab").getIdentifier("widget_surface","layout","dev.widgetlab")))}
                    manager.updateAppWidgetOptions(id,Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,332)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,332)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,142)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,142)
                    })
                    awaitSurface(view,"system update after process restart ${info.provider.className}")
                }
            }
        } finally {ids.forEach{host.deleteAppWidgetId(it)};instrumentation.runOnMainSync{host.stopListening();activity.finish()};host.deleteHost()}
    }
    @Test fun configurationCompletesOnlyAfterFirstRealWidgetUpdate() {
        shell("appwidget grantbind --package ${context.packageName} --user 0")
        val manager=AppWidgetManager.getInstance(context)
        val info=manager.installedProviders.first{it.provider==ComponentName("dev.widgetlab","dev.widgetlab.widgets.ClockWidget")}
        val host=AppWidgetHost(context,9202)
        val id=host.allocateAppWidgetId()
        instrumentation.runOnMainSync{host.startListening()}
        try {
            assertTrue(manager.bindAppWidgetIdIfAllowed(id,info.provider))
            // Binding update is allowed to finish; clear it so Apply must produce a NEW surface.
            Thread.sleep(1500)
            val resources=context.packageManager.getResourcesForApplication("dev.widgetlab")
            val initial=resources.getIdentifier("widget_surface","layout","dev.widgetlab")
            lateinit var view:AppWidgetHostView
            instrumentation.runOnMainSync{view=host.createView(context,id,info)}
            context.startActivity(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val device=UiDevice.getInstance(instrumentation)
            assertTrue("Configure UI",device.wait(Until.hasObject(By.textContains("Apply")),10_000))
            // MainActivity.onResume also requests a redraw. Clear AFTER that redraw, before Apply.
            Thread.sleep(2000)
            instrumentation.runOnMainSync{view.updateAppWidget(android.widget.RemoteViews("dev.widgetlab",initial))}
            Thread.sleep(400)
            device.findObject(By.textContains("Apply")).click()
            awaitSurface(view,"configuration Apply")
        } finally {host.deleteAppWidgetId(id);instrumentation.runOnMainSync{host.stopListening()};host.deleteHost()}
    }
}
