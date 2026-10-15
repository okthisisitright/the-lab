package dev.widgetlab

import android.app.Application
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import dev.widgetlab.core.*
import dev.widgetlab.data.*
import dev.widgetlab.media.MediaInfo
import dev.widgetlab.widgets.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRuntimeTest {
    private val context get()=ApplicationProvider.getApplicationContext<Application>()
    @Test fun independentlyConfiguredInstancesAndPinProfilesSurviveStoreRecreation() {
        val a=WidgetConfig(WidgetKind.DRIVER,selection="hamilton",accent="#F04C48")
        val b=WidgetConfig(WidgetKind.DRIVER,selection="leclerc",light=true)
        ConfigStore(context).save(301,a);ConfigStore(context).save(302,b)
        assertEquals(a,ConfigStore(context).read(301,WidgetKind.DRIVER));assertEquals(b,ConfigStore(context).read(302,WidgetKind.DRIVER))
        val token=ConfigStore(context).pending(a);assertEquals(a,ConfigStore(context).consume(token));assertNull(ConfigStore(context).consume(token))
        ConfigStore(context).delete(301);assertEquals(b,ConfigStore(context).read(302,WidgetKind.DRIVER))
    }
    @Test fun corruptProfilesAndInvalidTimezoneHaveSafeDefaults() {
        assertEquals(WidgetConfig(WidgetKind.CLOCK),WidgetConfig.parse("broken",WidgetKind.CLOCK))
        assertEquals(5,WidgetConfig.parse("{}",WidgetKind.DRIVER).rows)
        assertEquals(10,WidgetConfig.parse("""{"rows":1000}""",WidgetKind.RESULTS).rows)
        assertNotNull(WidgetConfig(WidgetKind.WEEKEND,timezone="invented/offset").zone())
        assertNull(context.artFile("../../secrets"))
    }
    @Test @Config(sdk=[29]) fun nativeClockLayoutAndRemoteFormattingWorkOnMinimumAndroidVersion() {
        val views=android.widget.RemoteViews(context.packageName,R.layout.widget_surface)
        views.setString(R.id.time,"setTimeZone","Asia/Kolkata")
        views.setCharSequence(R.id.time,"setFormat12Hour","HH:mm")
        views.setCharSequence(R.id.time,"setFormat24Hour","HH:mm")
        val layout=views.apply(context,android.widget.FrameLayout(context))
        assertEquals("Asia/Kolkata",layout.findViewById<android.widget.TextClock>(R.id.time).timeZone)
        assertEquals("HH:mm",layout.findViewById<android.widget.TextClock>(R.id.time).format24Hour)
    }
    @Test fun allNineteenDesignsHaveGenuineRegisteredProvidersAndConfiguration() {
        val providers=context.packageManager.queryBroadcastReceivers(android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setPackage(context.packageName),android.content.pm.PackageManager.GET_META_DATA)
        assertEquals(19,providers.size)
        WidgetKind.entries.forEach{k->
            val info=providers.first{it.activityInfo.name==ProviderCatalog.component(context,k).className}.activityInfo
            info.loadXmlMetaData(context.packageManager,"android.appwidget.provider").use { xml ->
                while(xml.eventType!=org.xmlpull.v1.XmlPullParser.START_TAG)xml.next()
                val ns="http://schemas.android.com/apk/res/android"
                assertNotNull(xml.getAttributeValue(ns,"configure"));assertEquals(1800000,xml.getAttributeIntValue(ns,"updatePeriodMillis",0))
                assertNotNull(xml.getAttributeValue(ns,"minWidth"));assertNotNull(xml.getAttributeValue(ns,"minHeight"))
            }
        }
    }
    @Test fun failedRefreshPreservesCacheAndReportsItsAge()=runBlocking {
        val directory=File(context.filesDir,"racing-v1").apply{mkdirs()}
        val payload=javaClass.classLoader!!.getResource("drivers.json")!!.readText()
        val stamp="2026-10-14T13:00:00Z"
        File(directory,"drivers.json").writeText(org.json.JSONObject().put("payload",payload).put("refreshed",stamp).toString())
        context.getSharedPreferences("sync",0).edit().clear().commit()
        val source=object:RacingProvider{override suspend fun fetch(path:String):String=throw java.io.IOException("offline test")}
        val repo=RacingRepository(context,source);assertEquals(4,repo.refresh().size)
        val data=repo.snapshot().drivers
        assertFalse(data.sample);assertEquals(Instant.parse(stamp),data.refreshed);assertNotNull(data.error)
        assertTrue(data.value.entries.isNotEmpty());assertTrue(repo.snapshot().schedule.sample)
    }
    @Test fun renderEveryDesignAndBoundBitmapMemoryWithEmptyRealWorldStates() {
        val data=RenderData(RacingSnapshot(Section(SampleRacing.drivers,sample=true),Section(SampleRacing.teams,sample=true),Section(SampleRacing.schedule,sample=true),Section(SampleRacing.result,sample=true)),MediaInfo(),BatteryInfo(73,false),AgendaData(false,emptyList()),false,Instant.parse("2026-10-15T11:00:00Z"))
        val renderer=WidgetRenderer(context)
        val dir=File(System.getProperty("user.dir"),"build/design-review").apply{mkdirs()}
        val sheet=Bitmap.createBitmap(1260,2020,Bitmap.Config.ARGB_8888);val c=Canvas(sheet);c.drawColor(Color.rgb(39,42,43))
        val p=Paint().apply{color=Color.WHITE;textSize=14f}
        WidgetKind.entries.forEachIndexed{i,k->
            val cfg=WidgetConfig(k,selection=if(k==WidgetKind.DRIVER)"hamilton" else "")
            val bitmap=renderer.render(cfg,data,width=630)
            assertTrue(bitmap.allocationByteCount<=1_680_000);assertTrue(Color.alpha(bitmap.getPixel(100,100))>0)
            File(dir,"${k.name.lowercase()}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            val x=(i%3)*420f;val y=(i/3)*285f
            c.drawText(k.title,x+12,y+20,p)
            val scaledHeight=minOf(245f,k.height*.78f)
            c.drawBitmap(bitmap,null,android.graphics.RectF(x+10,y+30,x+410,y+30+scaledHeight),null)
            val other=renderer.render(cfg.copy(light=!cfg.light,typography="mono",rows=10),data,720,900,false)
            assertTrue(other.allocationByteCount<=1_680_000)
        }
        File(dir,"contact-sheet.png").outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
}
