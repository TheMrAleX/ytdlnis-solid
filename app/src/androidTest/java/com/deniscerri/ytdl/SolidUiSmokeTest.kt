package com.deniscerri.ytdl

import android.graphics.Bitmap
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SolidUiSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "solid-previews")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun navigationAndNativeComponents() {
        PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext).edit()
            .putBoolean("welcome_completed", true)
            .putBoolean("update_app", false).putBoolean("auto_update_ytdlp", false)
            .putBoolean("check_clipboard_home", false).putString("ytdlnis_theme", "Light").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.solid_add_link)).check(matches(isDisplayed()))
            screenshot("01-home")
            onView(withId(R.id.solid_library_shortcut)).perform(click())
            onView(withId(R.id.history_toolbar)).check(matches(isDisplayed()))
            screenshot("02-library")
            scenario.onActivity { it.findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.downloadQueueMainFragment }
            onView(withId(R.id.downloads_toolbar)).check(matches(isDisplayed()))
            screenshot("03-queue")
            scenario.onActivity { it.findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.moreFragment }
            onView(withId(R.id.app_icon)).check(matches(isDisplayed()))
            screenshot("04-tools")
            scenario.onActivity { it.findViewById<View>(R.id.settings).performClick() }
            onView(withId(R.id.settings_toolbar)).check(matches(isDisplayed()))
            screenshot("05-settings")
            pressBack()

            // Fixture text is confined to tests. These are the actual production XML components.
            scenario.onActivity { activity ->
                val gallery = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 24, 0, 0)
                    setBackgroundColor(activity.getColor(R.color.solid_canvas))
                }
                val card = activity.layoutInflater.inflate(R.layout.result_card, gallery, false)
                card.findViewById<TextView>(R.id.result_title).text = "A moment worth keeping"
                card.findViewById<TextView>(R.id.author).text = "Solid · native component preview"
                card.findViewById<TextView>(R.id.duration).text = "04:32"
                card.findViewById<View>(R.id.result_image_view).setBackgroundResource(R.drawable.solid_hero)
                gallery.addView(card)
                val format = activity.layoutInflater.inflate(R.layout.format_item, gallery, false)
                format.findViewById<TextView>(R.id.container).text = "MP4"
                format.findViewById<TextView>(R.id.format_note).text = "1080p · Full HD"
                format.findViewById<TextView>(R.id.file_size).text = "42 MB"
                gallery.addView(format)
                gallery.addView(activity.layoutInflater.inflate(R.layout.solid_widget, gallery, false))
                activity.setContentView(gallery)
            }
            screenshot("06-native-components")
            PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext).edit()
                .putString("ytdlnis_theme", "Dark").commit()
            scenario.recreate()
            onView(withId(R.id.solid_add_link)).check(matches(isDisplayed()))
            screenshot("07-home-dark")
        }
    }
}
