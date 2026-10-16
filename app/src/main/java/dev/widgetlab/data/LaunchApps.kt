package dev.widgetlab.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class LaunchApp(val component:String,val label:String)
object LaunchApps {
    fun list(context:Context):List<LaunchApp> = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        .map{LaunchApp(ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString(),it.loadLabel(context.packageManager).toString())}
        .distinctBy{it.component}.sortedBy{it.label.lowercase()}
    fun intent(context:Context,component:String):Intent? {
        val cn=ComponentName.unflattenFromString(component) ?: return null
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return if(context.packageManager.resolveActivity(intent,0)!=null)intent else null
    }
    fun label(context:Context,component:String)=runCatching{context.packageManager.getActivityInfo(ComponentName.unflattenFromString(component)!!,0).loadLabel(context.packageManager).toString()}.getOrDefault("App unavailable")
    fun icon(context:Context,component:String):Drawable? = runCatching{context.packageManager.getActivityIcon(ComponentName.unflattenFromString(component)!!)}.getOrNull()
}
