package dev.widgetlab.ui

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.widgetlab.R
import dev.widgetlab.core.*
import dev.widgetlab.core.Collection
import dev.widgetlab.data.*
import dev.widgetlab.media.MediaHub
import dev.widgetlab.media.MediaInfo
import dev.widgetlab.media.MediaListener
import dev.widgetlab.widgets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

open class MainActivity: ComponentActivity() {
    open val configuring=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if(configuring) setResult(Activity.RESULT_CANCELED)
        val id=if(configuring) intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1) else -1
        val kind=if(id!=-1) ProviderCatalog.kind(this,id) else null
        if(configuring && kind==null) {finish();return}
        setContent { LabTheme { LabApp(this,id,kind) } }
    }
    override fun onResume() { super.onResume();MediaHub.connect(this);WidgetUpdates.request(this) }
    fun mediaSettings() {
        val intent=if(android.os.Build.VERSION.SDK_INT>=30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,ComponentName(this,MediaListener::class.java).flattenToString()) else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }
}
class WidgetConfigureActivity: MainActivity() { override val configuring=true }

private val Lime=Color(0xFFD8FF62)
private val Ground=Color(0xFF101112)
private val Soft=Color(0xFF989D9C)
private val White=Color(0xFFF4F2E9)
private val Grotesk=FontFamily(Font(R.font.space_grotesk))
@Composable fun LabTheme(content: @Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Lime,onPrimary=Ground,background=Ground,surface=Color(0xFF1C1E20),onSurface=White,onBackground=White,outline=Color(0xFF454A48)), typography=Typography().let { it.copy(bodyLarge=it.bodyLarge.copy(fontFamily=Grotesk),bodyMedium=it.bodyMedium.copy(fontFamily=Grotesk),titleLarge=it.titleLarge.copy(fontFamily=Grotesk),labelLarge=it.labelLarge.copy(fontFamily=Grotesk)) },content=content)
}

