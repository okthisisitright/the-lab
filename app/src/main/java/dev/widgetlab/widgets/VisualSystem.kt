package dev.widgetlab.widgets

import android.content.Context
import android.graphics.*
import android.util.LruCache
import dev.widgetlab.R
import dev.widgetlab.core.*
import kotlin.math.*

object LabType {
    fun resource(face: String)=when(face){"condensed"->R.font.condensed;"serif"->R.font.serif;"technical"->R.font.technical;"mono"->R.font.mono;"dots"->R.font.dots;else->R.font.inter}
    private val cache=mutableMapOf<String,Typeface>()
    @Synchronized fun face(context: Context,name: String,weight: Int=400): Typeface = cache.getOrPut("$name:$weight") {Typeface.create(context.resources.getFont(resource(name)),weight,false)}
}
object ArtCache {
    private val images=object:LruCache<String,Bitmap>(12*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
    @Synchronized fun get(context: Context,name:String):Bitmap? {
        if(name.isBlank())return null
        images.get(name)?.let{return it}
        val bitmap=runCatching{if(name in listOf("study/hadley","study/earth"))context.assets.open("studies/${name.substringAfter('/')}.jpg").use{BitmapFactory.decodeStream(it)} else if(name=="archive/hamilton")context.assets.open("portraits/hamilton.jpg").use{BitmapFactory.decodeStream(it)} else context.artFile(name)?.let{BitmapFactory.decodeFile(it.path)}}.getOrNull() ?: return null
        images.put(name,bitmap);return bitmap
    }
}
data class Bounds(val x:Float,val y:Float,val w:Float,val h:Float)
data class ClockText(val bounds:Bounds,val format:String,val zone:String,val face:String,val weight:Int,val size:Float,val alignment:String="start",val role:String="time")
object ClockDesign {
    fun plan(cfg:WidgetConfig,w:Float,h:Float):List<ClockText> {
        val k=cfg.kind;val p=min(22f,w*.065f);val format=if(cfg.hour24)"HH:mm" else "h:mm"
        val date=if(cfg.showWeekday)"EEE, d MMM" else "d MMM yyyy"
        fun item(x:Float,y:Float,width:Float,height:Float,f:String= format,face:String="sans",weight:Int=500,size:Float=82f,align:String="start",zone:String=cfg.zone().id,role:String="time"):ClockText = ClockText(Bounds(x,y,width.coerceAtLeast(1f),height.coerceAtLeast(1f)),f,zone,if(cfg.typography !in listOf("design","editorial"))cfg.typography else face,weight,size,if(cfg.alignment=="design")align else cfg.alignment,role)
        val parts=mutableListOf<ClockText>()
        val includeDate=cfg.showDate&&h>=82f
        val dateH=if(includeDate)22f else 0f
        when(k) {
            WidgetKind.VERTICAL,WidgetKind.BRUTAL_CLOCK -> {
                val top=if(k==WidgetKind.VERTICAL)h*.12f else p
                val usable=h-top-p-dateH
                parts+=item(p,top,w-2*p,usable*.5f,if(cfg.hour24)"HH" else "hh","condensed",700,min(140f,w*.66f),"center")
                parts+=item(p,top+usable*.5f,w-2*p,usable*.5f,"mm","condensed",700,min(140f,w*.66f),"center",role="minutes")
                if(includeDate)parts+=item(p,h-p-20,w-2*p,20f,date,size=12f,align="center",role="date")
            }
            WidgetKind.DUAL -> {
                val gap=24f;val col=(w-2*p-gap)/2
                parts+=item(p,h*.32f,col,h*.38f,face="technical",size=60f)
                parts+=item(p+col+gap,h*.32f,col,h*.38f,face="technical",size=60f,zone=cfg.secondZone().id,role="second")
                if(includeDate)parts+=item(p,h*.76f,w-2*p,22f,date,size=11f,role="date")
            }
            WidgetKind.SPLIT -> {
                if(!includeDate){parts+=item(p,p,w-2*p,h-2*p,weight=650,size=86f)}
                else if(w/h>1.2f){parts+=item(p,h*.2f,w*.57f-p,h*.6f,face="sans",weight=650,size=75f)
                    parts+=item(w*.65f,h*.2f,w*.35f-p,h*.36f,"dd","serif",500,58f,role="day")
                    if(includeDate)parts+=item(w*.65f,h*.58f,w*.35f-p,32f,"MMM EEE",size=11f,role="date")
                }else{parts+=item(p,p,w-2*p,h*.48f,weight=700,size=75f)
                    if(includeDate)parts+=item(p,h*.6f,w-2*p,h*.22f,"EEEE, d MMMM",face="serif",size=23f,role="date")}
            }
            WidgetKind.DATE -> {
                parts+=item(p,p,w*.42f,h*.43f,"dd","serif",500,92f,role="day")
                parts+=item(w*.49f,p+10,w*.51f-p,h*.3f,if(cfg.showWeekday)"MMM\nEEEE" else "MMMM",face="sans",weight=500,size=19f,role="date")
            }
            WidgetKind.TODAY -> {
                parts+=item(p,if(h<180)8f else p,w-2*p,if(h<180)h*.42f else h*.25f,face="sans",weight=650,size=68f)
                if(includeDate)parts+=item(p,if(h<180)h*.57f else h*.36f,w-2*p,22f,date,size=12f,role="date")
            }
            WidgetKind.ANALOG -> Unit
            else -> {
                val face=when(k){WidgetKind.SERIF->"serif";WidgetKind.CINEMATIC->"condensed";WidgetKind.DIGITAL->"technical";WidgetKind.DOT_CLOCK->"dots";WidgetKind.TERMINAL->"mono";else->"sans"}
                val weight=when(k){WidgetKind.THIN->200;WidgetKind.SWISS,WidgetKind.BRUTAL_CLOCK->850;WidgetKind.SERIF->450;else->500}
                val small=k==WidgetKind.CORNER
                val top=when(k){WidgetKind.SWISS->h*.14f;WidgetKind.TERMINAL->if(h<90)p else 28f;else->p}
                val align=if(k in listOf(WidgetKind.THIN,WidgetKind.SERIF,WidgetKind.GLASS_CLOCK))"center" else "start"
                parts+=item(p,top,w-2*p,(h-top-p-dateH).coerceAtLeast(24f),face=face,weight=weight,size=if(small)40f else if(k==WidgetKind.CINEMATIC)110f else 96f,align=align)
                if(includeDate)parts+=item(p,h-p-20,w-2*p,20f,date,size=if(small)10f else 12f,align=align,role="date")
            }
        }
        return parts
    }
    fun fittedSize(context: Context,t:ClockText,sample:String):Float {
        val paint=Paint().apply{typeface=LabType.face(context,t.face,t.weight);textSize=t.size}
        val width=sample.lines().maxOfOrNull{paint.measureText(it)}?.coerceAtLeast(1f) ?: 1f
        val fontHeight=(paint.fontMetrics.descent-paint.fontMetrics.ascent)*sample.lines().size
        return t.size*min(1f,min(t.bounds.w/width,t.bounds.h/fontHeight))
    }
}

data class ShortcutPlan(val bounds:Bounds,val columns:Int,val rows:Int)
fun shortcutPlan(cfg:WidgetConfig,w:Float,h:Float):ShortcutPlan {
    val p=if(h<150)if(cfg.kind==WidgetKind.STRIP)4f else 8f else 16f
    val count=cfg.apps.size.coerceAtLeast(1)
    val columns=when(cfg.kind){WidgetKind.STRIP->count.coerceAtMost(((w-2*p)/44).toInt().coerceIn(1,6));WidgetKind.LAUNCHER->if(w/h>1.7f)4 else 2;else->if(w<200)2 else 3}
    val top=if(cfg.kind==WidgetKind.LAUNCHER)if(h<150)44f else min(h*.37f,85f) else p
    val rows=if(cfg.kind==WidgetKind.STRIP)1 else min(ceil(count.toFloat()/columns).toInt(),((h-top-p)/44).toInt().coerceIn(1,3))
    return ShortcutPlan(Bounds(p,top,w-2*p,h-top-p),columns,rows)
}

fun musicIsCompact(kind:WidgetKind,height:Float)=kind==WidgetKind.COMPACT || height<148f || (kind in listOf(WidgetKind.EDITORIAL,WidgetKind.BRUTALIST)&&height<200f)

fun accentTextColor(cfg:WidgetConfig):Int {
    val color=Color.parseColor(cfg.accent)
    return if(cfg.light)Color.rgb((Color.red(color)*.48f).toInt(),(Color.green(color)*.48f).toInt(),(Color.blue(color)*.48f).toInt()) else color
}
