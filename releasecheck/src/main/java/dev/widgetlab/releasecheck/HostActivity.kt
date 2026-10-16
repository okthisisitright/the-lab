package dev.widgetlab.releasecheck

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

class HostActivity: Activity() {
    lateinit var container: FrameLayout
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        container=FrameLayout(this)
        container.setBackgroundColor(android.graphics.Color.rgb(64,70,66))
        setContentView(container)
    }
}