@Composable private fun LabApp(activity: MainActivity,initialId: Int,initialKind: WidgetKind?) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember {ConfigStore(context)}
    var screen by rememberSaveable {mutableStateOf(if(initialKind!=null) "detail" else "home")}
    var editingId by rememberSaveable {mutableIntStateOf(initialId)}
    var profile by rememberSaveable {mutableStateOf(initialKind?.let {kind ->
        (if(store.contains(initialId)) store.read(initialId,kind) else store.pendingConfig(kind) ?: WidgetConfig(kind)).json()
    }.orEmpty())}
    var collectionName by rememberSaveable {mutableStateOf(Collection.RACING.name)}
    var version by remember {mutableIntStateOf(0)}
    var data by remember {mutableStateOf<RenderData?>(null)}
    var installed by remember {mutableStateOf(ProviderCatalog.installed(context))}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    var manualHelp by remember {mutableStateOf(false)}
    var recent by remember {mutableStateOf(store.recent())}
    val automatic by remember {AppPreferences(context).automatic}.collectAsState(initial=true)
    // A lightweight revision is observed so metadata changes redraw open previews immediately.
    val mediaRevision by MediaHub.revision.collectAsState()
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) version++ }
        owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(version,mediaRevision) {
        data=withContext(Dispatchers.IO) { RenderData(RacingRepository(context).snapshot(),MediaHub.info(),DeviceData.battery(context),DeviceData.agenda(context),MediaHub.permitted(context)) }
        installed=ProviderCatalog.installed(context);recent=store.recent()
    }
    LaunchedEffect(Unit) { while(true) {delay(60_000);version++} }
    val config=WidgetConfig.parse(profile,WidgetKind.CLOCK)
    val currentData=data?.copy(media=MediaHub.info(config.selection))
    fun choose(k: WidgetKind) {editingId=-1;profile=WidgetConfig(k).json();screen="detail"}
    fun back() {if(activity.configuring) activity.finish() else {screen="home";editingId=-1}}
    BackHandler(screen!="home" || activity.configuring) {back()}
    val calendar=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->version++;WidgetUpdates.request(context);if(!granted)message="Calendar access is off. The agenda will show its permission state."}
    val artwork=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            val previous=profile
            scope.launch {
                busy=true
                runCatching {withContext(Dispatchers.IO){importArtwork(context,uri)}}
                    .onSuccess {name-> if(profile==previous)profile=config.copy(image=name).json();message="Artwork saved privately on this device."}
                    .onFailure {message="Could not import this image. Choose a JPEG, PNG or WebP image."}
                busy=false
            }
        }
    }
    fun refresh() {scope.launch {busy=true;val errors=RacingRepository(context).refresh();WidgetUpdates.updateAll(context);version++;busy=false;message=if(errors.isEmpty())"Racing feeds refreshed." else errors.joinToString("\n")}}
    fun save() {
        store.remember(config);recent=store.recent()
        if(editingId>=0) {
            store.save(editingId,config)
            scope.launch {WidgetUpdates.updateAll(context)}
            if(activity.configuring) {
                activity.setResult(Activity.RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,editingId));activity.finish()
            } else {message="Widget updated.";screen="installed";editingId=-1}
        } else {
            val manager=AppWidgetManager.getInstance(context)
            if(manager.isRequestPinAppWidgetSupported) {
                val token=store.pending(config)
                val callback=Intent(context,PinReceiver::class.java).setData(Uri.parse("widgetlab://pin/$token")).putExtra("token",token)
                // The launcher must fill in EXTRA_APPWIDGET_ID. Explicit component bounds the mutable callback.
                val pending=PendingIntent.getBroadcast(context,token.hashCode(),callback,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                val ok=manager.requestPinAppWidget(ProviderCatalog.component(context,config.kind),null,pending)
                if(!ok)manualHelp=true else message="Confirm placement in your launcher. Your saved style is applied after pinning."
            } else manualHelp=true
        }
    }
    Surface(Modifier.fillMaxSize(),color=Ground) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(if(screen=="home") "W/L" else "←",color=Lime,fontSize=25.sp,fontWeight=FontWeight.Bold,fontFamily=Grotesk,modifier=Modifier.clickable{if(screen!="home")back()}.padding(end=13.dp))
                Text("WIDGET LAB",fontFamily=Grotesk,fontSize=14.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp,modifier=Modifier.weight(1f))
                TextButton(onClick={screen=if(screen=="settings")"home" else "settings"}) {Text(if(screen=="settings")"CLOSE" else "SETTINGS",fontSize=10.sp,letterSpacing=1.sp)}
            }
            HorizontalDivider(color=Color(0xFF303332))
            when(screen) {
                "home" -> Home(data,installed.size,recent,collectionName,{collectionName=it.name},::choose,{c->editingId=-1;profile=c.json();screen="detail"},{screen="installed"})
                "detail" -> Detail(config,currentData,editingId,busy,{profile=it.json()},::save,{activity.mediaSettings()},{calendar.launch(Manifest.permission.READ_CALENDAR)},{artwork.launch(arrayOf("image/*"))})
                "installed" -> Installed(installed,store,data) {id,k->editingId=id;profile=store.read(id,k).json();screen="detail"}
                "settings" -> SettingsScreen(data,automatic,busy,{value->scope.launch{AppPreferences(context).automatic(value);RefreshScheduling.racing(context,value)}},::refresh,{activity.mediaSettings()},{scope.launch{WidgetUpdates.updateAll(context);version++;message="Installed widgets refreshed."}},{runCatching{activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))}})
            }
        }
    }
    if(message!=null) AlertDialog(onDismissRequest={message=null},title={Text("Widget Lab")},text={Text(message.orEmpty())},confirmButton={TextButton(onClick={message=null}){Text("GOT IT")}})
    if(manualHelp) AlertDialog(onDismissRequest={manualHelp=false},title={Text("Add through your launcher")},text={Text("Long-press an empty home-screen area → Widgets → Widget Lab → ${config.kind.title}. Place the widget, then choose its settings. Layout sizes depend on your launcher grid.")},confirmButton={TextButton(onClick={manualHelp=false}){Text("GOT IT")}})
}

