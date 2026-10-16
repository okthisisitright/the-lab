package dev.widgetlab.widgets

import android.content.Context
import android.graphics.Path
import org.json.JSONObject

/** Offline MIT-licensed, unofficial circuit diagrams; cached paths remain immutable to callers. */
object CircuitOutlines {
    private var catalog:JSONObject?=null
    private val cache=mutableMapOf<String,Path>()
    @Synchronized fun path(context:Context,id:String):Path? {
        cache[id]?.let{return Path(it)}
        val records=catalog ?: runCatching{context.assets.open("circuits/outlines.json").bufferedReader().use{JSONObject(it.readText()).getJSONObject("circuits")}}.getOrNull()?.also{catalog=it} ?: return null
        val points=records.optJSONArray(id) ?: return null
        if(points.length()<3)return null
        val path=Path()
        for(i in 0 until points.length()){
            val point=points.optJSONArray(i) ?: return null
            val x=point.optDouble(0).toFloat();val y=point.optDouble(1).toFloat()
            if(!x.isFinite()||!y.isFinite())return null
            if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
        }
        cache[id]=path
        return Path(path)
    }
}
