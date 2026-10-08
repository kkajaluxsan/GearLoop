package com.gearloop.app.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gearloop.app.ui.theme.GearLoopTheme
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.Space

/**
 * Previews for the component library.
 *
 * The library is the deliverable, so it has to be inspectable without building
 * and installing anything: open a component file in Android Studio and every
 * variant renders in the split pane. That is also the cheapest accessibility
 * check there is — [GearTypePreview] draws each component again at 200 % font
 * scale, which is where fixed-height rows give themselves away.
 *
 * Previews are `private` and take no parameters, so they are dead code to
 * everything except the tooling and R8 strips them from the release build.
 */

/**
 * Light and dark, side by side.
 *
 * The default for a component, because both themes ship and a token that reads
 * in one is not evidence it reads in the other. This is where the dark palette
 * actually gets reviewed — 26 components in a split pane rather than 42 screens
 * walked twice on a device.
 */
@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFEDEFEA, widthDp = 380)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF07130F,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class GearPreview

/** Dark only, for the few things worth looking at on their own. */
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF07130F,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class GearDarkPreview

/** Light, dark, and 200 % font scale — the three ways a component breaks. */
@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFEDEFEA, widthDp = 380)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF07130F,
    widthDp = 380,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "Text 200%",
    showBackground = true,
    backgroundColor = 0xFFEDEFEA,
    widthDp = 380,
    fontScale = 2f,
)
annotation class GearTypePreview

/** Full-height, for anything that needs the whole screen to make sense. */
@Preview(name = "Screen", showBackground = true, backgroundColor = 0xFFEDEFEA, widthDp = 380, heightDp = 800)
annotation class GearScreenPreview

/**
 * The frame every preview sits in: the real theme, the app surface, and the
 * screen margin, so nothing is judged against a white void it never appears on.
 */
@Composable
fun PreviewFrame(
    background: androidx.compose.ui.graphics.Color = GearSurface.app,
    padded: Boolean = true,
    wireframe: Boolean = false,
    blockText: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    GearLoopTheme(wireframe = wireframe, blockText = blockText) {
        Column(
            Modifier
                .background(background)
                .then(if (padded) Modifier.padding(Space.screen) else Modifier),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
            content = content,
        )
    }
}
