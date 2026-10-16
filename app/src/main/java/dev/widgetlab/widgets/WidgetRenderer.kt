package dev.widgetlab.widgets

import android.content.Context
import android.graphics.*
import dev.widgetlab.core.*
import dev.widgetlab.core.Collection
import dev.widgetlab.data.*
import dev.widgetlab.media.MediaInfo
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

data class RenderData(val racing:RacingSnapshot,val media:MediaInfo,val battery:BatteryInfo,val agenda:AgendaData,val mediaAccess:Boolean,val now:Instant=Instant.now(),val focus:FocusInfo=FocusInfo())

/** Real-size compositions. Only uniform raster scaling is permitted; layout responds to w/h. */
class WidgetRenderer(private val context:Context) {
    fun render(config:WidgetConfig,data:RenderData,width:Int=630,height:Int=config.kind.height*width/420,preview:Boolean=true,logicalWidth:Float=360f):Bitmap {
        val targetW=width.coerceIn(96,1080);val targetH=height.coerceAtLeast(48)
        val scale=min(1.0,sqrt(420_000.0/(targetW.toDouble()*targetH)))
        val w=(targetW*scale).toInt().coerceAtLeast(1);val h=(targetH*scale).toInt().coerceAtLeast(1)
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.scale(w/logicalWidth,w/logicalWidth)
        Draw(canvas,config,data,logicalWidth,h*logicalWidth/w,preview).draw()
        return bitmap
    }
    private inner class Draw(val c:Canvas,val cfg:WidgetConfig,val d:RenderData,val w:Float,val h:Float,val preview:Boolean) {
        val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val ink=if(cfg.light)Color.rgb(24,27,28) else Color.rgb(245,244,238)
        val paper=if(cfg.light)Color.rgb(246,243,234) else Color.rgb(17,20,24)
        val muted=if(cfg.light)Color.rgb(104,106,103) else Color.rgb(164,171,173)
        val accent=Color.parseColor(cfg.accent)
        val pad=min(22f,w*.065f)
        val inner=w-2*pad
        val compact=h<148f
        val tall=h>w*1.15f
        val ruleColor=if(cfg.light)0x22232323 else 0x33FFFFFF
        fun rect(x:Float,y:Float,width:Float,height:Float,color:Int,r:Float=0f){if(width<=0||height<=0)return;p.shader=null;p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(RectF(x,y,x+width,y+height),r,r,p)}
        fun line(x:Float,y:Float,x2:Float,y2:Float,color:Int=ruleColor,thick:Float=1f){p.shader=null;p.color=color;p.strokeWidth=thick;p.style=Paint.Style.STROKE;c.drawLine(x,y,x2,y2,p);p.style=Paint.Style.FILL}
        fun circle(x:Float,y:Float,r:Float,color:Int,stroke:Float=0f){p.shader=null;p.color=color;p.style=if(stroke>0)Paint.Style.STROKE else Paint.Style.FILL;p.strokeWidth=stroke;c.drawCircle(x,y,r.coerceAtLeast(0f),p);p.style=Paint.Style.FILL}
        fun text(value:String,x:Float,y:Float,size:Float,color:Int=ink,face:String="sans",weight:Int=400,max:Float=w-x-pad) {
            if(max<4||size<5)return
            p.shader=null;p.color=color;p.typeface=LabType.face(context,if(cfg.typography !in listOf("design","editorial"))cfg.typography else face,weight);p.textSize=size
            val clean=value.replace('\n',' ')
            var t=clean
            if(p.measureText(t)>max){val n=p.breakText(t,true,(max-p.measureText("…")).coerceAtLeast(0f),null);t=t.take(n).trimEnd()+"…"}
            c.drawText(t,x,y,p)
        }
        fun fit(value:String,x:Float,y:Float,size:Float,max:Float,color:Int=ink,face:String="sans",weight:Int=700) {
            p.typeface=LabType.face(context,if(cfg.typography !in listOf("design","editorial"))cfg.typography else face,weight);p.textSize=size
            text(value,x,y,size*min(1f,max/p.measureText(value).coerceAtLeast(1f)),color,face,weight,max)
        }
        fun label(value:String,x:Float=pad,y:Float=pad+5,color:Int=muted,max:Float=w-x-pad)=text(value.uppercase(Locale.ROOT),x,y,8.5f,color,"mono",400,max)
        fun title(value:String,x:Float,y:Float,size:Float,max:Float,color:Int=ink,face:String="condensed") {
            p.typeface=LabType.face(context,face,650);p.textSize=size
            val clean=value.replace('\n',' ');val count=p.breakText(clean,true,max,null)
            if(count>=clean.length){text(clean,x,y,size,color,face,650,max);return}
            val split=clean.take(count).lastIndexOf(' ').takeIf{it>0} ?: count.coerceAtLeast(1)
            text(clean.take(split),x,y,size,color,face,650,max);text(clean.substring(split).trimStart(),x,y+size*.98f,size,color,face,650,max)
        }
        fun photograph(bitmap:Bitmap?,b:Bounds,round:Float=0f,empty:Boolean=true) {
            val(x,y,width,height)=b
            if(width<=0||height<=0)return
            c.save();c.clipPath(Path().apply{addRoundRect(RectF(x,y,x+width,y+height),round,round,Path.Direction.CW)})
            if(bitmap==null){
                if(empty){rect(x,y,width,height,if(cfg.light)0xFFE1DFD6.toInt() else 0xFF272D32.toInt());
                    line(x+width*.35f,y+height*.5f,x+width*.65f,y+height*.5f,muted);line(x+width*.5f,y+height*.35f,x+width*.5f,y+height*.65f,muted)}
            }else{
                val scale=max(width/bitmap.width,height/bitmap.height);val bw=bitmap.width*scale;val bh=bitmap.height*scale
                val left=when(cfg.crop){"left"->x;"right"->x+width-bw;else->x+(width-bw)/2}
                val top=when(cfg.crop){"top"->y;"bottom"->y+height-bh;else->y+(height-bh)/2}
                p.color=Color.WHITE;p.shader=null;c.drawBitmap(bitmap,null,RectF(left,top,left+bw,top+bh),p)
            };c.restore()
        }
        fun circlePhoto(bitmap:Bitmap?,x:Float,y:Float,r:Float){c.save();c.clipPath(Path().apply{addCircle(x,y,r,Path.Direction.CW)});photograph(bitmap,Bounds(x-r,y-r,r*2,r*2));c.restore()}
        fun art()=ArtCache.get(context,cfg.image)
        fun fresh(section:Section<*>,season:String=""):String {
            if(section.sample)return "SAMPLE · $season ARCHIVE"
            val time=section.refreshed?.atZone(cfg.zone())?.format(DateTimeFormatter.ofPattern("d MMM HH:mm",Locale.getDefault())) ?: "—"
            return "${if(section.error!=null)"OFFLINE" else if(section.stale(d.now))"CACHED" else "UPDATED"} $time${if(season.isBlank())"" else " · $season"}"
        }
        fun footer(s:Section<*>,season:String="")=label(fresh(s,season),pad,h-9f,max=inner)
        fun draw() {
            c.save()
            val corner=when{cfg.surface=="transparent"->0f;cfg.kind in listOf(WidgetKind.BRUTALIST,WidgetKind.BRUTAL_CLOCK,WidgetKind.POLAROID)->0f;else->cfg.corners.toFloat()}
            c.clipPath(Path().apply{addRoundRect(RectF(0f,0f,w,h),corner,corner,Path.Direction.CW)})
            when(cfg.surface){
                "solid"->c.drawColor(paper)
                "semi"->c.drawColor((paper and 0x00FFFFFF) or 0xB0000000.toInt())
                "tint"->c.drawColor((accent and 0x00FFFFFF) or 0x45000000)
                "glass"->{c.drawColor(if(cfg.light)0xA9F5F4EF.toInt() else 0x99363D41.toInt());rect(1f,1f,w-2,1f,0x70FFFFFF);line(1f,1f,1f,h-1,0x28FFFFFF)}
                "outline"->{p.color=muted;p.style=Paint.Style.STROKE;p.strokeWidth=1f;c.drawRoundRect(RectF(.7f,.7f,w-.7f,h-.7f),corner,corner,p);p.style=Paint.Style.FILL}
            }
            when {
                cfg.kind.isRacing->racing()
                cfg.kind.isMusic->music()
                cfg.kind.isClock->clock()
                cfg.kind.isPhoto->photos()
                cfg.kind.collection==Collection.LAUNCHERS->launchers()
                else->essentials()
            }
            c.restore()
        }
        fun racing(){when(cfg.kind){
            WidgetKind.DRIVER,WidgetKind.MINIMAL_DRIVER->driver()
            WidgetKind.CONSTRUCTOR->constructor()
            WidgetKind.DRIVER_PODIUM,WidgetKind.TEAM_PODIUM->podium()
            WidgetKind.DRIVER_STANDINGS,WidgetKind.CONSTRUCTOR_STANDINGS->standings()
            WidgetKind.WEEKEND,WidgetKind.WEEKEND_EDITORIAL->weekend()
            WidgetKind.COUNTDOWN->countdown()
            WidgetKind.RESULTS->results()
            else->circuitCard()
        }}
        fun driver(){
            val s=d.racing.drivers;val table=s.value
            val driver=table.entries.find{it.id==cfg.selection} ?: if(cfg.selection.isBlank())table.entries.firstOrNull() else null
            if(driver==null){label("DRIVER / ${table.season}");text("Selection unavailable",pad,h*.5f,22f);footer(s,table.season);return}
            val team=driver.teams.firstOrNull().orEmpty();val color=Color.parseColor(teamColor(team))
            val portrait=art() ?: if(driver.id=="hamilton")ArtCache.get(context,"archive/hamilton") else null
            val minimal=cfg.kind==WidgetKind.MINIMAL_DRIVER
            if(h<180){
                val textRight=if(minimal)w-pad else w*.68f
                if(!minimal){rect(w*.71f,0f,w*.29f,h-18,color);photograph(portrait,Bounds(w*.71f,0f,w*.29f,h-18),empty=false)}
                label("${table.season} / DRIVER",pad,17f,max=textRight-pad)
                fit(driver.number,pad,h*.67f,min(54f,h*.48f),w*.22f,if(minimal)ink else color,"condensed")
                val x=w*.3f
                text(driver.family.uppercase(),x,h*.46f,min(19f,w*.065f),ink,"condensed",700,textRight-x)
                label("P${driver.position} / ${driver.points} PTS",x,h*.68f,max=textRight-x)
                if(!minimal&&cfg.image.isBlank()&&driver.id=="hamilton"){
                    rect(w*.71f,h-34,w*.29f,16f,0x90000000.toInt())
                    label("2021",w*.71f+6,h-22,Color.WHITE,w*.29f-12)
                }
                footer(s,table.season);return
            }
            if(minimal){
                label("${table.season} / DRIVER FILE",color=muted)
                fit(driver.number,pad,h*.52f,min(110f,h*.44f),w*.6f,ink,"condensed",700)
                circlePhoto(portrait,w*.79f,h*.29f,min(w*.13f,h*.18f))
                text(driver.name,pad,h*.7f,min(25f,w*.075f),ink,"serif",500,inner)
                line(pad,h*.77f,w-pad,h*.77f)
                label("P${driver.position}   /   ${driver.points} PTS   /   ${driver.nationality}",pad,h*.86f,max=inner)
            }else if(tall){
                rect(0f,0f,w,h*.55f,color);photograph(portrait,Bounds(0f,0f,w,h*.55f),empty=false)
                text(driver.number,pad,h*.2f,min(80f,w*.3f),Color.WHITE,"condensed",700,w*.3f)
                label("DRIVER / ${table.season}",pad,h*.62f)
                text(driver.given,pad,h*.7f,20f);fit(driver.family.uppercase(),pad,h*.79f,46f,inner)
                label("P${driver.position}     ${driver.points} PTS     ${driver.nationality}",pad,h*.9f)
            }else{
                rect(w*.48f,0f,w*.52f,h-31f,color);photograph(portrait,Bounds(w*.48f,0f,w*.52f,h-31f),empty=false)
                if(portrait==null)fit(driver.number,w*.54f,h*.57f,110f,w*.4f,0x66000000,"condensed")
                rect(w*.48f,0f,w*.52f,25f,0x65000000)
                label(driver.nationality,w*.52f,17f,Color.WHITE,w*.48f-pad)
                label("DRIVER / ${table.season}",max=w*.43f-pad)
                fit(driver.number,pad,h*.43f,min(76f,h*.31f),w*.4f,color,"condensed")
                text(driver.given,pad,h*.58f,min(18f,h*.08f),max=w*.43f-pad)
                fit(driver.family.uppercase(),pad,h*.72f,29f,w*.43f-pad,ink,"condensed")
                label("P${driver.position} / ${driver.points} PTS",pad,h-42,max=w*.44f-pad)
            }
            if(cfg.image.isBlank()&&driver.id=="hamilton"){
                if(minimal)label("2021 ARCHIVE",w*.65f,h*.59f,muted,w*.35f-pad)
                else{
                    val x=if(tall)0f else w*.48f
                    val bottom=if(tall)h*.55f else h-31f
                    rect(x,bottom-18,w-x,18f,0x90000000.toInt())
                    label("2021 ARCHIVE",x+10,bottom-6,Color.WHITE,w-x-20)
                }
            }
            if(!minimal) {val name=d.racing.teams.value.entries.find{it.id==team}?.name ?: team;label(name,pad,h-24,muted,inner)}
            footer(s,table.season)
        }
        fun constructor(){
            val s=d.racing.teams;val team=s.value.entries.find{it.id==cfg.selection} ?: if(cfg.selection.isBlank())s.value.entries.firstOrNull() else null
            if(team==null){text("Team unavailable",pad,h*.5f,24f);footer(s);return}
            val color=Color.parseColor(teamColor(team.id))
            val art=art()
            if(h<190){
                rect(0f,0f,4f,h,color)
                fit(team.name.uppercase(),pad,35f,27f,inner,ink,"condensed")
                text("P${team.position}",pad,h-28,32f,ink,"condensed",700,inner*.35f)
                text("${team.points} PTS",w*.43f,h-28,22f,color,"technical",600,inner*.55f)
                footer(s,s.value.season);return
            }
            val artHeight=if(tall)h*.44f else h*.36f
            if(art!=null)photograph(art,Bounds(0f,0f,w,artHeight)) else {
                rect(0f,0f,w,artHeight,color)
                text(team.name.uppercase(),pad,artHeight*.7f,min(62f,w*.18f),0xCC101418.toInt(),"condensed",700,inner)
                label("PERSONAL CAR ART / OPTIONAL",pad,artHeight-12f,0xAA101418.toInt())
            }
            val top=artHeight+20f
            label("WORKS / ${s.value.season}",pad,top)
            if(art!=null)fit(team.name.uppercase(),pad,top+27,25f,inner)
            val statsY=if(tall)h*.8f else h-43f
            text("P${team.position}",pad,statsY,min(43f,h*.17f),ink,"condensed",700,w*.31f)
            text(team.points,w*.43f,statsY,min(43f,h*.17f),color,"technical",600,w*.4f)
            label("PTS",w*.81f,statsY)
            val drivers=if(d.racing.drivers.value.season==s.value.season && d.racing.drivers.sample==s.sample)d.racing.drivers.value.entries.filter{team.id in it.teams}.joinToString(" / "){it.code} else "PAIRINGS UNAVAILABLE"
            label(drivers,pad,h-25f);footer(s,s.value.season)
        }
        fun podium(){
            val teams=cfg.kind==WidgetKind.TEAM_PODIUM
            val s=if(teams)d.racing.teams else d.racing.drivers
            val names=if(teams)d.racing.teams.value.entries.take(3).map{Triple(it.name,it.points,it.id)} else d.racing.drivers.value.entries.take(3).map{Triple(it.code,it.points,it.teams.firstOrNull().orEmpty())}
            val season=if(teams)d.racing.teams.value.season else d.racing.drivers.value.season
            label("${if(teams)"CONSTRUCTORS" else "DRIVERS"} / $season")
            if(names.isEmpty()){text("Not published",pad,h*.55f,23f);footer(s);return}
            if(h<200&&!teams){
                val col=inner/3
                names.forEachIndexed{i,e->val x=pad+i*col
                    label("0${i+1}",x,40f,max=col-8)
                    text(e.first,x,h-43,19f,ink,"condensed",700,col-8)
                    text(e.second,x,h-26,12f,muted,"technical",500,col-8)
                    rect(x,h-20,col-8,2f,Color.parseColor(teamColor(e.third)))
                };footer(s,season);return
            }
            val top=if(compact)25f else 63f
            if(teams){
                val step=(h-top-28)/3
                names.forEachIndexed{i,e->
                    val maxPoints=names.first().second.toFloatOrNull()?.coerceAtLeast(1f) ?: 1f
                    val fraction=(e.second.toFloatOrNull() ?: 0f)/maxPoints
                    val y=top+i*step
                    text("0${i+1}",pad,y+16,14f,muted,"mono",max=25f)
                    text(e.first,pad+34,y+16,min(22f,step*.60f),ink,"condensed",600,inner*.56f)
                    fit(e.second,w-83,y+16,min(22f,step*.70f),62f,ink,"technical")
                    rect(pad+34,y+step-2,(inner-34)*fraction,2f,Color.parseColor(teamColor(e.third)))
                }
            }else{
                val col=inner/3; val baseline=h-38f
                listOf(1,0,2).forEachIndexed{column,index->names.getOrNull(index)?.let{e->
                    val block=(baseline-top)*(if(index==0).96f else if(index==1).80f else .67f)
                    val x=pad+column*col;val y=baseline-block;val color=Color.parseColor(teamColor(e.third))
                    rect(x+2,y,col-4,block,color)
                    val numberSize=min(44f,block*.35f)
                    fit("0${index+1}",x+12,y+numberSize+6,numberSize,col-24,0xFF11151A.toInt(),"condensed")
                    text(e.first,x+12,baseline-27,min(20f,col*.29f),0xFF11151A.toInt(),"condensed",700,col-20)
                    text(e.second,x+12,baseline-10,min(12f,col*.2f),0xFF11151A.toInt(),"mono",400,col-20)
                }}
            };footer(s,season)
        }
        fun standings(){
            val drivers=cfg.kind==WidgetKind.DRIVER_STANDINGS
            val season=if(drivers)d.racing.drivers.value.season else d.racing.teams.value.season
            val round=if(drivers)d.racing.drivers.value.round else d.racing.teams.value.round
            val list=if(drivers)d.racing.drivers.value.entries.map{listOf(it.position.toString(),it.code,it.family,it.points,it.teams.firstOrNull().orEmpty())} else d.racing.teams.value.entries.map{listOf(it.position.toString(),it.name,it.name,it.points,it.id)}
            label("CLASSIFICATION / $season / R$round",color=accent)
            val titleH=if(compact)0f else 37f
            if(!compact)text(if(drivers)"DRIVERS" else "CONSTRUCTORS",pad,61f,30f,ink,"condensed",700)
            val top=pad+24+titleH
            val rows=min(cfg.rows,((h-top-26)/26f).toInt().coerceAtLeast(1)).coerceAtMost(list.size)
            val step=min(42f,(h-top-25)/rows.coerceAtLeast(1))
            list.take(rows).forEachIndexed{i,e->val y=top+i*step
                line(pad,y,w-pad,y)
                text(e[0].padStart(2,'0'),pad,y+step*.72f,12f,muted,"mono",max=26f)
                rect(pad+30,y+7,3f,step-12,Color.parseColor(teamColor(e[4])))
                text(e[1],pad+43,y+step*.72f,min(22f,step*.6f),ink,"condensed",650,if(drivers&&w>300)64f else inner-115)
                if(drivers&&w>300)text(e[2],pad+108,y+step*.7f,12f,muted,max=inner-175)
                fit(e[3],w-pad-54,y+step*.72f,20f,54f,ink,"technical")
            };footer(if(drivers)d.racing.drivers else d.racing.teams,season)
        }
        fun next():Race?=if(d.racing.schedule.sample)d.racing.schedule.value.firstOrNull() else nextRace(d.racing.schedule.value,d.now)
        fun flag(country:String,x:Float,y:Float,size:Float) {
            val emoji=when(country){"Monaco"->"🇲🇨";"Italy"->"🇮🇹";"UK"->"🇬🇧";"USA"->"🇺🇸";"Japan"->"🇯🇵";"Australia"->"🇦🇺";"Singapore"->"🇸🇬";"Brazil"->"🇧🇷";"Canada"->"🇨🇦";"Spain"->"🇪🇸";"France"->"🇫🇷";"Austria"->"🇦🇹";"Belgium"->"🇧🇪";"Netherlands"->"🇳🇱";"Mexico"->"🇲🇽";"Hungary"->"🇭🇺";else->""}
            if(country=="Monaco"){c.save();c.clipPath(Path().apply{addCircle(x+size/2,y+size/2,size/2,Path.Direction.CW)});rect(x,y,size,size/2,0xFFCF1137.toInt());rect(x,y+size/2,size,size/2,Color.WHITE);c.restore()}
            else if(emoji.isNotEmpty())text(emoji,x,y+size,size,ink,max=size*1.5f)
        }
        fun map(race:Race?,b:Bounds,color:Int=muted) {
            if(race?.circuitId!="monaco") {
                label("CIRCUIT",b.x,b.y+b.h*.45f,color,b.w)
                text(race?.circuit ?: "Awaiting schedule",b.x,b.y+b.h*.65f,12f,color,"sans",400,b.w);return
            }
            val pts=floatArrayOf(.02f,.80f,.12f,.41f,.28f,.05f,.46f,.35f,.53f,.46f,.64f,.57f,.72f,.51f,.75f,.29f,.95f,.21f,.91f,.44f,.99f,.46f,.92f,.67f,.73f,.79f,.58f,.73f,.45f,.61f,.32f,.35f,.18f,.57f,.10f,.90f,.02f,.80f)
            val path=Path();pts.asList().chunked(2).forEachIndexed{i,a->if(i==0)path.moveTo(b.x+a[0]*b.w,b.y+a[1]*b.h) else path.lineTo(b.x+a[0]*b.w,b.y+a[1]*b.h)}
            p.color=color;p.strokeWidth=2.2f;p.style=Paint.Style.STROKE;p.strokeJoin=Paint.Join.ROUND;c.drawPath(path,p);p.style=Paint.Style.FILL
        }
        fun weekend(){
            val race=next();val s=d.racing.schedule
            if(race==null){label("RACE CALENDAR");text("Calendar complete",pad,h*.5f,25f);footer(s);return}
            val editorial=cfg.kind==WidgetKind.WEEKEND_EDITORIAL
            if(compact){
                label("R${race.round} / ${race.season}",pad,18f,color=if(editorial)accent else muted)
                fit(race.location.ifBlank{race.name.removeSuffix(" Grand Prix")},pad,49f,27f,inner)
                val z=race.start?.atZone(cfg.zone())
                text("Race",pad,h-28,15f,ink,"sans",600,inner*.3f)
                fit(z?.format(DateTimeFormatter.ofPattern(if(cfg.hour24)"EEE HH:mm" else "EEE h:mm a")) ?: "TIME TBA",w*.4f,h-28,18f,w*.6f-pad,ink,"technical")
                footer(s,race.season);return
            }
            label("ROUND ${race.round.toString().padStart(2,'0')} / ${race.season}",color=if(editorial)accent else muted)
            val titleY=if(compact)55f else 67f
            if(!editorial)flag(race.country,pad,titleY-24,23f)
            fit(if(editorial)race.name.removeSuffix(" Grand Prix").uppercase() else race.location,if(editorial)pad else pad+34,titleY,if(editorial)38f else 36f,if(editorial)inner else inner-34,ink,if(editorial)"condensed" else "sans",750)
            val mapH=if(h>300)h*.17f else if(h>250)h*.11f else 0f
            if(mapH>0){map(race,Bounds(pad+inner*.34f,titleY+11,inner*.64f,mapH));label(race.country,pad,titleY+24,max=inner*.33f)}
            val start=titleY+mapH+25
            val available=h-start-38
            val capacity=(available/25).toInt().coerceAtLeast(1)
            // At compact heights retain qualifying and race; do not squeeze six sessions into tiny rows.
            val sessions=if(capacity>=race.sessions.size)race.sessions else race.sessions.takeLast(capacity)
            val step=min(35f,available/sessions.size.coerceAtLeast(1))
            sessions.forEachIndexed{i,e->val y=start+i*step;val z=e.start?.atZone(cfg.zone());val raceRow=e.name=="Race"
                if(editorial&&raceRow)rect(pad-6,y-14,inner+12,step,accent)
                val color=if(editorial&&raceRow)0xFF101418.toInt() else ink
                text(e.name,pad,y+7,min(19f,step*.65f),color,if(editorial)"condensed" else "sans",if(raceRow)750 else 500,inner*.46f)
                text(z?.format(DateTimeFormatter.ofPattern("EEE",Locale.getDefault()))?.uppercase() ?: "—",w-pad-inner*.43f,y+7,11f,if(editorial&&raceRow)color else muted,max=inner*.17f)
                fit(z?.format(DateTimeFormatter.ofPattern(if(cfg.hour24)"HH:mm" else "h:mm a")) ?: "TBA",w-pad-inner*.25f,y+7,18f,inner*.25f,color,"technical")
            }
            label(cfg.zone().id,pad,h-24f);footer(s,race.season)
        }
        fun countdown(){
            val race=next();val s=d.racing.schedule
            if(h>=100)label("NEXT / LIGHTS OUT",color=accent)
            if(race==null){text("Season complete",pad,h*.5f,24f);footer(s);return}
            val duration=race.start?.let{Duration.between(d.now,it)}
            val future=duration!=null&&!duration.isNegative
            if(h<180){
                fit(race.name.removeSuffix(" Grand Prix").uppercase(),pad,h*.47f,22f,inner*.62f)
                fit(if(future)"${duration!!.toDays()}d ${duration.toHours()%24}h" else if(s.sample)"ARCHIVE" else "TBA",w*.68f,h*.55f,26f,w*.32f-pad,accent,"technical")
            }else{
                fit(race.name.removeSuffix(" Grand Prix").uppercase(),pad,60f,27f,inner)
                if(future){
                    fit(duration!!.toDays().toString().padStart(2,'0'),pad,h*.71f,min(100f,h*.4f),inner*.45f,ink,"condensed")
                    fit((duration.toHours()%24).toString().padStart(2,'0'),w*.55f,h*.71f,min(100f,h*.4f),inner*.45f,accent,"condensed")
                    label("DAYS",pad,h*.8f);label("HOURS",w*.55f,h*.8f)
                }else fit(if(s.sample)"ARCHIVE / ${race.season}" else "TIME TBA",pad,h*.65f,42f,inner)
            };if(h>=100)label("R${race.round} / ${race.date}",pad,h-24);footer(s,race.season)
        }
        fun results(){
            val s=d.racing.result;val r=s.value
            label("FINAL / CLASSIFICATION",color=accent)
            if(r==null||r.finishers.isEmpty()){text("No result published",pad,h*.5f,23f);footer(s);return}
            if(h<200){
                val winner=r.finishers.first()
                fit(r.race.name.removeSuffix(" Grand Prix").uppercase(),pad,48f,24f,inner)
                text("01 / ${winner.code}",pad,h-26,24f,Color.parseColor(teamColor(winner.team)),"condensed",700,inner*.5f)
                label(r.finishers.drop(1).take(2).joinToString(" / "){it.code},w*.57f,h-28,max=w*.43f-pad)
                footer(s,r.race.season);return
            }
            fit(r.race.name.removeSuffix(" Grand Prix").uppercase(),pad,56f,30f,inner)
            val winner=r.finishers.first();val color=Color.parseColor(teamColor(winner.team));val top=75f
            rect(pad,top,inner*.35f,h-top-35,color)
            fit("01",pad+9,top+min((h-top)*.42f,72f),72f,inner*.3f,0xFF131719.toInt(),"condensed")
            text(winner.code,pad+10,h-49,min(29f,inner*.1f),0xFF131719.toInt(),"condensed",700,inner*.3f)
            val x=pad+inner*.42f;val col=inner*.58f
            text(winner.name,x,top+23,20f,ink,"condensed",600,col)
            r.finishers.drop(1).take(if(h>250)4 else 2).forEachIndexed{i,e->val y=top+56+i*29;line(x,y-17,w-pad,y-17);text("${e.position.padStart(2,'0')}  ${e.code}",x,y,17f,if(i==0)ink else muted,"technical",500,col)}
            footer(s,r.race.season)
        }
        fun circuitCard(){val race=next();val s=d.racing.schedule
            if(h<180){
                label("R${race?.round ?: "—"} / ${race?.season ?: "—"}",pad,18f,max=inner*.5f)
                fit(race?.location ?: "Circuit",pad,53f,24f,inner*.5f)
                label(race?.date?.toString() ?: "DATE TBA",pad,h-25,max=inner*.5f)
                map(race,Bounds(w*.56f,15f,w*.44f-pad,h-45));footer(s,race?.season.orEmpty());return
            }
            label("ROUND FILE / ${race?.season ?: "—"}")
            text(race?.round?.toString()?.padStart(2,'0') ?: "—",pad,h*.32f,min(88f,h*.25f),accent,"condensed",700,w*.5f)
            label(race?.country ?: "AWAITING SCHEDULE",w*.54f,h*.16f,max=w*.46f-pad)
            label(race?.date?.toString() ?: "DATE TBA",w*.54f,h*.24f,max=w*.46f-pad)
            map(race,Bounds(pad,h*.37f,inner,h*.35f),ink)
            fit(race?.name?.removeSuffix(" Grand Prix") ?: "Circuit unavailable",pad,h*.87f,25f,inner)
            footer(s,race?.season.orEmpty())
        }
        fun transport(){
            if(!preview)return
            val count=if(musicIsCompact(cfg.kind,h))2 else 3
            val start=w-12-count*44
            val actions=if(count==2)listOf("toggle","next") else listOf("previous","toggle","next")
            actions.forEachIndexed{i,a->val x=start+i*44+22;val y=h-24
                val enabled=d.mediaAccess&&when(a){"previous"->d.media.previous;"next"->d.media.next;else->d.media.toggle}
                val color=if(enabled)if(cfg.kind==WidgetKind.ARTWORK)Color.WHITE else ink else muted
                if(a=="toggle"&&d.media.playing){rect(x-5,y-7,3f,14f,color);rect(x+2,y-7,3f,14f,color)}
                else{val backwards=a=="previous";val path=Path().apply{moveTo(x+if(backwards)5f else -5f,y-7);lineTo(x+if(backwards)-5f else 5f,y);lineTo(x+if(backwards)5f else -5f,y+7);close()};p.color=color;c.drawPath(path,p);if(a!="toggle")rect(x+if(backwards)-8f else 7f,y-7,2f,14f,color)}
            }
        }
        fun music(){
            val m=d.media;val cover=m.artwork
            val track=if(!d.mediaAccess)"Connect your player" else m.title
            val artist=if(!d.mediaAccess)"Enable media access" else m.artist
            val k=cfg.kind
            if(musicIsCompact(k,h)){
                val size=min(h-20,w*.21f);photograph(cover,Bounds(10f,(h-size)/2,size,size),8f)
                val x=size+22;val max=w-x-100
                text(track,x,h*.42f,15f,ink,"sans",650,max)
                text(artist,x,h*.42f+20,10f,muted,max=max)
                rect(10f+size-2,(h-size)/2,2f,size,accent)
                transport();return
            }
            val bottom=h-49f
            when(k){
                WidgetKind.EDITORIAL->{
                    val artH=if(tall)h*.53f else bottom
                    val artW=if(tall)w else w*.48f
                    photograph(cover,Bounds(if(tall)0f else w-artW,0f,artW,artH))
                    val x=pad;val top=if(tall)artH+22 else pad+6;val col=if(tall)inner else w*.48f-2*pad
                    label(if(m.playing)"ON ROTATION" else "LISTENING ROOM",x,top,accent,col)
                    title(track,x,top+36,min(36f,h*.12f),col)
                    text(artist,x,if(tall)bottom-10 else bottom-27,12f,muted,max=col)
                    line(pad,bottom,w-pad,bottom);label("STEREO / ${if(m.playing)"PLAYING" else "PAUSED"}",pad,h-20,max=w-165)
                }
                WidgetKind.GLASS->{
                    if(cover!=null){photograph(cover,Bounds(0f,0f,w,h));rect(0f,0f,w,h,if(cfg.light)0xD0F2EEE3.toInt() else 0xC01C242A.toInt())}
                    rect(12f,12f,w-24,h-24,if(cfg.light)0x75FFFFFF else 0x35FFFFFF,18f)
                    line(28f,13f,w-28,13f,0x80FFFFFF.toInt())
                    val size=min(88f,h*.39f);photograph(cover,Bounds(pad+4,pad+5,size,size),12f)
                    val x=pad+size+19
                    title(track,x,pad+34,25f,w-x-pad-6)
                    text(artist,pad+4,bottom-18,13f,muted,max=inner-8)
                    label("${if(m.playing)"PLAYING" else "PAUSED"} / SESSION",pad+4,h-20,max=w-175)
                }
                WidgetKind.VINYL->{
                    val stacked=w/h<1.5f
                    val radius=min(w*.35f,(bottom-if(stacked)74 else 40)*.49f);val cx=if(stacked)w*.5f else w*.3f;val cy=if(stacked)radius+12 else bottom*.48f
                    circle(cx,cy,radius,0xFF090A0B.toInt())
                    for(i in 1..18)circle(cx,cy,radius*(.45f+i*.029f),if(i%4==0)0xFF333437.toInt() else 0xFF202124.toInt(),.55f)
                    circlePhoto(cover,cx,cy,radius*.39f);circle(cx,cy,3f,accent)
                    val tx=if(stacked)pad else w*.63f;val ty=if(stacked)bottom-38 else bottom*.36f;val tw=if(stacked)inner else w-tx-pad
                    text(track,tx,ty,23f,ink,"serif",500,tw);text(artist,tx,ty+21,11f,muted,max=tw)
                    label("33⅓ / RECORD STUDY",pad,h-20,max=w-160)
                }
                WidgetKind.BRUTALIST->{
                    rect(0f,0f,7f,h,accent);label("AUDIO / ${if(m.playing)"PLAY" else "HOLD"}",pad,pad+4)
                    title(track.uppercase(),pad,73f,min(54f,h*.2f),inner,ink,"condensed")
                    val artSize=min(50f,h*.22f);photograph(cover,Bounds(w-pad-artSize,bottom-artSize-6,artSize,artSize))
                    text(artist.uppercase(),pad,bottom-14,11f,muted,"mono",400,inner-artSize-12)
                    line(pad,bottom,w-pad,bottom,ink,3f);label("HI / FI",pad,h-20)
                }
                WidgetKind.MATRIX->{
                    for(x in 0..(w/7).toInt())for(y in 0..(h/7).toInt())circle(x*7f,y*7f,.55f,if(cfg.light)0x18000000 else 0x18FFFFFF)
                    label(if(m.playing)"● SIGNAL / ACTIVE" else "○ SIGNAL / IDLE",color=accent)
                    val artSize=min(52f,h*.26f);photograph(cover,Bounds(w-pad-artSize,pad+20,artSize,artSize),4f)
                    title(track.uppercase(),pad,80f,30f,inner-artSize-12,accent,"dots")
                    text(artist,pad,bottom-12,11f,muted,"mono",400,inner)
                    line(pad,bottom,w-pad,bottom);label("DIGITAL / STEREO",pad,h-20,max=w-160)
                }
                WidgetKind.CASSETTE->{
                    val top=14f;val tapeH=bottom-22
                    rect(14f,top,w-28,tapeH,0xFFB9AB8C.toInt(),9f)
                    rect(25f,top+11,w-50,tapeH*.38f,0xFFEDE4D0.toInt(),3f)
                    label("SIDE A / ${if(m.playing)"PLAY" else "PAUSE"}",33f,top+25,0xFF514B40.toInt())
                    text(track,33f,top+50,22f,0xFF242621.toInt(),"serif",500,w-66)
                    val cy=top+tapeH*.66f;val radius=min(22f,tapeH*.15f)
                    rect(w*.24f,cy-radius-7,w*.52f,radius*2+14,0xFF2F3433.toInt(),radius+7)
                    listOf(w*.3f,w*.7f).forEach{x->circle(x,cy,radius,0xFFDAD3BA.toInt());circle(x,cy,radius*.42f,0xFF313533.toInt());for(i in 0..5){val a=i*PI/3;line(x+cos(a).toFloat()*radius*.65f,cy+sin(a).toFloat()*radius*.65f,x+cos(a).toFloat()*radius*.87f,cy+sin(a).toFloat()*radius*.87f,0xFF575B50.toInt(),2f)}}
                    text(artist,30f,top+tapeH-12,10f,0xFF383D35.toInt(),"mono",400,w-60)
                    label("C / 60",pad,h-20)
                }
                WidgetKind.ARTWORK->{
                    photograph(cover,Bounds(0f,0f,w,h))
                    p.shader=LinearGradient(0f,h*.35f,0f,h,intArrayOf(Color.TRANSPARENT,0xF5080C10.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
                    text(track,pad,bottom-38,27f,Color.WHITE,"sans",650,inner)
                    text(artist,pad,bottom-16,12f,Color.LTGRAY,max=inner)
                    label(if(m.playing)"PLAYING" else "PAUSED",pad,h-20,Color.LTGRAY,max=w-160)
                }
                else->Unit
            }
            transport()
        }
        fun clock(){
            val k=cfg.kind
            when(k){
                WidgetKind.SWISS->{if(h>=90){rect(pad,14f,26f,4f,accent);label("LOCAL TIME",w-pad-78,21f,max=78f)}}
                WidgetKind.CINEMATIC->{if(h>=90){line(pad,13f,w-pad,13f);label("THE PRESENT",pad,h-8,max=inner)}}
                WidgetKind.BRUTAL_CLOCK->{val split=ClockDesign.plan(cfg,w,h).getOrNull(1)?.bounds?.y ?: h*.5f;rect(0f,split,w,2f,accent);label("H",w-pad-9,pad+8,max=10f);label("M",w-pad-9,split+8,max=10f)}
                WidgetKind.ANALOG->{circle(w/2,h/2,5f,accent)}
                WidgetKind.SPLIT->{if(cfg.showDate&&h>=96&&w/h>1.2f)line(w*.62f,pad,w*.62f,h-pad) else if(cfg.showDate&&h>=96)line(pad,h*.54f,w-pad,h*.54f)}
                WidgetKind.DUAL->{line(w*.5f,pad,w*.5f,h-pad);label(zoneLabel(cfg.timezone),pad,h*.23f,max=w*.44f-pad);label(zoneLabel(cfg.secondaryZone),w*.54f,h*.23f,max=w*.46f-pad)}
                WidgetKind.TERMINAL->{if(h>=90){label("> TIME / ${zoneLabel(cfg.timezone)}",color=accent);line(pad,33f,w-pad,33f);rect(w-pad-6,h-pad-6,6f,6f,accent)}}
                WidgetKind.SERIF->{if(h>=90)line(w*.4f,16f,w*.6f,16f,accent)}
                else->Unit
            }
            if(!preview)return
            if(k==WidgetKind.ANALOG){analog();return}
            ClockDesign.plan(cfg,w,h).forEach{t->
                val value=d.now.atZone(java.time.ZoneId.of(t.zone)).format(DateTimeFormatter.ofPattern(t.format,Locale.getDefault()))
                val sample=when(t.role){"time","second"->if(t.format.contains(':'))"23:58" else "88";"minutes"->"88";else->value}
                val size=ClockDesign.fittedSize(context,t,sample)
                p.typeface=LabType.face(context,t.face,t.weight);p.textSize=size;p.color=if(cfg.accentText&&t.role in listOf("time","second","minutes","day"))accentTextColor(cfg) else if(t.role=="minutes"&&k==WidgetKind.BRUTAL_CLOCK)accent else if(t.role=="date")muted else ink
                val lines=value.lines();val fontH=p.fontMetrics.descent-p.fontMetrics.ascent
                var y=t.bounds.y+(t.bounds.h-lines.size*fontH)/2-p.fontMetrics.ascent
                lines.forEach{v->val x=when(t.alignment){"center"->t.bounds.x+(t.bounds.w-p.measureText(v))/2;"end"->t.bounds.x+t.bounds.w-p.measureText(v);else->t.bounds.x};c.drawText(v,x,y,p);y+=fontH}
            }
        }
        fun zoneLabel(zone:String)=if(zone=="device")"LOCAL" else zone.substringAfterLast('/').replace('_',' ')
        fun analog(){
            val scale=(min(w,h)-16f).coerceAtLeast(1f)/200f
            val cx=w/2;val cy=h/2
            for(i in 0..59){
                val a=i*PI/30-PI/2
                val outer=94f*scale;val inner=(if(i%5==0)85f else 90f)*scale
                line(cx+cos(a).toFloat()*inner,cy+sin(a).toFloat()*inner,cx+cos(a).toFloat()*outer,cy+sin(a).toFloat()*outer,ink,1.3f*scale)
            }
            val now=d.now.atZone(cfg.zone())
            val hour=(now.hour%12+now.minute/60f)*PI/6-PI/2
            val minute=now.minute*PI/30-PI/2
            p.strokeCap=Paint.Cap.ROUND
            line(cx-cos(hour).toFloat()*8*scale,cy-sin(hour).toFloat()*8*scale,cx+cos(hour).toFloat()*49*scale,cy+sin(hour).toFloat()*49*scale,ink,3*scale)
            line(cx-cos(minute).toFloat()*8*scale,cy-sin(minute).toFloat()*8*scale,cx+cos(minute).toFloat()*75*scale,cy+sin(minute).toFloat()*75*scale,ink,1.7f*scale)
            p.strokeCap=Paint.Cap.BUTT
            circle(cx,cy,6f,accent)
        }
        fun photos(){
            val files=cfg.photos;val images=files.map{ArtCache.get(context,it)}
            fun imageAt(i:Int,b:Bounds,round:Float=0f){photograph(images.getOrNull(i),b,round);if(images.getOrNull(i)==null){label(if(files.isEmpty())"ADD A PHOTOGRAPH" else "ADD ANOTHER PHOTO",b.x+12,b.y+b.h*.77f,muted,b.w-24)}}
            when(cfg.kind){
                WidgetKind.PHOTO->imageAt(0,Bounds(0f,0f,w,h),cfg.corners.toFloat())
                WidgetKind.FRAME->{imageAt(0,Bounds(12f,12f,w-24,h-24-if(cfg.caption.isNotBlank())34 else 0),cfg.corners.toFloat());if(cfg.caption.isNotBlank())text(cfg.caption,pad,h-21,13f,ink,"sans",500,inner)}
                WidgetKind.POLAROID->{rect(0f,0f,w,h,0xFFF5F0E4.toInt());imageAt(0,Bounds(13f,13f,w-26,h-72));text(cfg.caption,24f,h-29,20f,0xFF282C2B.toInt(),"serif",400,w-48)}
                WidgetKind.FILMSTRIP->{
                    rect(0f,0f,w,h,0xFF111315.toInt());val count=files.size.coerceIn(2,4);val margin=14f;val gap=6f
                    val cell=(w-2*margin-(count-1)*gap)/count
                    val border=if(h<100)10f else 20f
                    for(i in 0 until count)imageAt(i,Bounds(margin+i*(cell+gap),border,cell,h-2*border))
                    val hole=if(h<100)4f else 7f;val edge=if(h<100)2f else 5f
                    for(i in 0..(w/17).toInt()){rect(i*17f+4,edge,8f,hole,0xFFB6B9B2.toInt(),1.5f);rect(i*17f+4,h-edge-hole,8f,hole,0xFFB6B9B2.toInt(),1.5f)}
                }
                WidgetKind.COLLAGE->{
                    val gap=5f
                    if(tall){imageAt(0,Bounds(0f,0f,w,h*.6f-gap));imageAt(1,Bounds(0f,h*.6f,w*.5f-gap/2,h*.4f));imageAt(2,Bounds(w*.5f+gap/2,h*.6f,w*.5f-gap/2,h*.4f))}
                    else{imageAt(0,Bounds(0f,0f,w*.61f-gap,h));imageAt(1,Bounds(w*.61f,0f,w*.39f,h*.5f-gap/2));imageAt(2,Bounds(w*.61f,h*.5f+gap/2,w*.39f,h*.5f-gap/2))}
                }
                else->Unit
            }
        }
        fun essentials(){when(cfg.kind){
            WidgetKind.BATTERY,WidgetKind.BATTERY_EDITORIAL->battery()
            WidgetKind.FOCUS->{
                val active=d.focus.active;val cx=pad+15;val cy=h*.42f
                circle(cx,cy,14f,if(active)accent else muted,1.2f);line(cx-8,cy,cx+8,cy,if(active)accent else muted,2f)
                text(d.focus.title,pad+45,cy+6,min(27f,w*.075f),ink,"sans",500,inner-45)
                label("ANDROID / INTERRUPTION FILTER",pad,h-9)
            }
            WidgetKind.DATE,WidgetKind.TODAY->{
                if(preview)clock()
                if(h<180)return
                val top=if(cfg.kind==WidgetKind.DATE)max(h*.60f,122f) else max(h*.55f,112f)
                line(pad,top-10,w-pad,top-10)
                val agenda=d.agenda
                if(!agenda.permitted){text("Your calendar, privately.",pad,top+17,17f,ink,"serif",500,inner);label("TAP TO ENABLE CALENDAR ACCESS",pad,top+38,max=inner)}
                else if(agenda.error){text("Calendar unavailable",pad,top+17,17f,max=inner)}
                else if(agenda.events.isEmpty()){text("A little room to breathe.",pad,top+20,18f,ink,"serif",500,inner);label("NO UPCOMING EVENTS",pad,top+42)}
                else{
                    val rows=((h-top-10)/42).toInt().coerceAtLeast(1)
                    agenda.events.take(rows).forEachIndexed{i,e->val y=top+i*43
                        rect(pad,y+5,2f,28f,e.color)
                        text(e.title,pad+12,y+15,15f,ink,"sans",550,inner-12)
                        val time=if(e.allDay)"ALL DAY / "+e.begin.atZone(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("d MMM")) else e.begin.atZone(cfg.zone()).format(DateTimeFormatter.ofPattern(if(cfg.hour24)"EEE HH:mm" else "EEE h:mm a"))
                        label(time,pad+12,y+32,max=inner-12)
                    }
                }
            }
            else->Unit
        }}
        fun battery(){val percent=d.battery.percent;val fraction=(percent ?: 0)/100f
            if(cfg.kind==WidgetKind.BATTERY){
                fit("${percent ?: "—"}%",pad,h*if(h<80).60f else .65f,min(72f,h*if(h<80).42f else .52f),min(inner*.68f,inner-49),ink,"sans",300)
                val x=w-pad-39;val y=h*.36f;rect(x,y,34f,17f,if(cfg.light)0x20202020 else 0x30FFFFFF,3f);rect(x+3,y+3,28*fraction,11f,accent,1f);rect(x+35,y+5,2f,7f,muted)
                label(if(d.battery.charging)"CHARGING" else "ON BATTERY",pad,h-if(h<80)8f else 12f)
            }else{
                if(compact){
                    label("ENERGY / RESERVE",pad,18f)
                    fit("${percent ?: "—"}%",pad,h*.69f,min(54f,h*.45f),inner*.6f,ink,"technical")
                    val cx=w*.78f;val cy=h*.47f;val r=min(w*.13f,h*.22f)
                    circle(cx,cy,r,ruleColor,5f)
                    p.color=accent;p.style=Paint.Style.STROKE;p.strokeWidth=5f;c.drawArc(RectF(cx-r,cy-r,cx+r,cy+r),-90f,360*fraction,false,p);p.style=Paint.Style.FILL
                    label(if(d.battery.charging)"CHARGING" else "ON BATTERY",pad,h-10);return
                }
                label("ENERGY / RESERVE")
                val cy=h*.48f;val cx=w*.7f;val radius=min(w*.22f,h*.3f)
                p.color=ruleColor;p.strokeWidth=9f;p.style=Paint.Style.STROKE;c.drawArc(RectF(cx-radius,cy-radius,cx+radius,cy+radius),140f,260f,false,p)
                p.color=accent
                c.drawArc(RectF(cx-radius,cy-radius,cx+radius,cy+radius),140f,260*fraction,false,p)
                p.style=Paint.Style.FILL
                fit((percent ?: "—").toString(),pad,cy+18,75f,inner*.45f,ink,"technical")
                label("PERCENT",pad,cy+39)
                line(pad,h-49,w-pad,h-49)
                text(if(d.battery.charging)"Charging" else "On battery",pad,h-23,16f,ink,"serif",500,inner)
            }
        }
        fun launchers(){
            when(cfg.kind){
                WidgetKind.LABEL->{fit(cfg.label,pad,h*.61f,min(48f,h*.46f),inner,if(cfg.accentText)accentTextColor(cfg) else ink,"serif",500);label("${cfg.caption}",pad,h-12,max=inner)}
                WidgetKind.SECTION->{label(cfg.caption.ifBlank{"01 / PERSONAL"},pad,pad+2);fit(cfg.label.uppercase(),pad,h*.69f,min(26f,h*.3f),inner,if(cfg.accentText)accentTextColor(cfg) else ink,"condensed");line(pad,h-12,w-pad,h-12,accent,1.5f)}
                else->{
                    if(cfg.kind==WidgetKind.LAUNCHER){fit(cfg.label,pad,if(h<150)28f else 40f,if(h<150)20f else 32f,inner,if(cfg.accentText)accentTextColor(cfg) else ink,"serif",500);line(pad,if(h<150)36f else 54f,w-pad,if(h<150)36f else 54f)}
                    if(cfg.apps.isEmpty()){text("Choose your applications",pad,h*.65f,17f,ink,"sans",500,inner);return}
                    val plan=shortcutPlan(cfg,w,h);val b=plan.bounds;val cellW=b.w/plan.columns;val cellH=b.h/plan.rows
                    cfg.apps.take(plan.columns*plan.rows).forEachIndexed{i,component->
                        val x=b.x+i%plan.columns*cellW;val y=b.y+i/plan.columns*cellH
                        val icon=LaunchApps.icon(context,component)
                        val name=LaunchApps.label(context,component)
                        val labelH=if(cfg.showLabels)18f else 0f
                        val size=min(38f,min(cellW-12,cellH-labelH-4)).coerceAtLeast(12f)
                        val left=x+(cellW-size)/2;val top=y+max(0f,(cellH-size-labelH)/2)
                        if(cfg.iconStyle=="letters"){
                            circle(x+cellW/2,top+size/2,size/2,ruleColor,1f)
                            val letter=name.take(1).uppercase(Locale.getDefault())
                            p.typeface=LabType.face(context,"serif",500);p.textSize=size*.66f
                            val letterWidth=p.measureText(letter)
                            text(letter,x+(cellW-letterWidth)/2,top+size*.74f,size*.66f,if(cfg.accentText)accentTextColor(cfg) else ink,"serif",500,size)
                        }else if(icon!=null){
                            c.save()
                            if(cfg.iconStyle=="mono"){
                                val gray=Paint(Paint.ANTI_ALIAS_FLAG).apply{colorFilter=ColorMatrixColorFilter(ColorMatrix().apply{setSaturation(0f)})}
                                c.saveLayer(RectF(left,top,left+size,top+size),gray)
                            }
                            c.translate(left,top);icon.setBounds(0,0,size.toInt(),size.toInt());icon.draw(c)
                            if(cfg.iconStyle=="mono")c.restore()
                            c.restore()
                        }else{circle(x+cellW/2,top+size/2,size/2,ruleColor);text("?",x+cellW/2-4,top+size*.7f,18f)}
                        if(cfg.showLabels){
                            p.typeface=LabType.face(context,"sans");p.textSize=9f
                            val width=p.measureText(name).coerceAtMost(cellW-6)
                            text(name,x+(cellW-width)/2,top+size+14,9f,muted,max=cellW-6)
                        }
                    }
                }
            }
        }
    }
}
