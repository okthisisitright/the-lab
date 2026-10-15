package dev.widgetlab.core

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.time.ZoneId
import java.util.UUID

/** One immutable profile per appWidgetId. Never share mutable settings between instances. */
data class WidgetConfig(
    val kind: WidgetKind,
    val light: Boolean = kind == WidgetKind.WEEKEND || kind == WidgetKind.VINYL || kind == WidgetKind.SWISS,
    val accent: String = "#D8FF62",
    val selection: String = if(kind == WidgetKind.DRIVER) "hamilton" else "",
    val timezone: String = "device",
    val hour24: Boolean = true,
    val rows: Int = 5,
    val image: String = "",
    val crop: String = "center",
    val typography: String = "editorial"
) {
    fun zone(): ZoneId = runCatching { if (timezone == "device") ZoneId.systemDefault() else ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
    fun json(): String = JSONObject().put("kind", kind.name).put("light", light).put("accent", accent)
        .put("selection", selection).put("timezone", timezone).put("hour24", hour24).put("rows", rows)
        .put("image", image).put("crop", crop).put("typography", typography).toString()
    companion object {
        fun parse(value: String?, fallback: WidgetKind): WidgetConfig = runCatching {
            val j = JSONObject(value ?: "{}"); val k = WidgetKind.entries.firstOrNull { it.name == j.optString("kind") } ?: fallback
            WidgetConfig(k, j.optBoolean("light", WidgetConfig(k).light),
                j.optString("accent", "#D8FF62").takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: "#D8FF62",
                j.optString("selection", WidgetConfig(k).selection), j.optString("timezone", "device"), j.optBoolean("hour24", true),
                j.optInt("rows", 5).coerceIn(3, 10), j.optString("image"),
                j.optString("crop", "center"), j.optString("typography", "editorial"))
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

fun Context.artFile(name: String): File? = name.takeIf { it.matches(Regex("[a-f0-9-]+\\.jpg")) }?.let { File(filesDir, "art/$it") }
