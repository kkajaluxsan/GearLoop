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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.nav.Catalogue
import com.gearloop.app.nav.GearLoopApp
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
        // The system splash shows the gear tag on pine while the process starts;
        // the database is already in memory by the time the activity is created,
        // so it hands straight over to the app's own splash.
        installSplashScreen()

        // Edge to edge. The bar scrims are transparent and the icon appearance is
        // set per screen by SystemBarIcons, because some screens are deliberately
        // dark inside an otherwise light app.
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

        // Debug builds can open one screen directly, which is how the functional
        // tests drive the app:
        //   adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21
        // A release build always starts at the beginning — sign-in included.
        val start = if (BuildConfig.DEBUG) {
            intent?.getStringExtra("screen")?.takeIf { route -> route in Catalogue.allRoutes }
        } else {
            null
        }

        // The structural drawing of any screen, used to produce wireframes from
        // the same source as the screens themselves. Only a fresh launch reads
        // the flags; a launch from Recents or a rebuilt activity draws the app.
        val fresh = savedInstanceState == null &&
            ((intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
        wireframe.value = if (fresh) intent?.getBooleanExtra("wire", false) == true
        else savedInstanceState?.getBoolean(KEY_WIRE) == true
        blockText.value = if (fresh) intent?.getBooleanExtra("blocks", false) == true
        else savedInstanceState?.getBoolean(KEY_BLOCKS) == true

        val session = container.session.session
        setContent {
            // Follows the system unless the member chose light or dark in Settings.
            val state by session.collectAsStateWithLifecycle()
            val dark = when (state.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            GearLoopTheme(darkTheme = dark, wireframe = wireframe.value, blockText = blockText.value) {
                GearLoopApp(
                    modifier = Modifier.fillMaxSize().background(GearSurface.app),
                    startDestination = start,
                )
            }
        }
    }

    /**
     * Wireframe mode is for capture scripts, which cold-start a screen and never
     * leave the app. Any return from the background puts the real app back.
     */
    override fun onRestart() {
        super.onRestart()
        wireframe.value = false
        blockText.value = false
    }

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