private suspend fun importArtwork(context: android.content.Context,uri: Uri): String = withContext(Dispatchers.IO) {
    val bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver,uri)) { decoder,info,_ ->
        check(info.size.width in 1..30000 && info.size.height in 1..30000)
        val scale=1024f/maxOf(info.size.width,info.size.height).coerceAtLeast(1024)
        decoder.setTargetSize((info.size.width*scale).toInt().coerceAtLeast(1),(info.size.height*scale).toInt().coerceAtLeast(1))
        decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val name="${UUID.randomUUID()}.jpg"
    val file=File(context.filesDir,"art/$name").apply{parentFile?.mkdirs()}
    file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,90,it))}
    bitmap.recycle();name
}

@Composable private fun Eyebrow(text: String,modifier: Modifier=Modifier) {Text(text.uppercase(),modifier,fontSize=10.sp,fontFamily=Grotesk,letterSpacing=1.8.sp,color=Soft)}
@Composable private fun Heading(text: String,size: Int=34) {Text(text,fontFamily=Grotesk,fontSize=size.sp,lineHeight=(size+3).sp,fontWeight=FontWeight.Bold,color=White)}
@Composable private fun Rule() {HorizontalDivider(Modifier.padding(vertical=20.dp),color=Color(0xFF303332))}

@Composable private fun Preview(config: WidgetConfig,data: RenderData?,modifier: Modifier=Modifier) {
    val context=LocalContext.current
    val bitmap by produceState<Bitmap?>(null,config.json(),data) {
        if(data!=null) value=withContext(Dispatchers.Default){WidgetRenderer(context).render(config,data.copy(media=MediaHub.info(config.selection)),width=630)}
    }
    Box(modifier.fillMaxWidth().aspectRatio(420f/config.kind.height).clip(RoundedCornerShape(18.dp)).background(Color(0xFF202321))) {
        bitmap?.let {Image(it.asImageBitmap(),contentDescription="${config.kind.title} preview",modifier=Modifier.fillMaxSize())}
    }
}

@Composable private fun Home(data: RenderData?,installed: Int,recent: List<WidgetConfig>,collectionName: String,onCollection: (Collection)->Unit,onKind: (WidgetKind)->Unit,onRecent: (WidgetConfig)->Unit,onInstalled: ()->Unit) {
    val collection=Collection.valueOf(collectionName)
    LazyColumn(contentPadding=PaddingValues(bottom=32.dp)) {
        item {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Eyebrow("A personal collection",Modifier.weight(1f))
                    Text("$installed INSTALLED ↗",color=Lime,fontSize=10.sp,fontFamily=Grotesk,modifier=Modifier.clickable(onClick=onInstalled).padding(8.dp))
                }
                Spacer(Modifier.height(17.dp))
                Heading("Objects for\nyour everyday.",42)
                Spacer(Modifier.height(13.dp))
                Text("Native widgets. Carefully composed.",color=Soft,fontSize=14.sp)
                Spacer(Modifier.height(29.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("01 / In the spotlight");Eyebrow("Racing")}
                Spacer(Modifier.height(13.dp))
                Preview(WidgetConfig(WidgetKind.WEEKEND),data,Modifier.clickable{onKind(WidgetKind.WEEKEND)})
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Race weekend",fontFamily=Grotesk,fontWeight=FontWeight.Bold);Text("EXPLORE ↗",fontSize=11.sp,color=Lime,modifier=Modifier.clickable{onKind(WidgetKind.WEEKEND)})}
                if(data?.racing?.schedule?.sample==true) {Spacer(Modifier.height(6.dp));Text("Labeled historical sample · Refresh racing in Settings for current data.",color=Soft,fontSize=11.sp)}
                if(recent.isNotEmpty()) {
                    Rule();Eyebrow("Your recent styles")
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        recent.forEach{c->OutlinedButton(onClick={onRecent(c)}){Text(c.kind.title,fontSize=11.sp)}}
                    }
                }
                Rule()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Heading("The collections.",29);Eyebrow("19 DESIGNS",Modifier.padding(top=12.dp))}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=22.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                Collection.entries.forEach { col ->
                    Column(Modifier.clickable{onCollection(col)}) {
                        Text(col.title,color=if(col==collection)Lime else Soft,fontFamily=Grotesk,fontSize=14.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(12.dp));Box(Modifier.height(2.dp).width(if(col==collection)34.dp else 0.dp).background(Lime))
                    }
                }
            }
            HorizontalDivider(color=Color(0xFF303332))
            Text(collection.subtitle,Modifier.padding(22.dp),color=Soft,fontSize=13.sp)
        }
        val widgets=WidgetKind.entries.filter{it.collection==collection}
        items(widgets.size,key={widgets[it].name}) { index ->
            val k=widgets[index]
            Column(Modifier.padding(horizontal=22.dp).padding(bottom=28.dp).clickable{onKind(k)}) {
                Preview(WidgetConfig(k),data)
                Row(Modifier.fillMaxWidth().padding(top=12.dp),verticalAlignment=Alignment.Top) {
                    Eyebrow((index+1).toString().padStart(2,'0'),Modifier.width(32.dp).padding(top=4.dp))
                    Column(Modifier.weight(1f)) {Text(k.title,fontFamily=Grotesk,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(k.size+" · "+if(k.isMusic)"MEDIA SESSION" else if(k.isRacing)"CACHED RACING" else "ON DEVICE",fontSize=10.sp,color=Soft,modifier=Modifier.padding(top=4.dp))}
                    Text("↗",color=Lime,fontSize=25.sp)
                }
            }
        }
        item {Column(Modifier.padding(horizontal=22.dp)){Rule();Eyebrow("Recently added / 001—019");Text("Swiss / time study, Tape study and Race weekend are part of the first collection.",color=Soft,fontSize=12.sp,modifier=Modifier.padding(top=10.dp));Spacer(Modifier.height(22.dp));Text("MAKE YOUR HOME SCREEN YOURS.",color=Lime,fontFamily=Grotesk,fontSize=11.sp,letterSpacing=2.sp)}}
    }
}

