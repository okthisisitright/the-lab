package dev.widgetlab.widgets

import android.graphics.Bitmap
import android.util.LruCache
import kotlin.math.min

/** Small, cached software blur of the player's cover; never reads or blurs the wallpaper. */
object MusicArtwork {
    private val cache=object:LruCache<Int,Bitmap>(512*1024){override fun sizeOf(key:Int,value:Bitmap)=value.allocationByteCount}
    @Synchronized fun diffuse(source:Bitmap?):Bitmap? {
        if(source==null)return null
        val key=source.generationId
        cache.get(key)?.let{return it}
        val w=72;val h=72
        val scaled=Bitmap.createScaledBitmap(source,w,h,true)
        var pixels=IntArray(w*h);scaled.getPixels(pixels,0,w,0,0,w,h)
        if(scaled!==source)scaled.recycle()
        repeat(3) {
            for(horizontal in listOf(true,false)) {
                val output=IntArray(w*h)
                for(y in 0 until h)for(x in 0 until w){
                    var a=0;var r=0;var g=0;var b=0
                    for(offset in -4..4){
                        val xx=if(horizontal)(x+offset).coerceIn(0,w-1) else x
                        val yy=if(horizontal)y else (y+offset).coerceIn(0,h-1)
                        val color=pixels[yy*w+xx]
                        a+=color ushr 24;r+=color ushr 16 and 255;g+=color ushr 8 and 255;b+=color and 255
                    }
                    output[y*w+x]=((a/9) shl 24) or ((r/9) shl 16) or ((g/9) shl 8) or (b/9)
                }
                pixels=output
            }
        }
        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888).also{cache.put(key,it)}
    }
}
