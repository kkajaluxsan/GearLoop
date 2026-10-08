package com.gearloop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.gearloop.app.nav.Catalogue
import com.gearloop.app.nav.GearLoopApp
import com.gearloop.app.nav.Routes
import com.gearloop.app.ui.theme.DarkPalette
import com.gearloop.app.ui.theme.GearLoopTheme
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.LightPalette

class MainActivity : ComponentActivity() {

    // The two drawing flags are held here rather than read once in setContent, so
    // that coming back to the app can switch them off (see onRestart and
    // onSaveInstanceState).
    private val wireframe = mutableStateOf(false)
    private val blockText = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge. The bar scrims are transparent and the icon appearance is
        // set per screen by SystemBarIcons, because three screens are deliberately
        // dark inside an otherwise light app — and now the whole app can be dark
        // too, so a single decision at launch could never have been right.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                LightPalette.chalk100.toArgb(),
                DarkPalette.surfaceApp.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                LightPalette.chalk000.toArgb(),
                DarkPalette.surfaceCard.toArgb(),
            ),
        )
        super.onCreate(savedInstanceState)

        // Lets a specific screen be opened directly, which is useful when an
        // examiner asks for one by number:
        //   adb shell am start -n com.gearloop.app/.MainActivity --es screen s21
        val start = intent?.getStringExtra("screen")
            ?.takeIf { route -> route in Catalogue.allRoutes }
            ?: Routes.COVER

        // Renders the whole app through WirePalette, with photographs drawn as
        // empty slots. Used to produce the wireframe set from the same source the
        // screens themselves come from, so the two can never drift apart:
        //   adb shell am start -n com.gearloop.app/.MainActivity --ez wire true
        //
        // Only a fresh launch reads the flags from the intent. A launch from Recents
        // replays the intent that first started the task, extras included, and an
        // activity rebuilt after a font-size change or a process death would read it
        // again too: after a wireframe capture, either brought the live demo back as
        // grey boxes. A rebuild takes the flags from its saved state instead.
        val fresh = savedInstanceState == null &&
            ((intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
        wireframe.value = if (fresh) intent?.getBooleanExtra("wire", false) == true
        else savedInstanceState?.getBoolean(KEY_WIRE) == true

        // Greeks every string into a bar, which is the other half of the wireframe
        // convention — a reviewer looking at a wireframe should be reading the
        // structure, not the copy. Independent of `wire`, so either can be used
        // alone, though the wireframe set is captured with both:
        //   adb shell am start -n com.gearloop.app/.MainActivity \
        //     --es screen s07 --ez wire true --ez blocks true
        blockText.value = if (fresh) intent?.getBooleanExtra("blocks", false) == true
        else savedInstanceState?.getBoolean(KEY_BLOCKS) == true

        setContent {
            // Follows the system, with an override the cover can drive — a demo
            // should never depend on someone finding the device settings.
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val dark = darkOverride ?: isSystemInDarkTheme()

            GearLoopTheme(darkTheme = dark, wireframe = wireframe.value, blockText = blockText.value) {
                GearLoopApp(
                    modifier = Modifier.fillMaxSize().background(GearSurface.app),
                    startDestination = start,
                    darkTheme = dark,
                    onToggleTheme = { darkOverride = !dark },
                )
            }
        }
    }

    /**
     * Wireframe mode is for the capture scripts, which cold-start every screen and
     * never leave the app. Tapping the launcher icon only brings the old task to the
     * front, with no new intent to read, so without this the app stayed a wireframe
     * until someone thought to force-stop it. Any return from the background now
     * puts the real app back.
     */
    override fun onRestart() {
        super.onRestart()
        wireframe.value = false
        blockText.value = false
    }

    /**
     * Kept through a configuration change while the app is on screen. Saved as off
     * otherwise, because the next time this activity is rebuilt from it, someone is
     * coming back to the app.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WIRE, isChangingConfigurations && wireframe.value)
        outState.putBoolean(KEY_BLOCKS, isChangingConfigurations && blockText.value)
    }

    private companion object {
        const val KEY_WIRE = "gearloop.wire"
        const val KEY_BLOCKS = "gearloop.blocks"
    }
}
