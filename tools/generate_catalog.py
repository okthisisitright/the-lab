"""Generate native provider metadata and font-specific RemoteViews from the stable catalog."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]; app=root/'app/src/main'; text=(app/'java/dev/widgetlab/core/Catalog.kt').read_text()
items=re.findall(r'^    (\w+)\("([^"]+)",Collection\.(\w+),"([^"]+)",(\d+),"([^"]+)"\)',text,re.M)
assert len(items)==50
legacy={'DRIVER':'Driver','CONSTRUCTOR':'Constructor','DRIVER_STANDINGS':'DriverStandings','CONSTRUCTOR_STANDINGS':'ConstructorStandings','WEEKEND':'Weekend','COUNTDOWN':'Countdown','RESULTS':'Results','EDITORIAL':'Editorial','GLASS':'Glass','VINYL':'Vinyl','BRUTALIST':'Brutalist','MATRIX':'Matrix','CASSETTE':'Cassette','COMPACT':'Compact','CLOCK':'Clock','DATE':'Date','BATTERY':'Battery','SWISS':'Swiss'}
names={key:legacy.get(key,''.join(p.title() for p in key.split('_')))+'Widget' for key,*_ in items}
# No positional relationship between enum and provider class.
provider='package dev.widgetlab.widgets\n\nimport dev.widgetlab.core.WidgetKind\n\nobject ProviderTypes {\n    val classes=mapOf(\n'+',\n'.join('        WidgetKind.'+key+' to '+name+'::class.java' for key,name in names.items())+'\n    )\n}\n'+''.join('class '+name+': LabProvider()\n' for name in names.values())
(app/'java/dev/widgetlab/widgets/ProviderTypes.kt').write_text(provider)
manifest=(app/'AndroidManifest.xml').read_text(); manifest=manifest[:manifest.index('        <receiver android:name=".widgets.DriverWidget"')]
from xml.sax.saxutils import escape
for key,title,col,size,height,desc in items:
    name=key.lower().replace('_',''); compact=key in ['COMPACT','STRIP','LABEL','SECTION','CORNER','BATTERY','FOCUS','FILMSTRIP','COUNTDOWN']
    minw=110 if col in ['CLOCKS','PHOTO'] or key in ['BATTERY','LABEL','SECTION','GRID'] else 180
    minh=54 if compact or col=='CLOCKS' and key not in ['ANALOG','VERTICAL','SPLIT','BRUTAL_CLOCK'] else 110
    if key=='VERTICAL':minh=210
    if key=='ANALOG':minh=110
    xml=f'''<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="{minw if minw==110 else 250}dp" android:minHeight="{max(minh,int(int(height)*.65))}dp"
    android:minResizeWidth="{minw}dp" android:minResizeHeight="{minh}dp"
    android:targetCellWidth="{2 if minw==110 else 4}" android:targetCellHeight="{1 if compact else 3 if key=='VERTICAL' else 2}"
    android:updatePeriodMillis="1800000" android:previewImage="@drawable/preview_{name}" android:initialLayout="@layout/widget_surface"
    android:configure="dev.widgetlab.ui.WidgetConfigureActivity"
    android:resizeMode="horizontal|vertical" android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable" android:description="@string/desc_{name}" />'''
    (app/f'res/xml/provider_{name}.xml').write_text(xml)
    manifest+=f'''        <receiver android:name=".widgets.{names[key]}" android:label="{escape(title)}" android:exported="true">
            <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/provider_{name}" />
        </receiver>\n'''
manifest+='    </application>\n</manifest>\n'; (app/'AndroidManifest.xml').write_text(manifest)
(app/'res/values/strings.xml').write_text('<resources>\n    <string name="app_name">Widget Lab</string>\n'+''.join(f'    <string name="desc_{key.lower().replace("_","")}">{escape(desc).replace(chr(39),chr(92)+chr(39))}</string>\n' for key,_,_,_,_,desc in items)+'</resources>\n')
# Picker art will be replaced by generated renders after tests. Temporary copies keep compilation possible.
from shutil import copyfile
for key,*_ in items:
    dest=app/f'res/drawable-nodpi/preview_{key.lower().replace("_","")}.png'
    if not dest.exists():copyfile(app/'res/drawable-nodpi/preview_clock.png',dest)
# Font-specific layouts let native TextClock use licensed typefaces in another process.
for face in ['sans','thin','bold','condensed','serif','technical','mono','dots']:
    font={'sans':'inter','thin':'inter','bold':'inter'}.get(face,face)
    style='bold' if face=='bold' else 'normal'
    weight='200' if face=='thin' else '850' if face=='bold' else '650' if face=='condensed' else '450' if face=='serif' else '500'
    for light in [False,True]:
        base='''<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/widget_root" android:layout_width="match_parent" android:layout_height="match_parent">
    <ImageView android:id="@+id/surface" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="fitXY" android:contentDescription="Widget Lab" />
'''
        for i,id in enumerate(['time','clock_second','clock_date','clock_extra']):
            ff=font if i<2 else 'inter'
            base+=f'''    <FrameLayout android:id="@+id/clock_slot{i}" android:layout_width="match_parent" android:layout_height="match_parent" android:visibility="gone">
        <TextClock android:id="@+id/{id}" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="center_vertical|start" android:includeFontPadding="false" android:fontFamily="@font/{ff}" android:textFontWeight="{weight if i<2 else '400'}" android:textStyle="{style if i<2 else 'normal'}" android:textSize="48sp" android:format12Hour="h:mm" android:format24Hour="HH:mm" />
    </FrameLayout>\n'''
        base+=f'''    <AnalogClock android:id="@+id/analog" android:layout_width="match_parent" android:layout_height="match_parent" android:layout_margin="8dp" android:visibility="gone" android:dial="@drawable/dial_{'dark' if light else 'light'}" android:hand_hour="@drawable/hand_hour_{'dark' if light else 'light'}" android:hand_minute="@drawable/hand_minute_{'dark' if light else 'light'}" />
    <LinearLayout android:id="@+id/transport" android:layout_width="match_parent" android:layout_height="48dp" android:layout_gravity="bottom" android:gravity="end|center_vertical" android:paddingEnd="12dp" android:orientation="horizontal" android:visibility="gone">
'''
        for id in ['previous','toggle','next']:
            base+=f'        <ImageView android:id="@+id/{id}" android:layout_width="44dp" android:layout_height="44dp" android:padding="13dp" android:src="@drawable/control_{"play" if id=="toggle" else id}" android:contentDescription="{id}" />\n'
        base+='''    </LinearLayout>
    <LinearLayout android:id="@+id/shortcut_host" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical" android:visibility="gone" />
</FrameLayout>\n'''
        (app/f'res/layout/surface_{face}_{"light" if light else "dark"}.xml').write_text(base)
        if face=='sans' and not light:(app/'res/layout/widget_surface.xml').write_text(base)
(app/'res/layout/shortcut_row.xml').write_text('<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/shortcut_row" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="horizontal" />')
(app/'res/layout/shortcut_cell.xml').write_text('<TextView xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/shortcut_click" android:layout_width="0dp" android:layout_weight="1" android:layout_height="match_parent" android:background="@android:color/transparent" android:text="" />')
for mode,color in [('light','#F5F4EE'),('dark','#181B1C')]:
    marks=[]
    import math
    for i in range(60):
        a=i*math.pi/30; r=90 if i%5 else 85
        marks.append(f'M {100+math.sin(a)*r:.2f},{100-math.cos(a)*r:.2f} L {100+math.sin(a)*94:.2f},{100-math.cos(a)*94:.2f}')
    path=' '.join(marks)
    (app/f'res/drawable/dial_{mode}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="200dp" android:height="200dp" android:viewportWidth="200" android:viewportHeight="200"><path android:pathData="{path}" android:strokeColor="{color}" android:strokeWidth="1.3" /></vector>')
    for hand,length,width in [('hour',49,3),('minute',75,1.7)]:
        (app/f'res/drawable/hand_{hand}_{mode}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="200dp" android:height="200dp" android:viewportWidth="200" android:viewportHeight="200"><path android:pathData="M100,{100-length} L100,108" android:strokeColor="{color}" android:strokeWidth="{width}" android:strokeLineCap="round"/><path android:pathData="M97,100 A3,3 0,1 0,103 100 A3,3 0,1 0,97 100" android:fillColor="{color}"/></vector>')
for name,path in {'play':'M8,5 L19,12 L8,19 Z','pause':'M7,5 L10,5 L10,19 L7,19 Z M14,5 L17,5 L17,19 L14,19 Z','next':'M5,5 L15,12 L5,19 Z M17,5 L19,5 L19,19 L17,19 Z','previous':'M19,5 L9,12 L19,19 Z M5,5 L7,5 L7,19 L5,19 Z'}.items():
    (app/f'res/drawable/control_{name}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FFFFFF" android:pathData="{path}" /></vector>')
print('Generated',len(items),'providers and native clock layouts')
for old in (app/'res/layout').glob('tick_*.xml'):
    old.unlink()
for face in ['sans','condensed','serif','technical','mono','dots']:
    font='inter' if face=='sans' else face
    for weight in [200,400,450,500,650,700,850]:
        for alignment in ['start','center','end']:
            gravity='center_horizontal' if alignment=='center' else alignment
            (app/f'res/layout/tick_{face}_{weight}_{alignment}.xml').write_text(f'<TextClock xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/clock_text" android:layout_width="match_parent" android:layout_height="match_parent" android:includeFontPadding="false" android:gravity="{gravity}|center_vertical" android:fontFamily="@font/{font}" android:textFontWeight="{weight}" android:textSize="48sp" android:format12Hour="h:mm" android:format24Hour="HH:mm" />')
lines=['# The 50 objects / Edition II','', 'Every row corresponds to a registered Android AppWidgetProvider. Launcher cell sizes vary; resize ranges below describe the intended supported grid. Compact layouts retain only information that fits.','', '| # | Design | Supported grid | Function | Customization |','|---|---|---|---|---|']
for index,(key,title,col,size,height,desc) in enumerate(items,1):
    options=['theme / typography'] if col!='PHOTO' else ['photo selection','focal alignment']
    if col=='CLOCKS':options+=['surface / transparency','time format' if key!='ANALOG' else 'native dial','timezone','date / weekday' if key!='ANALOG' else 'API 31+ zone']
    if col=='RACING':options+=['surface','accent','driver / portrait' if key in ['DRIVER','MINIMAL_DRIVER'] else 'team / car art' if key=='CONSTRUCTOR' else 'row limit' if 'STANDINGS' in key else 'IANA timezone' if key in ['WEEKEND','WEEKEND_EDITORIAL','COUNTDOWN'] else 'cached source']
    if col=='MUSIC':options+=['preferred media app','surface','real session artwork']
    if col=='PHOTO':options+=['corners' if key in ['PHOTO','FRAME'] else 'multi-image layout' if key in ['COLLAGE','FILMSTRIP'] else 'caption']
    if key=='FRAME':options+=['caption','frame theme']
    if col=='ESSENTIALS':options+=['surface','accent','calendar permission / timezone' if key in ['DATE','TODAY'] else 'actual device status']
    if col=='LAUNCHERS':options+=['transparency','apps / icon style / name visibility' if key in ['STRIP','LAUNCHER','GRID'] else 'label / caption']
    lines.append(f'| {index:02} | {title} | {size} | {desc} | {", ".join(options)} |')
lines+=['','Controls are restricted in the configuration UI where a fixed artwork/frame treatment applies. AnalogClock uses the device timezone on Android 10–11. No clock promises bitmap updates every second. Multi-image collage uses the first three selected images; filmstrip uses up to four according to width. Small launcher layouts reveal additional selections after expansion.','', 'Gallery and picker previews use the same renderer. NASA photographic/cover studies are explicitly sample content; installed widgets require real media sessions or chosen personal photos.']
(root/'docs/CATALOG.md').write_text('\n'.join(lines)+'\n')
