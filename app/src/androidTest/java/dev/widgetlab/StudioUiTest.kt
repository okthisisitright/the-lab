package dev.widgetlab

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.widgetlab.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import java.io.File

class StudioUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name:String){
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        // A single shell command avoids temporary app storage removed by runner cleanup.
        val screenshot=instrumentation.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/widgetlab-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(screenshot).use{it.readBytes()}
    }
    @Test fun galleryOpensActualClockConfigurationAndChangesTransparency(){
        compose.onNodeWithText("Make space.").assertIsDisplayed()
        screenshot("01-studio-home")
        compose.onNodeWithContentDescription("Minimal clock actual renderer preview").performScrollTo().performClick()
        compose.onNodeWithText("Minimal clock").assertIsDisplayed()
        screenshot("02-clock-detail")
        compose.onNodeWithTag("design-settings").performScrollToNode(hasText("Background"))
        compose.onNodeWithText("Solid").performScrollTo().performClick()
        compose.onNodeWithText("None").performScrollTo().performClick()
        compose.onNodeWithText("Add to home screen").assertIsDisplayed()
        compose.onNodeWithText("←").performClick()
        compose.onNodeWithText("Music").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Glass mini actual renderer preview").performScrollTo().assertIsDisplayed()
        screenshot("03-music-gallery")
    }
}