@Composable private fun Detail(cfg: WidgetConfig,data: RenderData?,id: Int,busy: Boolean,onChange: (WidgetConfig)->Unit,onSave: ()->Unit,onMedia: ()->Unit,onCalendar: ()->Unit,onArtwork: ()->Unit) {
    var picker by remember {mutableStateOf<String?>(null)}
    val context=LocalContext.current
    LazyColumn(contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item {Eyebrow("${cfg.kind.collection.title} / ${cfg.kind.size}")}
        item {Heading(cfg.kind.title,35)}
        item {Preview(cfg,data)}
        item {Text(cfg.kind.description,color=Soft,fontSize=14.sp,lineHeight=21.sp)}
        item {
            Rule();Eyebrow("Make it yours")
            Spacer(Modifier.height(16.dp))
            Text("Surface",fontSize=14.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                FilterChip(selected=!cfg.light,onClick={onChange(cfg.copy(light=false))},label={Text("Ink")})
                FilterChip(selected=cfg.light,onClick={onChange(cfg.copy(light=true))},label={Text("Paper")})
            }
            Text("Accent",fontSize=14.sp,modifier=Modifier.padding(top=12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                listOf("#D8FF62","#48DCC4","#F04C48","#FFA24C","#7898FF","#F295CE","#EDE9DC").forEach { hex ->
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).border(if(cfg.accent==hex)3.dp else 0.dp,White,CircleShape).clickable{onChange(cfg.copy(accent=hex))})
                }
            }
            if(!cfg.kind.isClock && cfg.kind!=WidgetKind.DATE) {
                Spacer(Modifier.height(19.dp));Text("Typography",fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected=cfg.typography=="editorial",onClick={onChange(cfg.copy(typography="editorial"))},label={Text("Editorial")})
                    FilterChip(selected=cfg.typography=="mono",onClick={onChange(cfg.copy(typography="mono"))},label={Text("Monospace")})
                }
            }
        }
        if(cfg.kind==WidgetKind.DRIVER || cfg.kind==WidgetKind.CONSTRUCTOR) {
            item {
                val entries=if(cfg.kind==WidgetKind.DRIVER) data?.racing?.drivers?.value?.entries?.map{it.id to it.name} else data?.racing?.teams?.value?.entries?.map{it.id to it.name}
                OptionRow(if(cfg.kind==WidgetKind.DRIVER)"Driver" else "Constructor",entries?.firstOrNull{it.first==cfg.selection}?.second ?: if(cfg.selection.isBlank())"First in cached standings" else "Selection unavailable") {picker="competitor"}
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick=onArtwork,enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(cfg.image.isBlank()) "Add personal ${if(cfg.kind==WidgetKind.DRIVER)"portrait" else "car artwork"}" else "Replace artwork")}
                Text("Use an image you have permission to use. Stored privately; never uploaded.",color=Soft,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                if(cfg.image.isNotBlank()) {
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected=cfg.crop=="center",onClick={onChange(cfg.copy(crop="center"))},label={Text("Center crop")})
                        FilterChip(selected=cfg.crop=="top",onClick={onChange(cfg.copy(crop="top"))},label={Text("Top crop")})
                        TextButton(onClick={onChange(cfg.copy(image=""))}){Text("Remove",fontSize=11.sp)}
                    }
                }
            }
        }
        if(cfg.kind in listOf(WidgetKind.DRIVER_STANDINGS,WidgetKind.CONSTRUCTOR_STANDINGS,WidgetKind.RESULTS)) {
            item {Text("Leaderboard rows",fontSize=14.sp);Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf(3,5,10).forEach {n->FilterChip(selected=cfg.rows==n,onClick={onChange(cfg.copy(rows=n))},label={Text("$n")})}}}
        }
        if(cfg.kind in listOf(WidgetKind.CLOCK,WidgetKind.SWISS,WidgetKind.DATE,WidgetKind.WEEKEND,WidgetKind.COUNTDOWN,WidgetKind.AGENDA)) {
            item {
                OptionRow("Timezone",if(cfg.timezone=="device")"Device timezone" else cfg.timezone){picker="timezone"}
                if(cfg.kind!=WidgetKind.DATE) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected=cfg.hour24,onClick={onChange(cfg.copy(hour24=true))},label={Text("24 hour")})
                    FilterChip(selected=!cfg.hour24,onClick={onChange(cfg.copy(hour24=false))},label={Text("12 hour")})
                }
            }
        }
        if(cfg.kind.isMusic) {
            item {
                Rule();Eyebrow("Media connection")
                Spacer(Modifier.height(10.dp))
                Text(if(data?.mediaAccess==true)"Media access enabled" else "Media access required",fontWeight=FontWeight.Bold)
                Text("Android notification-listener access lets Widget Lab discover compatible media sessions. It does not store notification messages. Playback controls depend on the player’s published capabilities.",color=Soft,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(vertical=10.dp))
                OutlinedButton(onClick=onMedia){Text(if(data?.mediaAccess==true)"Review media access" else "Connect music player")}
                if(data?.mediaAccess==true) {OptionRow("Player",if(cfg.selection.isBlank())"Automatic · playing session first" else appLabel(context,cfg.selection)){picker="media"};Text("Start playback in your music app to make its session available.",color=Soft,fontSize=11.sp)}
            }
        }
        if(cfg.kind==WidgetKind.AGENDA) {
            item {Rule();Eyebrow("Device calendar");Text("Calendar details stay on this device. Upcoming events refresh periodically; tapping the widget opens its settings.",color=Soft,fontSize=12.sp,modifier=Modifier.padding(vertical=12.dp));OutlinedButton(onClick=onCalendar){Text(if(data?.agenda?.permitted==true)"Calendar access enabled" else "Allow read-only calendar access")}}
        }
        item {
            Rule();Eyebrow("Source & behavior")
            val source=when {
                cfg.kind.isRacing -> {
                    val section=when(cfg.kind) {WidgetKind.CONSTRUCTOR,WidgetKind.CONSTRUCTOR_STANDINGS->data?.racing?.teams;WidgetKind.WEEKEND,WidgetKind.COUNTDOWN->data?.racing?.schedule;WidgetKind.RESULTS->data?.racing?.result;else->data?.racing?.drivers}
                    if(section?.sample!=false) "Historical sample / Monaco 2024. This is not live information. Refresh the public Jolpica feeds in Settings."
                    else "Jolpica / cached public Formula 1 data. Last successful refresh: ${section.refreshed?.atZone(cfg.zone())?.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z"))}. ${section.error?.let{"Refresh failed; previous data retained."}.orEmpty()}"
                }
                cfg.kind.isMusic -> "Active Android media sessions. Updated when the player publishes metadata or playback changes. No invented playback progress and no continuous widget animation."
                cfg.kind.isClock || cfg.kind==WidgetKind.DATE -> "Native Android TextClock. Time and date follow the selected IANA timezone."
                cfg.kind==WidgetKind.BATTERY -> "Android battery status. Refreshed on supported power events and roughly every 30 minutes. Android and Vivo power management may delay periodic work."
                else -> "Read-only Android device calendar. Roughly 30-minute periodic updates; no calendar data leaves this device."
            }
            Text(source,color=Soft,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(top=10.dp))
            if(cfg.kind.isRacing) Text("Automatic data sync: every six hours when enabled. Offline data remains labeled with its season and timestamp. Countdown values refresh periodically.",color=Soft,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))
            Spacer(Modifier.height(23.dp))
            Button(onClick=onSave,enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(10.dp)){Text(if(id>=0)"Save widget" else "Add to Home Screen  ↗",fontFamily=Grotesk,fontWeight=FontWeight.Bold)}
            Text("Resize on your home screen. Long-press → Edit widget to return to these settings, where supported by your launcher.",color=Soft,fontSize=11.sp,lineHeight=17.sp,modifier=Modifier.padding(top=10.dp,bottom=22.dp))
        }
    }
    if(picker!=null) {
        val choices: List<Pair<String,String>> = when(picker) {
            "competitor" -> listOf("" to "First in cached standings")+(if(cfg.kind==WidgetKind.DRIVER)data?.racing?.drivers?.value?.entries?.map{it.id to "${it.name} / ${data.racing.drivers.value.season}"} else data?.racing?.teams?.value?.entries?.map{it.id to "${it.name} / ${data.racing.teams.value.season}"}).orEmpty()
            "media" -> listOf("" to "Automatic")+MediaHub.apps().map{it to appLabel(context,it)}
            else -> listOf("device" to "Device timezone")+listOf("UTC","Europe/London","Europe/Paris","Europe/Monaco","America/New_York","America/Los_Angeles","Asia/Kolkata","Asia/Singapore","Asia/Tokyo","Australia/Sydney").map{it to it}
        }
        AlertDialog(onDismissRequest={picker=null},title={Text(if(picker=="timezone")"Timezone" else if(picker=="media")"Music player" else "Season selection")},text={LazyColumn{items(choices.size){i->Text(choices[i].second,Modifier.fillMaxWidth().clickable{if(picker=="timezone")onChange(cfg.copy(timezone=choices[i].first)) else onChange(cfg.copy(selection=choices[i].first));picker=null}.padding(vertical=14.dp),fontSize=14.sp)}}},confirmButton={TextButton(onClick={picker=null}){Text("CLOSE")}})
    }
}

