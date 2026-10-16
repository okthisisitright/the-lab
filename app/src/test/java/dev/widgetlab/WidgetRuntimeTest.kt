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
    @Test fun simplifiedNamesArePlainAndStable() {
        assertEquals(50,WidgetKind.entries.size)
        WidgetKind.entries.forEach{assertFalse(it.title.contains('/'))}
        assertEquals("CLOCK",WidgetKind.CLOCK.name)
        assertEquals("Serif clock",WidgetKind.SERIF.title)
    }
    @Test fun newMusicControlsStayInsideActualWidgetBounds() {
        WidgetKind.entries.filter{it.isMusic}.forEach{kind->
            for((w,h) in listOf(300f to 96f,180f to 250f,360f to 520f,140f to 180f,300f to 54f)){
                val plan=MusicDesign.plan(kind,w,h)
                plan.controls.forEach{control->val b=control.bounds
                    assertTrue("$kind ${control.action} left",b.x>=0)
                    assertTrue("$kind ${control.action} top",b.y>=0)
                    assertTrue("$kind ${control.action} right",b.x+b.w<=w+.1f)
                    assertTrue("$kind ${control.action} bottom",b.y+b.h<=h+.1f)
                }
                if(kind==WidgetKind.ARTWORK&&h>=112&&w>=145)assertTrue(plan.controls.isEmpty())
            }
        }
    }
    @Test fun ambientArtworkIsCachedWithoutMutatingTheOriginal() {
        val original=Bitmap.createBitmap(60,60,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(original);canvas.drawColor(Color.RED)
        canvas.drawRect(30f,0f,60f,60f,Paint().apply{color=Color.BLUE})
        val blurred=MusicArtwork.diffuse(original)!!
        assertSame(blurred,MusicArtwork.diffuse(original))
        assertFalse(original.isRecycled)
        assertEquals(Color.RED,original.getPixel(0,0))
        val center=blurred.getPixel(36,36)
        assertTrue(Color.red(center)>0&&Color.blue(center)>0)
        assertNull(MusicArtwork.diffuse(null))
    }
    @Test fun widgetSurfacesInflateOnlyTheClockComponentsTheyUse() {
        val data=RenderData(RacingSnapshot(Section(SampleRacing.drivers,sample=true),Section(SampleRacing.teams,sample=true),Section(SampleRacing.schedule,sample=true),Section(SampleRacing.result,sample=true)),MediaInfo(),BatteryInfo(73,false),AgendaData(false,emptyList()),false)
        fun clocks(view:android.view.View):List<android.view.View> =
            (if(view is android.widget.TextClock||view is android.widget.AnalogClock)listOf(view) else emptyList())+
                if(view is android.view.ViewGroup)(0 until view.childCount).flatMap{clocks(view.getChildAt(it))} else emptyList()
        for(kind in listOf(WidgetKind.COMPACT,WidgetKind.WEEKEND,WidgetKind.BATTERY,WidgetKind.PHOTO,WidgetKind.GRID,WidgetKind.CLOCK,WidgetKind.ANALOG)){
            val view=WidgetViews.build(context,900+kind.ordinal,WidgetConfig(kind),data,300f,220f).apply(context,android.widget.FrameLayout(context))
            val children=clocks(view)
            when(kind){
                WidgetKind.CLOCK->{assertEquals(ClockDesign.plan(WidgetConfig(kind),300f,220f).size,children.size);assertTrue(children.all{it is android.widget.TextClock})}
                WidgetKind.ANALOG->{assertEquals(1,children.size);assertTrue(children.single() is android.widget.AnalogClock)}
                else->assertTrue("No hidden clock dependencies in $kind",children.isEmpty())
            }
        }
    }
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
        assertNotNull(context.artFile("1234.png"))
        assertEquals("solid",WidgetConfig.parse("""{"kind":"SWISS","light":true}""",WidgetKind.SWISS).surface)
    }
    @Test @Config(sdk=[29]) fun nativeClockLayoutAndRemoteFormattingWorkOnMinimumAndroidVersion() {
        val views=android.widget.RemoteViews(context.packageName,R.layout.tick_sans_500_start)
        views.setString(R.id.clock_text,"setTimeZone","Asia/Kolkata")
        views.setCharSequence(R.id.clock_text,"setFormat12Hour","HH:mm")
        views.setCharSequence(R.id.clock_text,"setFormat24Hour","HH:mm")
        val layout=views.apply(context,android.widget.FrameLayout(context))
        assertEquals("Asia/Kolkata",layout.findViewById<android.widget.TextClock>(R.id.clock_text).timeZone)
        assertEquals("HH:mm",layout.findViewById<android.widget.TextClock>(R.id.clock_text).format24Hour)
    }
    @Test @Config(sdk=[29]) fun editionTwoLayersKeepTheirColorsAndZonesOnAndroidTen() {
        val data=RenderData(RacingSnapshot(Section(SampleRacing.drivers,sample=true),Section(SampleRacing.teams,sample=true),Section(SampleRacing.schedule,sample=true),Section(SampleRacing.result,sample=true)),MediaInfo(),BatteryInfo(73,false),AgendaData(false,emptyList()),false)
        for(kind in listOf(WidgetKind.CLOCK,WidgetKind.DUAL,WidgetKind.DOT_CLOCK,WidgetKind.SERIF,WidgetKind.ANALOG,WidgetKind.COMPACT,WidgetKind.STRIP)){
            val cfg=WidgetConfig(kind,timezone="Asia/Kolkata",accent="#E56152",accentText=true)
            val layout=WidgetViews.build(context,800+kind.ordinal,cfg,data,300f,180f).apply(context,android.widget.FrameLayout(context))
            assertNotNull(layout.findViewById<android.view.View>(R.id.surface))
            if(kind.isClock&&kind!=WidgetKind.ANALOG){
                val clock=layout.findViewById<android.widget.TextClock>(R.id.clock_text)
                assertEquals("Asia/Kolkata",clock.timeZone)
                assertEquals(Color.parseColor("#E56152"),clock.currentTextColor)
            }
        }
    }
    @Test fun licensedCircuitPathsAreAvailableOfflineAndUnknownMapsAreNotInvented(){
        val records=org.json.JSONObject(context.assets.open("circuits/outlines.json").bufferedReader().use{it.readText()})
        assertEquals("MIT / Tomislav Bacinger",records.getString("license"))
        val keys=records.getJSONObject("circuits").keys()
        var count=0
        while(keys.hasNext()){
            val path=CircuitOutlines.path(context,keys.next())
            assertNotNull(path)
            val bounds=android.graphics.RectF();path!!.computeBounds(bounds,true)
            assertTrue(bounds.width()>0);assertTrue(bounds.height()>0);count++
        }
        assertEquals(33,count)
        assertNull(CircuitOutlines.path(context,"not_a_known_circuit"))
    }
    @Test fun allDayAgendaUsesFloatingDatesAcrossBothSidesOfUtc(){
        val now=Instant.parse("2026-10-17T03:00:00Z")
        val event=AgendaEvent(1,"All-day test",Instant.parse("2026-10-16T00:00:00Z"),Instant.parse("2026-10-17T00:00:00Z"),true,0)
        val timed=AgendaEvent(2,"Timed test",Instant.parse("2026-10-17T01:00:00Z"),Instant.parse("2026-10-17T02:00:00Z"),false,0)
        val data=AgendaData(true,listOf(event,timed))
        assertEquals(listOf(event),data.forZone(now,java.time.ZoneId.of("America/Los_Angeles")).events)
        assertTrue(data.forZone(now,java.time.ZoneId.of("Australia/Sydney")).events.isEmpty())
    }
    @Test fun allFiftyDesignsHaveGenuineRegisteredProvidersAndConfiguration() {
        val providers=context.packageManager.queryBroadcastReceivers(android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setPackage(context.packageName),android.content.pm.PackageManager.GET_META_DATA)
        assertEquals(50,providers.size)
        assertEquals(listOf(12,8,15,5,5,5),dev.widgetlab.core.Collection.entries.map{c->WidgetKind.entries.count{it.collection==c}})
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
        val empty=RenderData(RacingSnapshot(Section(SampleRacing.drivers,sample=true),Section(SampleRacing.teams,sample=true),Section(SampleRacing.schedule,sample=true),Section(SampleRacing.result,sample=true)),MediaInfo(),BatteryInfo(73,false),AgendaData(false,emptyList()),false,Instant.parse("2026-10-15T11:28:00Z"))
        val renderer=WidgetRenderer(context)
        val dir=File(System.getProperty("user.dir"),"build/design-review").apply{mkdirs()}
        dev.widgetlab.core.Collection.entries.forEach{collection->
            val kinds=WidgetKind.entries.filter{it.collection==collection}
            val sheet=Bitmap.createBitmap(1260,((kinds.size+2)/3)*370,Bitmap.Config.ARGB_8888);val c=Canvas(sheet);c.drawColor(Color.rgb(44,57,61))
            val paint=Paint().apply{color=Color.WHITE;textSize=14f}
            kinds.forEachIndexed{i,k->
                val cfg=WidgetConfig(k)
                val bitmap=renderer.render(dev.widgetlab.ui.PreviewSamples.applications(context,dev.widgetlab.ui.PreviewSamples.config(cfg)),dev.widgetlab.ui.PreviewSamples.data(context,cfg,empty),width=630)
                assertTrue(bitmap.allocationByteCount<=1_680_000)
                File(dir,"${k.name.lowercase()}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                val picker=Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
                val pickerCanvas=Canvas(picker)
                if(cfg.surface!="solid")pickerCanvas.drawColor(Color.rgb(52,69,74))
                pickerCanvas.drawBitmap(bitmap,0f,0f,null)
                File(dir,"picker-${k.name.lowercase()}.png").outputStream().use{picker.compress(Bitmap.CompressFormat.PNG,100,it)}
                val compact=renderer.render(dev.widgetlab.ui.PreviewSamples.applications(context,dev.widgetlab.ui.PreviewSamples.config(cfg)),dev.widgetlab.ui.PreviewSamples.data(context,cfg,empty),600,220,true,300f)
                File(dir,"compact-${k.name.lowercase()}.png").outputStream().use{compact.compress(Bitmap.CompressFormat.PNG,100,it)}
                val x=(i%3)*420f;val y=(i/3)*370f
                c.drawText("${k.ordinal+1} / ${k.title}",x+14,y+25,paint)
                val scale=minOf(390f/bitmap.width,314f/bitmap.height)
                val drawnWidth=bitmap.width*scale;val drawnHeight=bitmap.height*scale
                val left=x+14+(390-drawnWidth)/2
                c.drawBitmap(bitmap,null,android.graphics.RectF(left,y+44,left+drawnWidth,y+44+drawnHeight),null)
                for((width,height)in listOf(300 to 96,180 to 250,360 to 520)){
                    val other=renderer.render(cfg.copy(light=!cfg.light,typography="mono",rows=10),empty,width,height,false)
                    assertTrue(other.allocationByteCount<=1_680_000)
                }
                if(k in listOf(WidgetKind.CLOCK,WidgetKind.VINYL,WidgetKind.CASSETTE))assertEquals(0,Color.alpha(bitmap.getPixel(1,1)))
                val views=WidgetViews.build(context,500+k.ordinal,cfg,empty,300f,(k.height*300f/420).coerceAtLeast(54f))
                val applied=views.apply(context,android.widget.FrameLayout(context))
                assertNotNull(applied.findViewById<android.view.View>(R.id.surface))
                val dp=context.resources.displayMetrics.density
                applied.measure(android.view.View.MeasureSpec.makeMeasureSpec((300*dp).toInt(),android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec((k.height*300*dp/420).toInt().coerceAtLeast((54*dp).toInt()),android.view.View.MeasureSpec.EXACTLY))
                applied.layout(0,0,applied.measuredWidth,applied.measuredHeight)
                val native=Bitmap.createBitmap(applied.measuredWidth,applied.measuredHeight,Bitmap.Config.ARGB_8888)
                applied.draw(Canvas(native))
                File(dir,"native-${k.name.lowercase()}.png").outputStream().use{native.compress(Bitmap.CompressFormat.PNG,100,it)}
            }
            File(dir,"collection-${collection.name.lowercase()}.png").outputStream().use{sheet.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun clockGeometryStaysInsideBoundsAndDualTimeIsIndependent() {
        for(kind in WidgetKind.entries.filter{it.isClock})for((w,h)in listOf(110f to 54f,110f to 110f,300f to 100f,360f to 440f)){
            ClockDesign.plan(WidgetConfig(kind),w,h).forEach{t->
                assertTrue("${kind.name} x",t.bounds.x>=0);assertTrue("${kind.name} width",t.bounds.x+t.bounds.w<=w+.1f)
                assertTrue("${kind.name} y",t.bounds.y>=0);assertTrue("${kind.name} height",t.bounds.y+t.bounds.h<=h+.1f)
            }
        }
        val config=WidgetConfig(WidgetKind.DUAL,timezone="Asia/Kolkata",secondaryZone="America/New_York")
        val clocks=ClockDesign.plan(config,360f,180f)
        assertEquals("Asia/Kolkata",clocks[0].zone);assertEquals("America/New_York",clocks[1].zone)
        val profile=config.copy(images=listOf("a.jpg","b.jpg"),apps=listOf("dev.example/.Main"),caption="Quiet mornings",surface="outline",showDate=false,accentText=true,iconStyle="mono",showLabels=false)
        assertEquals(profile,WidgetConfig.parse(profile.json(),WidgetKind.CLOCK))
    }
}
