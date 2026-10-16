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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
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

private val Lime=Color(0xFFC3D8B0)
private val Ground=Color(0xFF111417)
private val Soft=Color(0xFF9DA5A6)
private val White=Color(0xFFF3F1E9)
private val Grotesk=FontFamily(Font(R.font.inter))
private val Display=FontFamily(Font(R.font.condensed))
private val Serif=FontFamily(Font(R.font.serif))
private val Mono=FontFamily(Font(R.font.mono))
@Composable fun LabTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Lime,onPrimary=Ground,background=Ground,surface=Color(0xFF1D2327),onSurface=White,onBackground=White,outline=Color(0xFF424B4E)),typography=Typography().let{it.copy(bodyLarge=it.bodyLarge.copy(fontFamily=Grotesk),bodyMedium=it.bodyMedium.copy(fontFamily=Grotesk),labelLarge=it.labelLarge.copy(fontFamily=Grotesk))},content=content)
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
        data=withContext(Dispatchers.IO) { RenderData(RacingRepository(context).snapshot(),MediaHub.info(),DeviceData.battery(context),DeviceData.agenda(context),MediaHub.permitted(context),focus=readFocus(context)) }
        installed=ProviderCatalog.installed(context);recent=store.recent()
    }
    LaunchedEffect(Unit) { while(true) {delay(60_000);version++} }
    val config=WidgetConfig.parse(profile,WidgetKind.CLOCK)
    val currentData=data?.copy(media=MediaHub.info(config.selection))
    fun choose(k: WidgetKind) {editingId=-1;profile=WidgetConfig(k).json();screen="detail"}
    fun back() {if(activity.configuring) activity.finish() else {screen="home";editingId=-1}}
    BackHandler(screen!="home" || activity.configuring) {back()}
    val calendar=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->version++;WidgetUpdates.request(context);if(!granted)message="Calendar access is off. The agenda will show its permission state."}
    fun importPhotos(uris:List<Uri>) {
        if(uris.isEmpty())return
        val previous=profile
        scope.launch {
            busy=true
            runCatching{withContext(Dispatchers.IO){uris.take(6).map{importArtwork(context,it)}}}
                .onSuccess{names->if(profile==previous)profile=(if(config.kind.isPhoto)config.copy(images=names,image=names.first()) else config.copy(image=names.first())).json();message="Images copied to private storage."}
                .onFailure{message="This image could not be opened. Try a JPEG, PNG or WebP file."}
            busy=false
        }
    }
    val artwork=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->importPhotos(listOfNotNull(uri))}
    val multiArtwork=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(if(config.kind==WidgetKind.COLLAGE)3 else 4)){uris->importPhotos(uris)}
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
            Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(if(screen=="home") "w/l." else "←",color=White,fontSize=27.sp,fontWeight=FontWeight.Bold,fontFamily=Grotesk,modifier=Modifier.clickable{if(screen!="home")back()}.padding(end=14.dp))
                Column(Modifier.weight(1f)){Text("WIDGET LAB",fontFamily=Grotesk,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp);Text("PERSONAL OBJECTS / EDITION II",color=Soft,fontFamily=Mono,fontSize=7.sp,letterSpacing=1.sp)}
                Text(if(screen=="settings")"CLOSE ×" else "STUDIO ↗",color=Soft,fontFamily=Mono,fontSize=10.sp,modifier=Modifier.clickable{screen=if(screen=="settings")"home" else "settings"}.padding(10.dp))
            }
            HorizontalDivider(color=Color(0xFF303332))
            when(screen) {
                "home" -> Home(data,installed.size,recent,collectionName,{collectionName=it.name},::choose,{c->editingId=-1;profile=c.json();screen="detail"},{screen="installed"})
                "detail" -> Detail(config,currentData,editingId,busy,{profile=it.json()},::save,::refresh,{activity.mediaSettings()},{calendar.launch(Manifest.permission.READ_CALENDAR)},{if(config.kind in listOf(WidgetKind.FILMSTRIP,WidgetKind.COLLAGE))multiArtwork.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else artwork.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))})
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
    val preserveAlpha=bitmap.hasAlpha()
    val name="${UUID.randomUUID()}.${if(preserveAlpha)"png" else "jpg"}"
    val file=File(context.filesDir,"art/$name").apply{parentFile?.mkdirs()}
    file.outputStream().use{check(bitmap.compress(if(preserveAlpha)Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,90,it))}
    bitmap.recycle();name
}