private fun appLabel(context: android.content.Context,pkg: String): String = runCatching {context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg,0)).toString()}.getOrDefault(pkg)
@Composable private fun OptionRow(title: String,value: String,onClick: ()->Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=12.dp)) {
        Eyebrow(title);Spacer(Modifier.height(6.dp));Row {Text(value,fontSize=15.sp,modifier=Modifier.weight(1f));Text("↗",color=Lime)}
    }
}
@Composable private fun Installed(installed: List<Pair<Int,WidgetKind>>,store: ConfigStore,data: RenderData?,onEdit: (Int,WidgetKind)->Unit) {
    LazyColumn(contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {Eyebrow("YOUR HOME SCREEN");Spacer(Modifier.height(14.dp));Heading("In place.",42);Text("${installed.size} installed widget${if(installed.size==1)"" else "s"} · independent styles",color=Soft,modifier=Modifier.padding(top=10.dp))}
        if(installed.isEmpty())item{Text("Your collection starts with one object. Open a design and choose Add to Home Screen.",color=Soft,fontSize=15.sp,modifier=Modifier.padding(vertical=38.dp))}
        items(installed.size,key={installed[it].first}){i->val(id,k)=installed[i];Column(Modifier.clickable{onEdit(id,k)}){Preview(store.read(id,k),data);Spacer(Modifier.height(10.dp));Text("${k.title} / #$id  ↗",fontFamily=Grotesk,fontSize=16.sp);Text("Tap to customize. Remove widgets through your launcher.",color=Soft,fontSize=11.sp,modifier=Modifier.padding(top=5.dp))}}
    }
}
@Composable private fun SettingsScreen(data: RenderData?,automatic: Boolean,busy: Boolean,onAuto: (Boolean)->Unit,onRefresh: ()->Unit,onMedia: ()->Unit,onRedraw: ()->Unit,onSystem: ()->Unit) {
    var licenses by remember{mutableStateOf(false)}
    val context=LocalContext.current
    val fullNotices=remember {
        listOf("THIRD-PARTY-NOTICES.txt","SPACE-GROTESK-LICENSE.txt","JOLPICA-TERMS.txt","APACHE-2.0.txt").joinToString("\n\n") { name ->
            context.assets.open("notices/$name").bufferedReader().use { it.readText() }
        }
    }
    LazyColumn(contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item{Eyebrow("THE LAB / 0.1.0");Heading("Quietly capable.",35);Text("No account. No ads. No analytics. Everything you configure lives on your device.",color=Soft,fontSize=14.sp,lineHeight=21.sp)}
        item{Rule();Eyebrow("Racing data");Row(verticalAlignment=Alignment.CenterVertically){Text("Automatic sync / every 6 hours",Modifier.weight(1f),fontSize=14.sp);Switch(checked=automatic,onCheckedChange=onAuto)};Button(onClick=onRefresh,enabled=!busy){Text(if(busy)"Refreshing…" else "Refresh racing feeds")}}
        item{val sections=listOf("Driver standings" to data?.racing?.drivers,"Constructor standings" to data?.racing?.teams,"Race calendar" to data?.racing?.schedule,"Latest results" to data?.racing?.result);sections.forEach{(title,s)->Column(Modifier.padding(vertical=8.dp)){Text(title,fontSize=13.sp);Text(if(s==null)"Loading…" else if(s.sample)"SAMPLE / 2024 · no successful sync" else "Cached · ${s.refreshed?.atZone(java.time.ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z"))}",color=if(s?.sample==true)Lime else Soft,fontSize=11.sp);s?.error?.let{Text(it,color=Color(0xFFFFA38E),fontSize=11.sp)}}};Text("Source: api.jolpi.ca / Ergast-compatible API. Four small requests per pass; manual refresh has a one-minute cooldown. An incomplete response never invents missing statistics.",color=Soft,fontSize=11.sp,lineHeight=18.sp)}
        item{Rule();Eyebrow("Permissions & diagnostics");OptionRow("Media access",if(data?.mediaAccess==true)"Enabled" else "Off",onMedia);Text("Calendar: ${if(data?.agenda?.permitted==true)"read-only access granted" else "off · enable from Private agenda"}",color=Soft,fontSize=12.sp);Text("Battery: ${data?.battery?.percent?:"—"}% · ${if(data?.battery?.charging==true)"charging" else "on battery"}",color=Soft,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));OutlinedButton(onClick=onRedraw){Text("Refresh installed widgets")};OptionRow("Android app settings","Permissions & power management",onSystem);Text("On Vivo, allow background activity and autostart for Widget Lab if periodic data or media updates stop. Exact options depend on your Funtouch OS version. Standard widgets cannot guarantee exact background intervals.",color=Soft,fontSize=12.sp,lineHeight=19.sp)}
        item{Rule();Eyebrow("Storage & privacy");Text("Racing JSON and imported artwork are stored in app-private files. Widget profiles are local. Artwork is never uploaded. Media metadata stays in memory. Calendar queries remain on this device. Android backup is disabled. Uninstalling removes local settings.",color=Soft,fontSize=12.sp,lineHeight=19.sp);Spacer(Modifier.height(12.dp));TextButton(onClick={licenses=true}){Text("Credits & licenses")};Text("Built for a home screen that feels like you.",fontFamily=Grotesk,fontSize=22.sp,modifier=Modifier.padding(vertical=25.dp))}
    }
    if(licenses)AlertDialog(onDismissRequest={licenses=false},title={Text("Credits & licenses")},text={Text(fullNotices,Modifier.verticalScroll(rememberScrollState()),fontSize=11.sp,lineHeight=17.sp)},confirmButton={TextButton(onClick={licenses=false}){Text("CLOSE")}})
}
