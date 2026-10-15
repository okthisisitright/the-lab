package dev.widgetlab.widgets

import android.content.Context
import android.graphics.*
import android.text.TextUtils
import dev.widgetlab.R
import dev.widgetlab.core.*
import dev.widgetlab.data.*
import dev.widgetlab.media.MediaInfo
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

data class RenderData(val racing: RacingSnapshot, val media: MediaInfo, val battery: BatteryInfo, val agenda: AgendaData, val mediaAccess: Boolean, val now: Instant = Instant.now())

/** Canvas is shared by the native gallery and real RemoteViews. Controls remain native views. */
class WidgetRenderer(private val context: Context) {
    private val regular = context.resources.getFont(R.font.space_grotesk)
    private val bold = Typeface.create(regular, 700, false)
    private val mono = Typeface.create("monospace", Typeface.NORMAL)
    fun render(config: WidgetConfig, data: RenderData, width: Int = 630, height: Int = (config.kind.height * width / 420), preview: Boolean = true): Bitmap {
        val w = width.coerceIn(120, 720)
        val h = height.coerceIn(60, (420_000 / w).coerceAtMost(900))
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(w / 420f, h / config.kind.height.toFloat())
        Draw(canvas, config, data, config.kind.height.toFloat(), preview).draw()
        return bitmap
    }
    private inner class Draw(val c: Canvas, val cfg: WidgetConfig, val d: RenderData, val h: Float, val preview: Boolean) {
        val ink = if (cfg.light) Color.rgb(21, 23, 23) else Color.rgb(245, 244, 237)
        val paper = if (cfg.light) Color.rgb(245, 242, 233) else Color.rgb(22, 24, 28)
        val muted = if (cfg.light) Color.rgb(111, 112, 108) else Color.rgb(164, 168, 171)
        private val selectedAccent = Color.parseColor(cfg.accent)
        val accent = if(cfg.light) Color.rgb((Color.red(selectedAccent)*.48f).toInt(),(Color.green(selectedAccent)*.48f).toInt(),(Color.blue(selectedAccent)*.48f).toInt()) else selectedAccent
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        fun rect(x: Float, y: Float, w: Float, height: Float, color: Int, radius: Float = 0f) { p.color = color; p.style = Paint.Style.FILL; c.drawRoundRect(RectF(x, y, x+w, y+height), radius, radius, p) }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = muted, width: Float = 1f) { p.color = color; p.strokeWidth = width; p.style = Paint.Style.STROKE; c.drawLine(x1, y1, x2, y2, p); p.style = Paint.Style.FILL }
        fun circle(x: Float, y: Float, r: Float, color: Int, stroke: Float = 0f) { p.color = color; p.style = if (stroke > 0) Paint.Style.STROKE else Paint.Style.FILL; p.strokeWidth = stroke; c.drawCircle(x,y,r,p); p.style = Paint.Style.FILL }
        fun text(value: String, x: Float, y: Float, size: Float, color: Int = ink, heavy: Boolean = false, max: Float = 368f, face: Typeface? = null) {
            p.color = color; p.typeface = face ?: if (cfg.typography == "mono") mono else if (heavy) bold else regular; p.textSize = size
            var fitted=value.replace('\n',' ')
            if(p.measureText(fitted)>max) {
                var count=p.breakText(fitted,true,(max-p.measureText("…")).coerceAtLeast(0f),null)
                if(count>0 && count<fitted.length && Character.isHighSurrogate(fitted[count-1]))count--
                fitted=fitted.take(count).trimEnd()+"…"
            }
            c.save();c.clipRect(x,y-size*1.3f,x+max,y+size*.4f)
            c.drawText(fitted, x, y, p);c.restore()
        }
        fun titleLines(value: String,x: Float,y: Float,size: Float,max: Float,color: Int=ink) {
            p.typeface=if(cfg.typography=="mono")mono else bold;p.textSize=size
            val clean=value.replace('\n',' ')
            if(p.measureText(clean)<=max) {text(clean,x,y,size,color,true,max);return}
            val chars=p.breakText(clean,true,max,null).coerceAtLeast(1)
            val space=clean.take(chars).lastIndexOf(' ')
            val cut=if(space>0)space else chars
            text(clean.take(cut),x,y,size,color,true,max)
            text(clean.substring(cut).trimStart(),x,y+size*1.1f,size,color,true,max)
        }
        fun label(value: String, x: Float = 24f, y: Float = 30f, color: Int = muted, max: Float = 370f) = text(value.uppercase(Locale.ROOT), x,y,10f,color,true,max)
        fun photo(bitmap: Bitmap?, x: Float, y: Float, w: Float, height: Float, round: Float = 0f, crop: String = "center") {
            val r = RectF(x,y,x+w,y+height)
            if (bitmap == null) {
                rect(x,y,w,height, if(cfg.light) Color.rgb(219,220,208) else Color.rgb(47,52,54), round)
                c.save(); c.clipRect(r)
                for (i in -5..12) line(x+i*w/8, y+height, x+i*w/8+w, y, Color.argb(65,160,174,150),2f)
                circle(x+w*.6f, y+height*.4f, min(w,height)*.22f, accent)
                circle(x+w*.6f, y+height*.4f, min(w,height)*.08f, paper)
                c.restore(); return
            }
            c.save(); val path = Path().apply { addRoundRect(r, round, round, Path.Direction.CW) }; c.clipPath(path)
            val scale = max(w/bitmap.width, height/bitmap.height)
            val bw = bitmap.width*scale; val bh=bitmap.height*scale
            val top = if(crop == "top") y else y+(height-bh)/2
            c.drawBitmap(bitmap, null, RectF(x+(w-bw)/2,top,x+(w+bw)/2,top+bh),p)
            c.restore()
        }
        fun customArt(): Bitmap? = cfg.image.takeIf { it.isNotBlank() }?.let { context.artFile(it) }?.let { BitmapFactory.decodeFile(it.path) }
        fun portrait(driver: Competitor): Bitmap? = customArt() ?: if(driver.id=="hamilton") {
            context.assets.open("portraits/hamilton.jpg").use { BitmapFactory.decodeStream(it) }
        } else null
        fun freshness(section: Section<*>, season: String = ""): String = if(section.sample) "SAMPLE / $season · OFFLINE STUDY" else {
            val stamp = section.refreshed?.atZone(cfg.zone())?.format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.getDefault())) ?: "—"
            "${if(section.error != null) "OFFLINE" else if(section.stale(d.now)) "CACHED" else "UPDATED"} $stamp${if(season.isNotBlank()) " / $season" else ""}"
        }
        fun footer(value: String) = label(value,24f,h-12f,muted,372f)
        fun draw() {
            c.save(); c.clipPath(Path().apply { addRoundRect(RectF(0f,0f,420f,h),20f,20f,Path.Direction.CW) })
            c.drawColor(paper)
            when(cfg.kind) {
                WidgetKind.DRIVER -> driver()
                WidgetKind.CONSTRUCTOR -> constructor()
                WidgetKind.DRIVER_STANDINGS, WidgetKind.CONSTRUCTOR_STANDINGS -> standings()
                WidgetKind.WEEKEND -> weekend()
                WidgetKind.COUNTDOWN -> countdown()
                WidgetKind.RESULTS -> results()
                WidgetKind.EDITORIAL, WidgetKind.GLASS, WidgetKind.VINYL, WidgetKind.BRUTALIST, WidgetKind.MATRIX, WidgetKind.CASSETTE, WidgetKind.COMPACT -> music()
                WidgetKind.CLOCK, WidgetKind.SWISS -> clock()
                WidgetKind.DATE -> date()
                WidgetKind.BATTERY -> battery()
                WidgetKind.AGENDA -> agenda()
            }
            c.restore()
        }
        fun driver() {
            val s = d.racing.drivers; val table = s.value
            val driver = if(cfg.selection.isBlank()) table.entries.firstOrNull() else table.entries.find { it.id == cfg.selection }
            if(driver == null) { label("DRIVER / ${table.season}"); text("Driver unavailable",24f,100f,28f,heavy=true); footer(freshness(s,table.season)); return }
            val color=Color.parseColor(teamColor(driver.teams.firstOrNull().orEmpty()))
            rect(218f,0f,202f,h-57f,color)
            val art=portrait(driver)
            if(art != null) photo(art,218f,0f,202f,h-57f,crop=cfg.crop)
            else { text(driver.number,230f,142f,112f,Color.argb(115,0,0,0),true,185f); label("ADD YOUR PORTRAIT",232f,h-74f,Color.rgb(22,24,28),180f) }
            label("DRIVER / ${table.season}", max=185f)
            text(driver.number,24f,100f,65f,accent,true,175f)
            text(driver.given,24f,h-106f,18f,max=186f)
            text(driver.family.uppercase(),24f,h-78f,27f,heavy=true,max=187f)
            line(24f,h-58f,396f,h-58f)
            text("P${driver.position.toString().padStart(2,'0')}",24f,h-30f,23f,heavy=true,max=68f)
            text("${driver.points} PTS",105f,h-30f,20f,heavy=true,max=125f)
            text(driver.nationality,242f,h-31f,12f,muted,max=154f)
            footer(freshness(s,table.season))
            label(driver.teams.joinToString(" / ") { id -> d.racing.teams.value.entries.find { it.id==id }?.name ?: id.replace('_',' ') },24f,48f,muted,185f)
        }
        fun constructor() {
            val s = d.racing.teams; val table=s.value
            val team = if(cfg.selection.isBlank()) table.entries.firstOrNull() else table.entries.find { it.id==cfg.selection }
            label("CONSTRUCTOR / ${table.season}")
            if(team==null) { text("Team unavailable",24f,100f,28f,heavy=true); footer(freshness(s,table.season)); return }
            val color=Color.parseColor(teamColor(team.id))
            rect(24f,48f,5f,45f,color)
            text(team.name.uppercase(),42f,82f,32f,heavy=true,max=346f)
            val art=customArt()
            if(art!=null) photo(art,230f,100f,166f,75f,8f,cfg.crop)
            else {
                // Original geometric constructor identifier; no unlicensed team logo or car photograph.
                for(i in 0..3) { val x=245f+i*30f; val path=Path().apply{moveTo(x,107f);lineTo(x+22,107f);lineTo(x,166f);lineTo(x-22,166f);close()}; p.color=color; c.drawPath(path,p) }
            }
            text("P${team.position}",24f,152f,54f,heavy=true,max=102f)
            text(team.points,135f,145f,32f,accent,true,95f); label("POINTS",135f,164f)
            val participants = if(d.racing.drivers.value.season==table.season && d.racing.drivers.sample==s.sample) d.racing.drivers.value.entries.filter { team.id in it.teams }.joinToString("  /  "){it.code} else "Pairings unavailable"
            label(participants,24f,h-34f,ink)
            footer(freshness(s,table.season))
        }
        fun standings() {
            val drivers=cfg.kind==WidgetKind.DRIVER_STANDINGS
            val table = if(drivers) d.racing.drivers.value else d.racing.teams.value
            val section = if(drivers) d.racing.drivers else d.racing.teams
            label("${table.season} / ROUND ${table.round}",color=accent)
            text(if(drivers) "THE DRIVERS." else "THE TEAMS.",24f,65f,32f,heavy=true)
            val rows=cfg.rows.coerceAtMost(table.entries.size)
            if(rows==0) text("Standings not published",24f,125f,21f,max=370f)
            val step=min(39f,(h-112f)/rows.coerceAtLeast(1))
            table.entries.take(rows).forEachIndexed { i, e ->
                val pos: Int; val title: String; val points: String; val team: String
                if(e is Competitor) { pos=e.position; title=e.code+"  "+e.family; points=e.points; team=e.teams.firstOrNull().orEmpty() }
                else { e as Team; pos=e.position; title=e.name; points=e.points; team=e.id }
                val y=89f+i*step
                line(24f,y-8f,396f,y-8f,Color.argb(45,128,128,128))
                text(pos.toString().padStart(2,'0'),24f,y+step*.46f,min(17f,step*.65f),muted,max=33f)
                rect(64f,y-2f,3f,step*.57f,Color.parseColor(teamColor(team)))
                text(title,80f,y+step*.46f,min(18f,step*.66f),heavy=true,max=228f)
                text(points,326f,y+step*.46f,min(19f,step*.66f),ink,true,72f)
            }
            footer(freshness(section,table.season))
        }
        fun weekend() {
            val s=d.racing.schedule
            val race=if(s.sample) s.value.firstOrNull() else nextRace(s.value,d.now)
            if(race==null) { label("RACE WEEKEND"); text(if(s.value.isEmpty()) "Calendar unavailable" else "Calendar complete",24f,108f,30f,heavy=true); text("Refresh for the next published season.",24f,139f,15f,muted); footer(freshness(s)); return }
            label("ROUND ${race.round.toString().padStart(2,'0')} / ${race.season}")
            rect(392f,22f,4f,16f,accent)
            text(race.location.ifBlank { race.name.removeSuffix(" Grand Prix") },24f,74f,38f,heavy=true)
            label(race.country,24f,94f)
            circuit(race,251f,91f,142f,52f)
            val sessions = race.sessions
            val step=min(32f,(h-182f)/sessions.size.coerceAtLeast(1))
            sessions.forEachIndexed { i,session ->
                val y=156f+i*step
                text(session.name,24f,y,min(19f,step*.69f),heavy=session.name=="Race",max=156f)
                val zoned=session.start?.atZone(cfg.zone())
                text(zoned?.format(DateTimeFormatter.ofPattern("EEE",Locale.getDefault()))?.uppercase() ?: "—",220f,y,min(16f,step*.65f),muted,max=60f)
                text(zoned?.format(DateTimeFormatter.ofPattern(if(cfg.hour24) "HH:mm" else "h:mm a",Locale.getDefault())) ?: "TBA",286f,y,min(20f,step*.69f),max=110f)
            }
            label(cfg.zone().id,24f,h-30f,muted)
            footer(freshness(s,race.season))
        }
        fun circuit(race: Race,x: Float,y: Float,w: Float,height: Float) {
            if(race.circuitId!="monaco") { label(race.circuit,x,y+20f,muted,w); return }
            // Simplified, original outline; decorative, never used for telemetry.
            val points=floatArrayOf(.02f,.80f,.12f,.41f,.28f,.05f,.46f,.35f,.53f,.46f,.64f,.57f,.72f,.51f,.75f,.29f,.95f,.21f,.91f,.44f,.99f,.46f,.92f,.67f,.73f,.79f,.58f,.73f,.45f,.61f,.32f,.35f,.18f,.57f,.10f,.90f,.02f,.80f)
            val path=Path(); points.asList().chunked(2).forEachIndexed { i, a -> if(i==0)path.moveTo(x+a[0]*w,y+a[1]*height) else path.lineTo(x+a[0]*w,y+a[1]*height) }
            p.color=muted;p.style=Paint.Style.STROKE;p.strokeWidth=2f;p.strokeJoin=Paint.Join.ROUND;c.drawPath(path,p);p.style=Paint.Style.FILL
        }
        fun countdown() {
            val s=d.racing.schedule;val race=if(s.sample) s.value.firstOrNull() else nextRace(s.value,d.now)
            label("NEXT LIGHTS OUT",color=accent)
            if(race==null) { text("Season complete",24f,100f,31f,heavy=true); footer(freshness(s)); return }
            text(race.name.removeSuffix(" Grand Prix").uppercase(),24f,71f,32f,heavy=true)
            val duration=race.start?.let { Duration.between(d.now,it) }
            val future=duration!=null && !duration.isNegative
            if(future) {
                text("${duration!!.toDays().toString().padStart(2,'0')}",24f,161f,77f,heavy=true,max=172f)
                text("${(duration.toHours()%24).toString().padStart(2,'0')}",232f,161f,77f,accent,true,164f)
                label("DAYS",28f,183f);label("HOURS",236f,183f)
            } else { text(if(s.sample) "ARCHIVE / 2024" else "TIME TBA",24f,142f,38f,heavy=true) }
            label("R${race.round} · ${race.start?.atZone(cfg.zone())?.format(DateTimeFormatter.ofPattern("dd MMM HH:mm",Locale.getDefault())) ?: race.date} · ${cfg.zone().id}",24f,h-34f)
            footer(freshness(s,race.season))
        }
        fun results() {
            val s=d.racing.result; val result=s.value
            label("LAST CHEQUERED FLAG",color=accent)
            if(result==null || result.finishers.isEmpty()) { text("No results published",24f,110f,28f,heavy=true);footer(freshness(s));return }
            text(result.race.name.removeSuffix(" Grand Prix").uppercase(),24f,65f,31f,heavy=true)
            label("${result.race.date} / FINAL CLASSIFICATION",24f,87f)
            val rows=result.finishers.take(cfg.rows); val step=min(34f,(h-132f)/rows.size.coerceAtLeast(1))
            rows.forEachIndexed { i,f -> val y=113f+i*step
                rect(24f,y-9f,3f,step*.65f,Color.parseColor(teamColor(f.team)))
                text(f.position,39f,y+10f,18f,muted,max=32f)
                text(f.code,85f,y+10f,22f,heavy=true,max=64f)
                text(f.name.substringAfter(' '),166f,y+9f,17f,max=217f)
            }
            footer(freshness(s,result.race.season))
        }
        fun controls(y: Float, color: Int = ink) {
            if(!preview) return
            val media=d.media
            if(cfg.kind!=WidgetKind.COMPACT) text("│◀",224f,y,24f,if(media.previous)color else muted,max=44f)
            text(if(media.playing) "Ⅱ" else "▶",285f,y,27f,if(media.toggle)color else muted,max=44f)
            text("▶│",350f,y,24f,if(media.next)color else muted,max=44f)
        }
        fun music() {
            val m=d.media
            val title=if(!d.mediaAccess) "Connect your player" else m.title
            val artist=if(!d.mediaAccess) "Enable media access in Widget Lab" else m.artist
            when(cfg.kind) {
                WidgetKind.EDITORIAL -> {
                    photo(m.artwork,232f,0f,188f,h-56f)
                    label(if(m.playing) "ON ROTATION" else "LISTENING ROOM",color=accent,max=198f)
                    titleLines(title,24f,83f,30f,190f)
                    text(artist,24f,143f,14f,muted,max=184f)
                    label("STEREO / ${if(m.playing) "PLAYING" else "PAUSED"}",24f,h-78f,max=190f)
                    line(24f,h-56f,396f,h-56f)
                    label("W / L",24f,h-24f); controls(h-20f)
                }
                WidgetKind.GLASS -> {
                    if(m.artwork!=null) photo(m.artwork,0f,0f,420f,h,20f) else { circle(320f,40f,180f,Color.rgb(61,91,88)); circle(40f,180f,130f,Color.rgb(63,70,95)) }
                    rect(0f,0f,420f,h,Color.argb(175,12,19,25),20f)
                    rect(16f,16f,388f,h-32f,Color.argb(38,255,255,255),18f)
                    line(36f,17f,380f,17f,Color.argb(95,255,255,255))
                    rect(32f,h-58f,40f,2f,accent)
                    photo(m.artwork,32f,32f,92f,92f,12f)
                    text(title,143f,66f,25f,Color.WHITE,true,240f)
                    text(artist,143f,92f,14f,Color.LTGRAY,max=235f)
                    label(if(m.playing) "NOW PLAYING" else "SOFT GLASS / STILL",32f,h-79f,Color.LTGRAY)
                    controls(h-20f,Color.WHITE)
                }
                WidgetKind.VINYL -> {
                    val cx=201f;val cy=133f
                    circle(cx,cy,111f,Color.rgb(20,21,21))
                    for(i in 0..12) circle(cx,cy,65f+i*3.2f,Color.rgb(39,40,40),.7f)
                    c.save(); c.clipPath(Path().apply{addCircle(cx,cy,61f,Path.Direction.CW)});photo(m.artwork,cx-61f,cy-61f,122f,122f);c.restore()
                    circle(cx,cy,5f,paper)
                    rect(24f,h-89f,32f,2f,accent)
                    line(353f,46f,323f,206f,muted,5f);line(323f,206f,302f,225f,ink,10f);rect(341f,33f,21f,32f,ink,3f)
                    label(artist,24f,h-70f,max=365f)
                    text(title,24f,h-42f,25f,heavy=true,max=368f)
                    controls(h-9f)
                }
                WidgetKind.BRUTALIST -> {
                    rect(0f,0f,10f,h,accent)
                    label("AUDIO / ${if(m.playing) "IN MOTION" else "STANDBY"}")
                    titleLines(title.uppercase(),24f,88f,43f,373f)
                    line(24f,153f,396f,153f,ink,3f)
                    text(artist.uppercase(),24f,177f,14f,heavy=true)
                    label("VOL. 01 / WIDGET LAB",24f,h-23f,max=172f)
                    controls(h-20f)
                }
                WidgetKind.MATRIX -> {
                    for(y in 16..145 step 7) for(x in 18..400 step 7) circle(x.toFloat(),y.toFloat(),.8f,Color.argb(40,128,150,128))
                    label("● ${if(m.playing) "SIGNAL ACTIVE" else "AWAITING SIGNAL"}",color=accent)
                    text(title.uppercase(),24f,88f,27f,accent,true,370f,mono)
                    text(artist,24f,119f,14f,muted,max=370f,face=mono)
                    line(24f,h-57f,396f,h-57f)
                    label("DIGITAL / STEREO",24f,h-23f)
                    controls(h-20f)
                }
                WidgetKind.CASSETTE -> {
                    rect(18f,18f,384f,h-76f,if(cfg.light) Color.rgb(215,196,162) else Color.rgb(93,84,66),12f)
                    rect(33f,32f,354f,61f,Color.rgb(238,225,194),4f)
                    label("SIDE A / WIDGET LAB",43f,49f,Color.DKGRAY)
                    text(title,43f,77f,22f,Color.BLACK,true,329f)
                    val y=146f
                    rect(64f,y-31f,292f,60f,Color.rgb(35,36,33),28f)
                    listOf(102f,318f).forEach { x -> circle(x,y,24f,Color.rgb(217,210,188));circle(x,y,10f,Color.rgb(64,63,54));for(i in 0..5) {val a=i*PI/3;line(x+cos(a).toFloat()*12,y+sin(a).toFloat()*12,x+cos(a).toFloat()*19,y+sin(a).toFloat()*19,Color.rgb(64,63,54),3f)} }
                    text(artist,40f,h-91f,13f,Color.rgb(240,232,213),max=342f)
                    label("HI-FI / TYPE I",24f,h-23f)
                    rect(24f,h-49f,40f,2f,accent)
                    controls(h-20f)
                }
                WidgetKind.COMPACT -> {
                    photo(m.artwork,12f,12f,86f,86f,9f)
                    text(title,112f,40f,18f,heavy=true,max=130f)
                    text(artist,112f,65f,12f,muted,max=130f)
                    rect(112f,75f,32f,2f,accent)
                    controls(h-20f)
                }
                else -> Unit
            }
        }
        fun clock() {
            label(if(cfg.kind==WidgetKind.SWISS) "SWISS / TIME STUDY 01" else "LOCAL TIME / ${cfg.zone().id}",color=if(cfg.light)muted else accent)
            if(cfg.kind==WidgetKind.SWISS) { rect(375f,20f,20f,20f,accent);line(24f,h-44f,396f,h-44f,ink,2f) }
            if(preview) {
                val time=d.now.atZone(cfg.zone())
                text(time.format(DateTimeFormatter.ofPattern(if(cfg.hour24) "HH:mm" else "h:mm",Locale.getDefault())),24f,148f,88f,heavy=true)
                text(time.format(DateTimeFormatter.ofPattern(if(cfg.hour24) "EEEE, dd MMMM" else "a · EEEE, dd MMMM",Locale.getDefault())),26f,h-20f,16f,muted)
            }
        }
        fun date() {
            label("THE EVERYDAY / ${cfg.zone().id}")
            rect(378f,21f,17f,17f,accent)
            if(preview) {
                val date=d.now.atZone(cfg.zone())
                text(date.format(DateTimeFormatter.ofPattern("dd")),24f,143f,106f,heavy=true,max=190f)
                text(date.format(DateTimeFormatter.ofPattern("MMM",Locale.getDefault())).uppercase(),219f,100f,35f,heavy=true,max=180f)
                text(date.format(DateTimeFormatter.ofPattern("EEEE · yyyy",Locale.getDefault())),220f,128f,15f,muted,max=178f)
            }
            line(24f,h-43f,396f,h-43f)
            footer("DAY / MONTH / YEAR")
        }
        fun battery() {
            label("ENERGY / DEVICE")
            text(d.battery.percent?.let { "$it" } ?: "—",24f,137f,101f,heavy=true,max=235f)
            text("%",260f,134f,50f,muted,max=74f)
            label(if(d.battery.charging) "CONNECTED / CHARGING" else "ON BATTERY",24f,h-39f)
            rect(24f,h-28f,372f,4f,muted,2f)
            rect(24f,h-28f,372f*(d.battery.percent?:0)/100f,4f,accent,2f)
            footer("SYSTEM READING · PERIODIC REFRESH")
        }
        fun agenda() {
            label("PRIVATE / NEXT SEVEN DAYS",color=accent)
            text("On the agenda.",24f,67f,32f,heavy=true)
            if(!d.agenda.permitted) {
                text("Your calendar, on your terms.",24f,132f,20f,heavy=true)
                text("Tap to grant read-only calendar access.",24f,161f,15f,muted)
            } else if(d.agenda.error) { text("Calendar unavailable",24f,132f,23f,heavy=true);text("Tap to review access and refresh.",24f,160f,15f,muted) }
            else if(d.agenda.events.isEmpty()) { text("Room to breathe.",24f,136f,28f,heavy=true);text("No upcoming events in the next seven days.",24f,164f,14f,muted) }
            else d.agenda.events.take(4).forEachIndexed { i,e ->
                val y=106f+i*41f
                line(24f,y-14f,396f,y-14f,Color.argb(40,128,128,128))
                val at=e.begin.atZone(if(e.allDay) java.time.ZoneOffset.UTC else cfg.zone())
                text(at.format(DateTimeFormatter.ofPattern("EEE dd",Locale.getDefault())),24f,y+5f,12f,muted,max=68f)
                text(if(e.allDay) "ALL DAY" else at.format(DateTimeFormatter.ofPattern(if(cfg.hour24) "HH:mm" else "h:mm a",Locale.getDefault())),24f,y+22f,12f,max=70f)
                rect(106f,y-6f,3f,27f,e.color)
                text(e.title,123f,y+13f,18f,heavy=true,max=272f)
            }
            footer("ON DEVICE / ${cfg.zone().id}")
        }
    }
}
