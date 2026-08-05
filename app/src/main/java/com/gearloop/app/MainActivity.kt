package com.gearloop.app

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
        val wireframe = intent?.getBooleanExtra("wire", false) ?: false

        setContent {
            // Follows the system, with an override the cover can drive — a demo
            // should never depend on someone finding the device settings.
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val dark = darkOverride ?: isSystemInDarkTheme()

            GearLoopTheme(darkTheme = dark, wireframe = wireframe) {
                GearLoopApp(
                    modifier = Modifier.fillMaxSize().background(GearSurface.app),
                    startDestination = start,
                    darkTheme = dark,
                    onToggleTheme = { darkOverride = !dark },
                )
            }
        }
    }
}
