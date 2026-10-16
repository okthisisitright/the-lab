package dev.widgetlab.ui

import android.content.Context
import dev.widgetlab.core.WidgetConfig
import dev.widgetlab.core.WidgetKind
import dev.widgetlab.media.MediaInfo
import dev.widgetlab.widgets.ArtCache
import dev.widgetlab.widgets.RenderData

/** Explicit gallery-only art studies. Installed widgets always receive actual device/session data. */
object PreviewSamples {
    fun applications(context:Context,config:WidgetConfig)=if(config.kind.hasApps&&config.apps.isEmpty())config.copy(apps=dev.widgetlab.data.LaunchApps.list(context).take(6).map{it.component}) else config
    fun music(data:RenderData)=data.media.packageName.isBlank()
    fun photo(config:WidgetConfig)=config.kind.isPhoto&&config.photos.isEmpty()
    fun config(config:WidgetConfig)=if(photo(config))config.copy(image="study/hadley",images=listOf("study/hadley","study/earth","study/hadley"),caption=config.caption.ifBlank{if(config.kind==WidgetKind.POLAROID)"Hadley, 1971" else ""}) else config
    fun data(context:Context,config:WidgetConfig,data:RenderData)=if(config.kind.isMusic&&music(data))data.copy(media=MediaInfo("A quiet moment","Sample artwork · Widget Lab",ArtCache.get(context,if(config.kind in listOf(WidgetKind.MATRIX,WidgetKind.GLASS,WidgetKind.COMPACT,WidgetKind.CASSETTE))"study/earth" else "study/hadley"),previous=true,next=true,toggle=true),mediaAccess=true) else data
}