@Composable private fun Eyebrow(text:String,modifier:Modifier=Modifier,color:Color=Soft){Text(text.uppercase(),modifier,fontFamily=Mono,fontSize=9.sp,letterSpacing=1.sp,color=color)}
@Composable private fun Heading(text:String,size:Int=38){Text(text,fontFamily=Display,fontSize=size.sp,lineHeight=(size*.95f).sp,fontWeight=FontWeight.SemiBold,color=White)}
@Composable private fun Rule(){HorizontalDivider(Modifier.padding(vertical=20.dp),color=Color(0xFF343C40))}
@Composable private fun Action(text:String,onClick:()->Unit,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true){
    Box(modifier.fillMaxWidth().background(if(primary)Lime else Color(0xFF242C30)).clickable(enabled=enabled,onClick=onClick).padding(horizontal=17.dp,vertical=17.dp)){
        Text(text,Modifier.fillMaxWidth(),color=if(primary)Ground else White,fontFamily=Grotesk,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun ChoiceRow(label:String,values:List<Pair<String,String>>,current:String,onChange:(String)->Unit){
    Eyebrow(label,Modifier.padding(top=16.dp,bottom=10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        values.forEach{(key,title)->Text(title,Modifier.border(1.dp,if(current==key)Lime else Color(0xFF3C454A)).background(if(current==key)Color(0xFF29352D) else Color.Transparent).clickable{onChange(key)}.padding(horizontal=13.dp,vertical=11.dp),fontFamily=Grotesk,fontSize=12.sp,color=if(current==key)Lime else Soft)}
    }
}
@Composable private fun Field(label:String,value:String,onChange:(String)->Unit){
    Eyebrow(label,Modifier.padding(top=16.dp,bottom=10.dp))
    BasicTextField(value,onChange,Modifier.fillMaxWidth().background(Color(0xFF20272B)).padding(14.dp),textStyle=TextStyle(color=White,fontFamily=Grotesk,fontSize=15.sp),cursorBrush=androidx.compose.ui.graphics.SolidColor(Lime))
}

/** The backdrop is preview wallpaper, not part of the rendered widget. */
@Composable private fun Preview(config:WidgetConfig,data:RenderData?,modifier:Modifier=Modifier,shape:String="design",wallpaper:Int=0){
    val context=LocalContext.current
    val ratio=when(shape){"compact"->3.5f;"square"->1f;"tall"->.7f;else->420f/config.kind.height}
    val live=data?.copy(media=if(config.kind.isMusic)MediaHub.info(config.selection) else data.media)
    val sampleMusic=config.kind.isMusic&&live?.let{PreviewSamples.music(it)}==true
    val samplePhoto=PreviewSamples.photo(config)
    val sampleApps=config.kind.hasApps&&config.apps.isEmpty()
    val bitmap by produceState<Bitmap?>(null,config.json(),data,shape){if(live!=null)value=withContext(Dispatchers.Default){WidgetRenderer(context).render(PreviewSamples.applications(context,PreviewSamples.config(config)),PreviewSamples.data(context,config,live),630,(630/ratio).toInt())}}
    val transparent=config.surface in listOf("transparent","outline","semi","tint","glass")
    Column(modifier){
        Box(Modifier.fillMaxWidth().aspectRatio(ratio).clipToBounds().background(if(transparent)when(wallpaper){1->Color(0xFFD4CDBE);2->Color(0xFF493C35);else->Color(0xFF34454A)} else Color.Transparent)){
            if(transparent)Canvas(Modifier.fillMaxSize()){
                val stroke=Color.White.copy(alpha=.045f)
                for(i in 0..12)drawLine(stroke,androidx.compose.ui.geometry.Offset(size.width*.17f+i*size.width*.09f,-10f),androidx.compose.ui.geometry.Offset(i*size.width*.09f,size.height+10f),size.width*.035f)
            }
            bitmap?.let{Image(it.asImageBitmap(),"${config.kind.title} actual renderer preview",Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)}
        }
        if(sampleMusic||samplePhoto||sampleApps)Eyebrow(if(sampleApps)"APP STUDY / CHOOSE YOUR SHORTCUTS" else if(sampleMusic)"ART STUDY / CONNECT A PLAYER FOR YOUR MUSIC" else "NASA PHOTO STUDY / CHOOSE YOUR OWN IMAGES",Modifier.padding(top=8.dp),Soft)
    }
}

@Composable private fun Home(data:RenderData?,installed:Int,recent:List<WidgetConfig>,collectionName:String,onCollection:(Collection)->Unit,onKind:(WidgetKind)->Unit,onRecent:(WidgetConfig)->Unit,onInstalled:()->Unit){
    val collection=Collection.valueOf(collectionName)
    var query by rememberSaveable{mutableStateOf("")}
    val widgets=WidgetKind.entries.filter{(query.isNotBlank()||it.collection==collection)&&(query.isBlank()||it.title.contains(query,true)||it.collection.title.contains(query,true))}
    LazyColumn(contentPadding=PaddingValues(bottom=36.dp)){
        item{
            Column(Modifier.padding(22.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("YOUR HOMESCREEN, COMPOSED.");Text("$installed IN PLACE ↗",color=Lime,fontFamily=Mono,fontSize=9.sp,modifier=Modifier.clickable(onClick=onInstalled))}
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment=Alignment.Bottom){
                    BoxWithConstraints(Modifier.weight(1f)){
                        val context=LocalContext.current
                        val widthPerSp=remember{android.graphics.Paint().apply{typeface=LabType.face(context,"condensed",650);textSize=100f}.measureText("COLLECTION")/100f}
                        val headingSize=(maxWidth.value/widthPerSp/LocalDensity.current.fontScale*.94f).toInt().coerceAtMost(67)
                        Heading("THE\nCOLLECTION",headingSize)
                    }
                    Text("50",Modifier.padding(start=6.dp),fontFamily=Serif,fontSize=70.sp,color=Lime,textAlign=androidx.compose.ui.text.style.TextAlign.End)
                }
                Spacer(Modifier.height(16.dp));Text("Time. Sound. Speed. Your point of view.",fontFamily=Serif,fontSize=19.sp,color=Soft)
                Spacer(Modifier.height(28.dp))
                Eyebrow("STUDY 034 / TIME, WITHOUT A FRAME",color=Lime)
                Spacer(Modifier.height(12.dp))
                Preview(WidgetConfig(WidgetKind.SERIF),data,Modifier.clickable{onKind(WidgetKind.SERIF)})
                Row(Modifier.fillMaxWidth().padding(top=11.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Maison",fontFamily=Serif,fontSize=22.sp);Eyebrow("TRANSPARENT ↗",Modifier.padding(top=9.dp))}
                Rule()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("SIX COLLECTIONS / ONE PERSONAL STUDIO");Text("↗",color=Lime)}
                Spacer(Modifier.height(14.dp))
                Collection.entries.chunked(2).forEach{pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){pair.forEach{col->
                    Column(Modifier.weight(1f).clickable{onCollection(col);query=""}.padding(vertical=11.dp)){
                        Row{Text(col.title,fontSize=13.sp,color=if(col==collection)Lime else White,fontFamily=Grotesk,modifier=Modifier.weight(1f));Eyebrow(WidgetKind.entries.count{it.collection==col}.toString().padStart(2,'0'))}
                        HorizontalDivider(Modifier.padding(top=10.dp),color=if(col==collection)Lime else Color(0xFF343C40))
                    }
                }}}
                Field("FIND A DESIGN",query){query=it}
                Spacer(Modifier.height(25.dp));Heading(if(query.isBlank())collection.title else "Search / ${widgets.size}",37)
                Text(if(query.isBlank())collection.subtitle else "From the complete collection",color=Soft,fontFamily=Serif,fontSize=16.sp,modifier=Modifier.padding(top=8.dp,bottom=18.dp))
            }
        }
        items(widgets.size,key={widgets[it].name}){index->val k=widgets[index]
            Column(Modifier.padding(horizontal=22.dp).padding(bottom=30.dp).clickable{onKind(k)}){
                Preview(WidgetConfig(k),data)
                Row(Modifier.padding(top=12.dp),verticalAlignment=Alignment.Top){
                    Eyebrow((k.ordinal+1).toString().padStart(3,'0'),Modifier.width(36.dp).padding(top=5.dp),Lime)
                    Column(Modifier.weight(1f)){Text(k.title,fontFamily=Grotesk,fontSize=17.sp,fontWeight=FontWeight.Medium);Eyebrow(k.size+if(k.transparentDefault)" / TRANSPARENT" else " / ADAPTIVE",Modifier.padding(top=5.dp))}
                    Text("↗",fontSize=25.sp,color=Soft)
                }
            }
        }
        if(recent.isNotEmpty())item{Column(Modifier.padding(22.dp)){Rule();Eyebrow("RECENTLY COMPOSED");recent.forEach{cfg->OptionRow(cfg.kind.collection.title,cfg.kind.title){onRecent(cfg)}}}}
        item{Column(Modifier.padding(22.dp)){Rule();Text("MAKE ROOM\nFOR YOURSELF.",fontFamily=Display,fontSize=40.sp,lineHeight=37.sp,color=Soft);Eyebrow("WIDGET LAB / EDITION II / 50 OBJECTS",Modifier.padding(top=18.dp))}}
    }
}

@Composable private fun Detail(cfg:WidgetConfig,data:RenderData?,id:Int,busy:Boolean,onChange:(WidgetConfig)->Unit,onSave:()->Unit,onRefresh:()->Unit,onMedia:()->Unit,onCalendar:()->Unit,onArtwork:()->Unit){
    val context=LocalContext.current
    var picker by remember{mutableStateOf<String?>(null)}
    var previewShape by rememberSaveable(cfg.kind){mutableStateOf("design")}
    var wallpaper by rememberSaveable{mutableIntStateOf(0)}
    val availableApps=remember(picker){if(picker=="apps")LaunchApps.list(context) else emptyList()}
    Column(Modifier.fillMaxSize()){
        LazyColumn(Modifier.weight(1f).testTag("design-settings"),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
            item{Eyebrow("${(cfg.kind.ordinal+1).toString().padStart(3,'0')} / ${cfg.kind.collection.title}",color=Lime);Spacer(Modifier.height(12.dp));Heading(cfg.kind.title,43);Text(cfg.kind.size+" · launcher grid varies",color=Soft,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))}
            item{Preview(cfg,data,shape=previewShape,wallpaper=wallpaper);if(cfg.surface!="solid")Eyebrow("SHOWN OVER PREVIEW WALLPAPER",Modifier.padding(top=8.dp))}
            item{
                ChoiceRow("PREVIEW PROPORTIONS",listOf("design" to "Default","compact" to "Wide","square" to "Square","tall" to "Tall"),previewShape){previewShape=it}
                if(cfg.surface!="solid")ChoiceRow("PREVIEW WALLPAPER",listOf("0" to "Slate","1" to "Linen","2" to "Earth"),wallpaper.toString()){wallpaper=it.toInt()}
                Text(cfg.kind.description,color=Soft,fontSize=13.sp,lineHeight=20.sp,modifier=Modifier.padding(top=18.dp))
            }
            item{Rule();Eyebrow("COMPOSE YOUR OBJECT",color=Lime)
                if(!cfg.kind.isPhoto&&cfg.kind !in listOf(WidgetKind.ARTWORK,WidgetKind.GLASS)){
                    ChoiceRow("BACKGROUND",listOf("transparent" to "None","solid" to "Solid","semi" to "Translucent","tint" to "Tint","glass" to "Glass","outline" to "Outline"),cfg.surface){onChange(cfg.copy(surface=it))}
                }
                if((!cfg.kind.isPhoto&&cfg.kind!=WidgetKind.ARTWORK)||cfg.kind==WidgetKind.FRAME)ChoiceRow("TYPE & SURFACE COLOR",listOf("dark" to "Light type / dark surface","light" to "Dark type / light surface"),if(cfg.light)"light" else "dark"){onChange(cfg.copy(light=it=="light"))}
                if(!cfg.kind.isPhoto&&cfg.kind !in listOf(WidgetKind.ARTWORK,WidgetKind.CASSETTE,WidgetKind.GLASS,WidgetKind.STRIP,WidgetKind.GRID,WidgetKind.DRIVER,WidgetKind.MINIMAL_DRIVER,WidgetKind.CONSTRUCTOR,WidgetKind.DRIVER_PODIUM,WidgetKind.TEAM_PODIUM,WidgetKind.WEEKEND)||(cfg.surface=="tint"&&cfg.kind!=WidgetKind.GLASS)){
                    Eyebrow("ACCENT",Modifier.padding(top=18.dp,bottom=12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.horizontalScroll(rememberScrollState())){
                        listOf("#C3D8B0","#F1E5CA","#5AC7B5","#E56152","#E6A95C","#96B2D0","#F4F4EF").forEach{hex->Box(Modifier.size(29.dp).background(Color(android.graphics.Color.parseColor(hex)),CircleShape).border(if(cfg.accent==hex)3.dp else 0.dp,White,CircleShape).clickable{onChange(cfg.copy(accent=hex,accentText=cfg.kind.isClock||cfg.kind.hasAgenda||cfg.kind.hasLabel||cfg.accentText))})}
                    }
                }
                if(!cfg.kind.isPhoto&&cfg.kind!=WidgetKind.ANALOG)ChoiceRow("TYPOGRAPHY",listOf("design" to "Original","sans" to "Swiss","condensed" to "Condensed","serif" to "Serif","technical" to "Technical","mono" to "Mono","dots" to "Dots"),cfg.typography){onChange(cfg.copy(typography=it))}
                if((cfg.kind.isClock||cfg.kind.hasAgenda||cfg.kind.hasLabel)&&cfg.kind!=WidgetKind.ANALOG)ChoiceRow("PRIMARY TYPE COLOR",listOf("default" to "Theme","accent" to "Accent"),if(cfg.accentText)"accent" else "default"){onChange(cfg.copy(accentText=it=="accent"))}
                if(cfg.kind.isClock&&cfg.kind!=WidgetKind.ANALOG){
                    ChoiceRow("ALIGNMENT",listOf("design" to "Original","start" to "Left","center" to "Center","end" to "Right"),cfg.alignment){onChange(cfg.copy(alignment=it))}
                    ChoiceRow("DATE",listOf("yes" to "Visible","no" to "Hidden"),if(cfg.showDate)"yes" else "no"){onChange(cfg.copy(showDate=it=="yes"))}
                    if(cfg.showDate)ChoiceRow("WEEKDAY",listOf("yes" to "Visible","no" to "Hidden"),if(cfg.showWeekday)"yes" else "no"){onChange(cfg.copy(showWeekday=it=="yes"))}
                }
                if(cfg.surface!="transparent"&&!cfg.kind.isPhoto&&cfg.kind !in listOf(WidgetKind.BRUTALIST,WidgetKind.BRUTAL_CLOCK))ChoiceRow("CORNERS",listOf("0" to "Square","12" to "Soft","24" to "Rounded","40" to "Pill"),cfg.corners.toString()){onChange(cfg.copy(corners=it.toInt()))}
            }
            if(cfg.kind.hasPortrait){item{
                Rule()
                val entries=if(cfg.kind==WidgetKind.CONSTRUCTOR)data?.racing?.teams?.value?.entries?.map{it.id to it.name} else data?.racing?.drivers?.value?.entries?.map{it.id to it.name}
                OptionRow(if(cfg.kind==WidgetKind.CONSTRUCTOR)"CONSTRUCTOR" else "DRIVER",entries?.find{it.first==cfg.selection}?.second ?: "First in cached standings"){picker="competitor"}
                Action(if(cfg.image.isBlank())"Add your ${if(cfg.kind==WidgetKind.CONSTRUCTOR)"car artwork" else "portrait"}  +" else "Replace personal artwork",onArtwork,enabled=!busy)
                Text("Use an image you have permission to use. The included Hamilton photo is a credited 2021 archive, independent of current standings.",fontSize=11.sp,color=Soft,lineHeight=17.sp)
            }}
            if(cfg.kind.isPhoto){item{
                Rule();Eyebrow("YOUR PHOTOGRAPHS")
                Spacer(Modifier.height(12.dp));Action(if(cfg.kind==WidgetKind.FILMSTRIP)"Choose up to 4 photographs  +" else if(cfg.kind==WidgetKind.COLLAGE)"Choose 3 photographs  +" else "Choose photograph  +",onArtwork,enabled=!busy)
                Text("Android's photo picker grants access only to your selection. Optimized copies remain in private app storage after restart.",color=Soft,fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=10.dp))
                if(cfg.photos.isNotEmpty())OptionRow("SELECTED IMAGES","${cfg.photos.size} · tap to clear"){onChange(cfg.copy(image="",images=emptyList()))}
                if(cfg.kind in listOf(WidgetKind.FRAME,WidgetKind.POLAROID))Field("CAPTION",cfg.caption){onChange(cfg.copy(caption=it.take(120)))}
                if(cfg.kind in listOf(WidgetKind.PHOTO,WidgetKind.FRAME))ChoiceRow("FRAME CORNERS",listOf("0" to "Square","12" to "Soft","24" to "Rounded","40" to "Round"),cfg.corners.toString()){onChange(cfg.copy(corners=it.toInt()))}
            }}
            if(cfg.kind.hasPortrait||cfg.kind.isPhoto){item{
                ChoiceRow("IMAGE FOCAL ALIGNMENT",listOf("center" to "Center","top" to "Top","bottom" to "Bottom","left" to "Left","right" to "Right"),cfg.crop){onChange(cfg.copy(crop=it))}
            }}
            if(cfg.kind in listOf(WidgetKind.DRIVER_STANDINGS,WidgetKind.CONSTRUCTOR_STANDINGS)){item{ChoiceRow("MAXIMUM STANDINGS ROWS",listOf("3" to "3","5" to "5","10" to "10"),cfg.rows.toString()){onChange(cfg.copy(rows=it.toInt()))};Text("Visible rows adapt to height; small widgets keep readable text.",color=Soft,fontSize=11.sp)}}
            if(cfg.kind.isClock||cfg.kind.hasAgenda||cfg.kind in listOf(WidgetKind.WEEKEND,WidgetKind.WEEKEND_EDITORIAL,WidgetKind.COUNTDOWN)){item{
                Rule()
                if(cfg.kind!=WidgetKind.ANALOG||android.os.Build.VERSION.SDK_INT>=31)OptionRow("TIMEZONE",if(cfg.timezone=="device")"Device timezone" else cfg.timezone){picker="timezone"}
                else Text("On Android 10–11 the native analog clock uses the device timezone.",color=Soft,fontSize=12.sp)
                if(cfg.kind==WidgetKind.DUAL)OptionRow("SECOND TIMEZONE",cfg.secondaryZone){picker="secondZone"}
                if(cfg.kind!=WidgetKind.ANALOG&&cfg.kind!=WidgetKind.DATE)ChoiceRow("TIME FORMAT",listOf("24" to "24 hour","12" to "12 hour"),if(cfg.hour24)"24" else "12"){onChange(cfg.copy(hour24=it=="24"))}
            }}
            if(cfg.kind.isMusic){item{
                Rule();Eyebrow("MEDIA CONNECTION",color=Lime)
                Text(if(data?.mediaAccess==true)"Media access enabled" else "Connect your listening room",fontFamily=Serif,fontSize=23.sp,modifier=Modifier.padding(vertical=12.dp))
                Text("Notification-listener access discovers active media sessions. Widget Lab does not store notification messages. Start playback in your music app to expose artwork and supported controls.",color=Soft,fontSize=12.sp,lineHeight=19.sp)
                Spacer(Modifier.height(12.dp));Action(if(data?.mediaAccess==true)"Review Android media access  ↗" else "Enable media access  ↗",onMedia)
                OptionRow("PLAYER",if(cfg.selection.isBlank())"Automatic / playing session first" else appLabel(context,cfg.selection)){picker="media"}
            }}
            if(cfg.kind.hasAgenda){item{Rule();Eyebrow("PRIVATE DEVICE CALENDAR");Text("Upcoming events remain on your device. Granting access is optional; permission-denied and empty-calendar states are explicit.",color=Soft,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(vertical=12.dp));Action(if(data?.agenda?.permitted==true)"Calendar access granted" else "Allow read-only calendar access",onCalendar)}}
            if(cfg.kind.hasLabel){item{Rule();Field("SECTION NAME",cfg.label){onChange(cfg.copy(label=it.take(60)))};if(cfg.kind!=WidgetKind.LAUNCHER)Field("SUPPORTING LABEL",cfg.caption){onChange(cfg.copy(caption=it.take(120)))}}}
            if(cfg.kind.hasApps){item{Rule();OptionRow("APPLICATION SHORTCUTS","${cfg.apps.size} selected / choose up to 6"){picker="apps"}
                ChoiceRow("ICONS",listOf("original" to "Original","mono" to "Monochrome","letters" to "Initials"),cfg.iconStyle){onChange(cfg.copy(iconStyle=it))}
                ChoiceRow("APP NAMES",listOf("yes" to "Visible","no" to "Hidden"),if(cfg.showLabels)"yes" else "no"){onChange(cfg.copy(showLabels=it=="yes"))}
                Text("These are widget shortcuts, not native launcher folders. Missing or uninstalled applications open configuration.",color=Soft,fontSize=11.sp,lineHeight=18.sp,modifier=Modifier.padding(top=12.dp))}}
            if(cfg.kind.isRacing){item{
                Rule();Eyebrow("RACING SOURCE",color=Lime)
                Text("Jolpica / public Formula 1 snapshots. Each feed has independent timestamps and offline caching. Historical samples are labeled; no statistics are fabricated.",color=Soft,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(vertical=12.dp))
                listOf("Drivers" to data?.racing?.drivers,"Constructors" to data?.racing?.teams,"Schedule" to data?.racing?.schedule,"Results" to data?.racing?.result).forEach{(name,s)->Text("$name · ${if(s?.sample!=false)"SAMPLE / 2024" else s.refreshed?.atZone(cfg.zone())?.format(DateTimeFormatter.ofPattern("d MMM HH:mm z"))}${if(s?.error!=null)" / OFFLINE" else ""}",color=Soft,fontFamily=Mono,fontSize=9.sp,modifier=Modifier.padding(bottom=7.dp))}
                Action(if(busy)"Refreshing…" else "Refresh racing data  ↻",onRefresh,enabled=!busy)
            }}
            item{Rule();Eyebrow("HOW IT WORKS");Text(when{
                cfg.kind.isClock->"Native Android TextClock / AnalogClock keeps time independently of app refreshes. No aggressive polling or artificial seconds."
                cfg.kind.isMusic->"Session callbacks update metadata and playback. Buttons act only on supported controls. No invented playback progress."
                cfg.kind.isPhoto->"Your chosen photos are copied to private app storage and rendered with focal cropping. Tap the widget to change its photographs."
                cfg.kind.hasApps->"Tap an icon to open its installed application. Selections are stored independently for each widget."
                cfg.kind==WidgetKind.FOCUS->"Reads Android's current interruption filter. Tap to open Android Do Not Disturb settings. This does not infer a proprietary Vivo focus mode."
                cfg.kind==WidgetKind.BATTERY||cfg.kind==WidgetKind.BATTERY_EDITORIAL->"Real Android battery and charging status. Power events and approximately 30-minute work refresh the display; device power policy may delay updates."
                cfg.kind.hasAgenda->"Native time/date with a local read-only calendar query. Agenda data refreshes approximately every 30 minutes."
                cfg.kind.hasLabel->"A real transparent Android widget. Tap to edit this label."
                else->"Cached racing snapshots refresh approximately every six hours. Countdown values update periodically. Session times use IANA timezones and daylight-saving rules."
            },color=Soft,fontSize=12.sp,lineHeight=19.sp,modifier=Modifier.padding(top=12.dp,bottom=12.dp));Text("Long-press the home-screen widget to resize. Layouts change with the available space; your launcher controls the exact grid.",color=Soft,fontSize=11.sp,lineHeight=17.sp)}
        }
        Box(Modifier.background(Ground).padding(horizontal=22.dp,vertical=12.dp)){Action(if(busy)"Working…" else if(id>=0)"Apply to this widget  ↗" else "Add to Home Screen  +",onSave,primary=true,enabled=!busy)}
    }
    if(picker!=null){
        var filter by remember{mutableStateOf("")}
        val choices=when(picker){
            "competitor"->listOf("" to "First in cached standings")+(if(cfg.kind==WidgetKind.CONSTRUCTOR)data?.racing?.teams?.value?.entries?.map{it.id to "${it.name} / ${data.racing.teams.value.season}"} else data?.racing?.drivers?.value?.entries?.map{it.id to "${it.name} / ${data.racing.drivers.value.season}"}).orEmpty()
            "media"->listOf("" to "Automatic")+MediaHub.apps().map{it to appLabel(context,it)}
            "apps"->cfg.apps.filter{component->availableApps.none{it.component==component}}.map{it to "Unavailable / ${ComponentName.unflattenFromString(it)?.packageName ?: "remove selection"}"}+availableApps.map{it.component to it.label}
            else->listOf("device" to "Device timezone")+java.time.ZoneId.getAvailableZoneIds().sorted().map{it to it.replace('_',' ')}
        }.filter{it.second.contains(filter,true)}
        AlertDialog(onDismissRequest={picker=null},title={Text(if(picker=="apps")"Choose applications" else "Select ${picker}",fontFamily=Serif)},text={Column{
            Field("SEARCH",filter){filter=it};Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.heightIn(max=360.dp)){items(choices.size){i->val(key,name)=choices[i];Row(Modifier.fillMaxWidth().clickable{
                when(picker){"timezone"->onChange(cfg.copy(timezone=key));"secondZone"->onChange(cfg.copy(secondaryZone=key));"apps"->onChange(cfg.copy(apps=if(key in cfg.apps)cfg.apps-key else (cfg.apps+key).take(6)));else->onChange(cfg.copy(selection=key))}
                if(picker!="apps")picker=null
            }.padding(vertical=14.dp)){Text(name,fontSize=13.sp,color=if(picker=="apps"&&key in cfg.apps)Lime else White,modifier=Modifier.weight(1f));if(picker=="apps")Text(if(key in cfg.apps)"✓" else "+",color=Lime)}}}
        }},confirmButton={TextButton(onClick={picker=null}){Text("DONE")}})
    }
}
private fun appLabel(context:android.content.Context,pkg:String)=runCatching{context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg,0)).toString()}.getOrDefault(pkg)
@Composable private fun OptionRow(title:String,value:String,onClick:()->Unit){Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=14.dp)){Eyebrow(title);Row(Modifier.padding(top=7.dp)){Text(value,fontFamily=Grotesk,fontSize=14.sp,modifier=Modifier.weight(1f));Text("↗",color=Lime)}}}
@Composable private fun Installed(installed:List<Pair<Int,WidgetKind>>,store:ConfigStore,data:RenderData?,onEdit:(Int,WidgetKind)->Unit){LazyColumn(contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){
    item{Eyebrow("YOUR HOME SCREEN",color=Lime);Spacer(Modifier.height(16.dp));Heading("IN PLACE.",62);Text("${installed.size} objects / independent settings",color=Soft,fontFamily=Serif,fontSize=18.sp,modifier=Modifier.padding(top=14.dp))}
    if(installed.isEmpty())item{Text("Find your first object in the collection. Add it here, then arrange it on your home screen.",color=Soft,fontSize=14.sp,lineHeight=21.sp,modifier=Modifier.padding(vertical=30.dp))}
    items(installed.size,key={installed[it].first}){i->val(id,k)=installed[i];Column(Modifier.clickable{onEdit(id,k)}){Preview(store.read(id,k).copy(kind=k),data);OptionRow("INSTANCE #$id",k.title){onEdit(id,k)}}}
}}
@Composable private fun SettingsScreen(data:RenderData?,automatic:Boolean,busy:Boolean,onAuto:(Boolean)->Unit,onRefresh:()->Unit,onMedia:()->Unit,onRedraw:()->Unit,onSystem:()->Unit){
    val context=LocalContext.current;var licenses by remember{mutableStateOf(false)}
    val notices=remember{context.assets.list("notices").orEmpty().filter{it.endsWith(".txt")}.joinToString("\n\n"){name->name+"\n"+context.assets.open("notices/$name").bufferedReader().use{it.readText()}}}
    LazyColumn(contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Eyebrow("EDITION II / ${dev.widgetlab.BuildConfig.VERSION_NAME}",color=Lime);Spacer(Modifier.height(15.dp));Heading("THE STUDIO.",59);Text("50 objects. No account. No ads. No analytics.",fontFamily=Serif,color=Soft,fontSize=20.sp,modifier=Modifier.padding(top=18.dp))}
        item{Rule();Eyebrow("RACING DATA");OptionRow("AUTOMATIC SYNC",if(automatic)"Every six hours / enabled" else "Off"){onAuto(!automatic)};Action(if(busy)"Refreshing…" else "Refresh racing feeds",onRefresh,enabled=!busy)}
        item{listOf("Drivers" to data?.racing?.drivers,"Constructors" to data?.racing?.teams,"Schedule" to data?.racing?.schedule,"Results" to data?.racing?.result).forEach{(name,s)->Text("$name / ${if(s?.sample!=false)"SAMPLE · 2024" else s.refreshed?.atZone(java.time.ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm z"))}",color=Soft,fontSize=11.sp);s?.error?.let{Text(it,color=Color(0xFFE5A494),fontSize=11.sp)}}}
        item{Rule();Eyebrow("PERMISSIONS & DIAGNOSTICS");OptionRow("MEDIA ACCESS",if(data?.mediaAccess==true)"Enabled" else "Connect a player",onMedia);Text("Calendar: ${if(data?.agenda?.permitted==true)"granted" else "off / enable from Date or Today"}",color=Soft,fontSize=12.sp);Text("Battery: ${data?.battery?.percent ?: "—"}% / ${if(data?.battery?.charging==true)"charging" else "on battery"}",color=Soft,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));Text("DND: ${data?.focus?.title ?: "Unknown"}",color=Soft,fontSize=12.sp,modifier=Modifier.padding(top=8.dp));Spacer(Modifier.height(16.dp));Action("Refresh installed widgets",onRedraw);OptionRow("ANDROID APP SETTINGS","Permissions & power management",onSystem);Text("On Vivo, allow background activity and autostart if media or periodic data updates stop. Time displays use native Android clocks. Physical Vivo launcher behavior still needs device testing.",color=Soft,fontSize=12.sp,lineHeight=19.sp)}
        item{Rule();Eyebrow("LOCAL BY DESIGN");Text("Photos and widget profiles stay in app-private storage. Calendar information is never uploaded. Media metadata stays in memory. Android backup is disabled. Uninstalling removes your local settings. Only public racing feeds use the network.",color=Soft,fontSize=12.sp,lineHeight=19.sp);OptionRow("CREDITS & LICENSES","Type, imagery and data"){licenses=true};Text("A home screen,\nwith a point of view.",fontFamily=Serif,fontSize=30.sp,lineHeight=35.sp,modifier=Modifier.padding(vertical=22.dp))}
    }
    if(licenses)AlertDialog(onDismissRequest={licenses=false},title={Text("Credits & licenses")},text={Text(notices,Modifier.verticalScroll(rememberScrollState()),fontSize=10.sp,lineHeight=16.sp)},confirmButton={TextButton(onClick={licenses=false}){Text("CLOSE")}})
}
