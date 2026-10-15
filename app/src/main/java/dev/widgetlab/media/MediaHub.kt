package dev.widgetlab.media

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.widgetlab.widgets.WidgetUpdates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Metadata lives only in memory. Notification bodies are neither read nor persisted. */
data class MediaInfo(val title: String = "Nothing playing", val artist: String = "Start music in your player", val artwork: Bitmap? = null, val playing: Boolean = false, val previous: Boolean = false, val next: Boolean = false, val toggle: Boolean = false, val packageName: String = "", val open: PendingIntent? = null, val token: MediaSession.Token? = null)

object MediaHub {
    private var app: Context? = null
    private var manager: MediaSessionManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var controllers = emptyList<MediaController>()
    @Volatile private var snapshots = emptyList<MediaInfo>()
    private val revisionState = MutableStateFlow(0)
    val revision = revisionState.asStateFlow()
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { replace(it.orEmpty()) }
    fun permitted(context: Context) = context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(ComponentName(context, MediaListener::class.java))
    fun connect(context: Context) {
        if (Looper.myLooper() != Looper.getMainLooper()) { handler.post { connect(context) }; return }
        app = context.applicationContext
        if (!permitted(context)) { disconnect(); return }
        if (manager == null) {
            manager = context.getSystemService(MediaSessionManager::class.java)
            try { manager?.addOnActiveSessionsChangedListener(listener, ComponentName(context, MediaListener::class.java), handler) }
            catch (_: SecurityException) { disconnect(); return }
        }
        rescan()
    }
    fun rescan() {
        val context = app ?: return
        try { replace(manager?.getActiveSessions(ComponentName(context, MediaListener::class.java)).orEmpty()) }
        catch (_: SecurityException) { disconnect() }
    }
    private fun replace(list: List<MediaController>) {
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        callbacks.clear()
        controllers = list
        list.forEach { controller ->
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = changed()
                override fun onPlaybackStateChanged(state: PlaybackState?) = changed()
                override fun onSessionDestroyed() = rescan()
            }
            controller.registerCallback(cb, handler); callbacks[controller] = cb
        }
        changed()
    }
    private fun changed() {
        snapshots = controllers.map { c ->
            val m = c.metadata; val s = c.playbackState; val actions = s?.actions ?: 0
            val art = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val small = art?.let { if (it.width > 512 || it.height > 512) { val ratio = 512f / maxOf(it.width, it.height); Bitmap.createScaledBitmap(it, (it.width * ratio).toInt().coerceAtLeast(1), (it.height * ratio).toInt().coerceAtLeast(1), true) } else it }
            val playing = s?.state == PlaybackState.STATE_PLAYING || s?.state == PlaybackState.STATE_BUFFERING
            val toggleAction = if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
            MediaInfo(m?.getString(MediaMetadata.METADATA_KEY_TITLE)?.ifBlank { "Untitled track" } ?: "Untitled track", m?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.ifBlank { "Unknown artist" } ?: "Unknown artist", small, playing,
                actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L, actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
                actions and (toggleAction or PlaybackState.ACTION_PLAY_PAUSE) != 0L, c.packageName, c.sessionActivity, c.sessionToken)
        }
        revisionState.value++
        app?.let { WidgetUpdates.request(it, musicOnly = true) }
    }
    fun info(preferred: String = ""): MediaInfo {
        val list=snapshots
        val preferredSessions=list.filter { preferred.isNotBlank() && it.packageName==preferred }
        return preferredSessions.firstOrNull { it.playing } ?: preferredSessions.firstOrNull()
            ?: list.firstOrNull { it.playing } ?: list.firstOrNull() ?: MediaInfo()
    }
    fun apps(): List<String> = snapshots.map { it.packageName }.distinct()
    fun control(context: Context, action: String, preferred: String) {
        if (!permitted(context)) { disconnect(); return }
        connect(context)
        val chosen = info(preferred)
        val controller = controllers.firstOrNull { it.sessionToken == chosen.token } ?: return
        runCatching {
            when (action) {
                "previous" -> if (chosen.previous) controller.transportControls.skipToPrevious()
                "next" -> if (chosen.next) controller.transportControls.skipToNext()
                "toggle" -> if (chosen.toggle) { if (chosen.playing) controller.transportControls.pause() else controller.transportControls.play() }
            }
        }.onFailure { if (it is SecurityException) disconnect() }
    }
    fun disconnect() {
        runCatching { manager?.removeOnActiveSessionsChangedListener(listener) }; manager = null
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }; callbacks.clear()
        controllers = emptyList(); snapshots = emptyList()
        revisionState.value++
        app?.let { WidgetUpdates.request(it, musicOnly = true) }
    }
}

class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() { super.onListenerConnected(); MediaHub.connect(this) }
    override fun onListenerDisconnected() { MediaHub.disconnect(); super.onListenerDisconnected() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { MediaHub.rescan() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { MediaHub.rescan() }
}
