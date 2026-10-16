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
        val dir=File(instrumentation.targetContext.filesDir,"visuals").apply{mkdirs()}
        instrumentation.uiAutomation.takeScreenshot()?.let{bitmap->File(dir,"$name.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
    }
    @Test fun galleryOpensActualClockConfigurationAndChangesTransparency(){
        compose.onNodeWithText("THE\nCOLLECTION").assertIsDisplayed()
        screenshot("01-studio-home")
        compose.onNodeWithContentDescription("Time / maison actual renderer preview").performScrollTo().performClick()
        compose.onNodeWithText("Time / maison").assertIsDisplayed()
        screenshot("02-clock-detail")
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Time / maison"))).performScrollToNode(hasText("BACKGROUND"))
        compose.onNodeWithText("Solid").performScrollTo().performClick()
        compose.onNodeWithText("None").performScrollTo().performClick()
        compose.onNodeWithText("Add to Home Screen  +").assertIsDisplayed()
    }
}
