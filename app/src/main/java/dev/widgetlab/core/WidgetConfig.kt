package dev.widgetlab.core

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.ZoneId
import java.util.UUID

/** One immutable profile per appWidgetId. Never share mutable settings between instances. */
data class WidgetConfig(
    val kind: WidgetKind,
    val light: Boolean = kind.defaultLight,
    val accent: String = "#C3D8B0",
    val selection: String = if(kind.hasPortrait && kind!=WidgetKind.CONSTRUCTOR) "hamilton" else "",
    val timezone: String = "device",
    val hour24: Boolean = true,
    val rows: Int = 5,
    val image: String = "",
    val crop: String = "center",
    val typography: String = "design",
    val surface: String = if(kind.transparentDefault) "transparent" else if(kind==WidgetKind.GLASS || kind==WidgetKind.GLASS_CLOCK) "glass" else "solid",
    val secondaryZone: String = "Europe/London",
    val showDate: Boolean = true,
    val showWeekday: Boolean = true,
    val alignment: String = "design",
    val caption: String = "",
    val label: String = if(kind==WidgetKind.SECTION) "THE EVERYDAY" else "Essentials",
    val images: List<String> = emptyList(),
    val apps: List<String> = emptyList(),
    val corners: Int = 24,
    val accentText: Boolean = false,
    val iconStyle: String = if(kind==WidgetKind.GRID)"mono" else if(kind==WidgetKind.LAUNCHER)"letters" else "original",
    val showLabels: Boolean = true
) {
    fun zone(): ZoneId = zone(timezone)
    fun secondZone(): ZoneId = zone(secondaryZone)
    private fun zone(value: String): ZoneId = runCatching { if(value=="device") ZoneId.systemDefault() else ZoneId.of(value) }.getOrDefault(ZoneId.systemDefault())
    val photos get() = (if(images.isEmpty()) listOf(image) else images).filter{it.isNotBlank()}.take(6)
    fun json(): String = JSONObject().put("kind",kind.name).put("light",light).put("accent",accent)
        .put("selection",selection).put("timezone",timezone).put("hour24",hour24).put("rows",rows)
        .put("image",image).put("crop",crop).put("typography",typography).put("surface",surface)
        .put("secondaryZone",secondaryZone).put("showDate",showDate).put("showWeekday",showWeekday)
        .put("alignment",alignment).put("caption",caption).put("label",label).put("corners",corners)
        .put("iconStyle",iconStyle).put("showLabels",showLabels).put("accentText",accentText).put("images",org.json.JSONArray(images)).put("apps",org.json.JSONArray(apps)).toString()
    companion object {
        fun parse(value: String?, fallback: WidgetKind): WidgetConfig = runCatching {
            val j=JSONObject(value ?: "{}"); val old=j.optString("kind")
            val k=WidgetKind.entries.firstOrNull{it.name==old} ?: if(old=="AGENDA") WidgetKind.DATE else fallback
            val defaults=WidgetConfig(k)
            fun list(key: String)=j.optJSONArray(key)?.let{a->(0 until a.length()).map{a.optString(it)}.filter{it.isNotBlank()}.take(6)} ?: emptyList()
            defaults.copy(light=j.optBoolean("light",defaults.light),
                accent=j.optString("accent",defaults.accent).takeIf{it.matches(Regex("#[0-9a-fA-F]{6}"))} ?: defaults.accent,
                selection=j.optString("selection",defaults.selection),timezone=j.optString("timezone","device"),
                hour24=j.optBoolean("hour24",true),rows=j.optInt("rows",5).coerceIn(3,10),
                image=j.optString("image"),crop=j.optString("crop","center").takeIf{it in listOf("center","top","bottom","left","right")} ?: "center",
                typography=j.optString("typography","design").takeIf{it in listOf("design","sans","condensed","serif","technical","mono","dots","editorial")} ?: "design",
                surface=j.optString("surface",if(j.has("kind")&&j.has("light")&&!j.has("surface"))"solid" else defaults.surface).takeIf{it in listOf("transparent","solid","tint","glass","outline","semi")} ?: defaults.surface,
                secondaryZone=j.optString("secondaryZone","Europe/London"),showDate=j.optBoolean("showDate",true),showWeekday=j.optBoolean("showWeekday",true),
                alignment=j.optString("alignment","design"),caption=j.optString("caption").take(120),label=j.optString("label",defaults.label).take(60),
                images=list("images"),apps=list("apps"),corners=j.optInt("corners",24).coerceIn(0,40),accentText=j.optBoolean("accentText",false),
                iconStyle=j.optString("iconStyle",defaults.iconStyle).takeIf{it in listOf("original","mono","letters")} ?: defaults.iconStyle,showLabels=j.optBoolean("showLabels",true))
        }.getOrDefault(WidgetConfig(fallback))
    }
}

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("widget-profiles-v1", Context.MODE_PRIVATE)
    fun contains(id: Int) = prefs.contains("widget:$id")
    fun read(id: Int, kind: WidgetKind) = WidgetConfig.parse(prefs.getString("widget:$id", null), kind)
    fun save(id: Int, config: WidgetConfig) { check(prefs.edit().putString("widget:$id", config.json()).commit()) }
    fun delete(id: Int) { prefs.edit().remove("widget:$id").apply() }
    fun pending(config: WidgetConfig): String {
        val token = UUID.randomUUID().toString()
        // Keep only recent pin requests; a cancelled launcher request has no callback.
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("pin:") }.forEach { editor.remove(it) }
        check(editor.putString("pin:$token", config.json()).putLong("pin-time", System.currentTimeMillis()).commit())
        return token
    }
    fun pendingConfig(kind: WidgetKind): WidgetConfig? {
        if (System.currentTimeMillis() - prefs.getLong("pin-time", 0) > 300_000) return null
        return prefs.all.keys.firstOrNull { it.startsWith("pin:") }?.let {
            WidgetConfig.parse(prefs.getString(it, null), kind).takeIf { it.kind == kind }
        }
    }
    fun consume(token: String): WidgetConfig? {
        val key = "pin:$token"; val value = prefs.getString(key, null) ?: return null
        prefs.edit().remove(key).commit()
        return WidgetConfig.parse(value, WidgetKind.CLOCK)
    }
    fun remember(config: WidgetConfig) { prefs.edit().putString("recent:${config.kind.name}", config.json()).putLong("used:${config.kind.name}", System.currentTimeMillis()).apply() }
    fun recent(): List<WidgetConfig> = prefs.all.keys.filter { it.startsWith("recent:") }
        .sortedByDescending { prefs.getLong("used:${it.substringAfter(':')}", 0) }
        .take(4).map { WidgetConfig.parse(prefs.getString(it, null), WidgetKind.CLOCK) }
}

fun Context.artFile(name: String): File? = name.takeIf { it.matches(Regex("[a-f0-9-]+\\.(jpg|png)")) }?.let { File(filesDir, "art/$it") }
