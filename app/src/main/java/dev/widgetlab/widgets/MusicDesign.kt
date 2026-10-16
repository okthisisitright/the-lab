package dev.widgetlab.widgets

import dev.widgetlab.core.WidgetKind
import kotlin.math.min

data class MusicControl(val action:String,val bounds:Bounds,val ring:Boolean=false)
data class MusicLayout(
    val frame:Bounds, val circular:Boolean=false, val diffuse:Boolean=false,
    val cover:Bounds?=null, val title:Bounds, val artist:Bounds,
    val controls:List<MusicControl>, val titleSize:Float=18f, val state:Bounds?=null,
    val centered:Boolean=false
)

/** One layout contract for the actual Canvas, gallery and native clickable control layers. */
object MusicDesign {
    fun plan(kind:WidgetKind,w:Float,h:Float):MusicLayout {
        val whole=Bounds(0f,0f,w,h)
        fun controls(cx:Float,cy:Float,room:Float,toggleOnly:Boolean=false):List<MusicControl> {
            val target=min(44f,min(h-8,room/(if(toggleOnly)1 else 3))).coerceAtLeast(24f)
            val actions=if(toggleOnly)listOf("toggle") else listOf("previous","toggle","next")
            return actions.mapIndexed{i,a->MusicControl(a,Bounds(cx+(i-(actions.size-1)/2f)*target-target/2,cy-target/2,target,target),a=="toggle")}
        }
        if(h<112 || w<145 && kind !in listOf(WidgetKind.VINYL,WidgetKind.CASSETTE)) {
            val s=min(h-20,w*.23f).coerceAtLeast(24f);val x=s+22
            val targets=if(w>220)controls(w-44,h*.5f,80f).filter{it.action!="previous"}.mapIndexed{i,c->c.copy(bounds=c.bounds.copy(x=w-88+i*40f,w=40f,h=40f,y=h/2-20),ring=false)} else controls(w-25,h*.5f,40f,true)
            return MusicLayout(whole,cover=Bounds(10f,(h-s)/2,s,s),title=Bounds(x,h*.3f,w-x-100,22f),artist=Bounds(x,h*.58f,w-x-100,18f),controls=targets,titleSize=15f)
        }
        return when(kind) {
            WidgetKind.COMPACT -> {
                val s=min(h-28,w*.30f);val x=s+30
                MusicLayout(whole,diffuse=true,cover=Bounds(14f,(h-s)/2,s,s),title=Bounds(x,20f,w-x-16,26f),artist=Bounds(x,48f,w-x-16,20f),controls=controls((x+w-16)/2,h-31f,w-x-16),titleSize=17f)
            }
            WidgetKind.EDITORIAL -> {
                val pad=16f;val bottom=h-80;val side=min(w-2*pad,bottom-pad)
                val cover=Bounds(pad,pad,w-2*pad,(bottom-pad).coerceAtLeast(24f))
                MusicLayout(whole,diffuse=true,cover=cover,title=Bounds(20f,h-61,w-40,27f),artist=Bounds(20f,h-31,w-40,20f),controls=controls(w/2,cover.y+cover.h*.58f,side*.8f),titleSize=20f)
            }
            WidgetKind.GLASS -> {
                val s=min(h-42,w*.32f);val x=s+42;val room=w-x-20
                MusicLayout(whole,diffuse=true,cover=Bounds(20f,(h-s)/2,s,s),title=Bounds(x,27f,room,32f),artist=Bounds(x,64f,room,26f),controls=controls(x+room/2,h*.72f,room),titleSize=22f)
            }
            WidgetKind.VINYL,WidgetKind.CASSETTE -> {
                val size=min(w,h);val f=Bounds((w-size)/2,(h-size)/2,size,size)
                val ambient=kind==WidgetKind.CASSETTE
                val inset=if(ambient)Bounds(f.x+size*.32f,f.y+size*.18f,size*.36f,size*.36f) else null
                MusicLayout(f,circular=true,diffuse=ambient,cover=inset,title=Bounds(f.x+size*.14f,f.y+size*(if(ambient).57f else .40f),size*.72f,size*.12f),artist=Bounds(f.x+size*.18f,f.y+size*(if(ambient).69f else .55f),size*.64f,size*.08f),controls=controls(w/2,f.y+size*(if(ambient).84f else .77f),size*.65f,toggleOnly=size<190),titleSize=min(22f,size*.068f),centered=true)
            }
            WidgetKind.MATRIX -> {
                val center=h>w*1.2f
                MusicLayout(whole,diffuse=true,title=Bounds(24f,h*(if(center).48f else .25f),w-48,55f),artist=Bounds(24f,h*(if(center).48f else .25f)+60,w-48,30f),controls=controls(w/2,h*.81f,w-48),titleSize=25f,state=Bounds(24f,26f,w-48,20f),centered=true)
            }
            WidgetKind.BRUTALIST -> {
                val room=if(w>h*1.4f)w*.62f else w-40
                MusicLayout(whole,title=Bounds(22f,h*.22f,room,40f),artist=Bounds(22f,h*.22f+44,room,24f),controls=controls(if(w>h*1.4f)w-78 else w/2,h-34f,if(w>h*1.4f)132f else w-44),titleSize=24f)
            }
            else -> {
                val s=min(h-32,w*.37f);val x=s+36
                MusicLayout(whole,cover=Bounds(16f,(h-s)/2,s,s),title=Bounds(x,h*.43f,w-x-25,38f),artist=Bounds(x,h*.69f,w-x-25,30f),controls=emptyList(),titleSize=25f,state=Bounds(x,h*.23f,w-x-25,25f))
            }
        }
    }
}
