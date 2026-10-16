package dev.widgetlab.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import dev.widgetlab.core.ConfigStore
import dev.widgetlab.data.LaunchApps
import dev.widgetlab.widgets.ProviderCatalog

/** Resolve at tap time so an uninstalled application immediately falls back to configuration. */
class ShortcutLaunchActivity:Activity(){
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
        val kind=ProviderCatalog.kind(this,id)
        if(kind==null||!kind.hasApps){startActivity(Intent(this,MainActivity::class.java));finish();return}
        val cfg=ConfigStore(this).read(id,kind)
        val component=cfg.apps.getOrNull(intent.getIntExtra("index",-1))
        val destination=component?.let{LaunchApps.intent(this,it)}
        val edit=Intent(this,WidgetConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)
        runCatching{startActivity(destination ?: edit)}.onFailure{startActivity(edit)}
        finish()
    }
}
