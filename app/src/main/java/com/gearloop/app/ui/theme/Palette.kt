package com.gearloop.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The raw colour values, one set per theme.
 *
 * Nothing reads this directly. Screens go through [GearColor], [GearSurface] and
 * [GearText], which resolve against whichever palette is in scope — that is what
 * lets a single call site render correctly in both themes.
 *
 * ## What moves between light and dark, and what doesn't
 *
 * **Hi-vis does not move.** It marks action and safety, and safety information
 * should not depend on which theme someone happens to be in. The same steps
 * appear in both palettes, and text on hi-vis stays pine/900 in both — so the
 * 11.2:1 contrast rule holds without a second calculation.
 *
 * **Chalk inverts, pine lifts.** In light, surfaces come from the chalk end and
 * text from pine. In dark they trade places: surfaces are drawn from a deepened
 * pine, text from chalk. The ramps keep their names because they keep their jobs.
 *
 * **The semantic and fit ramps lighten rather than invert.** Success/500 and
 * danger/500 as drawn for a white card do not clear 4.5:1 on a near-black one,
 * so the dark palette carries brighter variants of the same hues.
 *
 * **Anything sitting on a photograph stays put.** See [GearPalette.overImage].
 */
data class GearPalette(
    val isDark: Boolean,

    // Pine — structure
    val pine900: Color,
    val pine800: Color,
    val pine700: Color,
    val pine600: Color,
    val pine500: Color,

    // Chalk — neutrals
    val chalk000: Color,
    val chalk50: Color,
    val chalk100: Color,
    val chalk200: Color,
    val chalk300: Color,
    val chalk400: Color,
    val chalk500: Color,
    val chalk600: Color,
    val chalk700: Color,

    // Hi-vis — action and safety. Identical in both palettes.
    val hiVis600: Color,
    val hiVis500: Color,
    val hiVis400: Color,
    val hiVis200: Color,
    val hiVis100: Color,

    // Marine — informational
    val marine700: Color,
    val marine500: Color,
    val marine300: Color,
    val marine100: Color,

    /**
     * Semantic, in three steps: `100` is the tint a banner is filled with, `500`
     * is the accent — the spine and the icon — and `700` is the ink written on
     * the tint.
     *
     * The `700` step exists because an accent bright enough to read as a colour
     * is rarely dark enough to read as text on its own tint: amber at `500` on
     * amber at `100` measures 2.6:1. Marine always had this shape; the other
     * three were reusing `500` for both jobs and quietly failing.
     */
    val success700: Color,
    val success500: Color,
    val success100: Color,
    val warning700: Color,
    val warning500: Color,
    val warning100: Color,
    val danger700: Color,
    val danger600: Color,
    val danger500: Color,
    val danger100: Color,

    /**
     * Trust tiers — a metal scale of its own, in the same two steps the semantic
     * ramps use: the metal is the ring drawn round an avatar, the ink is the
     * word written inside the badge.
     *
     * They are separate for the reason amber is: a colour chosen to look like
     * the metal is nowhere near dark enough to be read as text. Silver measures
     * 3.3:1 on a white card and gold 2.9:1 — both were being used as the label
     * on `TrustBadge`, and the trust screen went further and wrote the tier in
     * hi-vis/600, which is 1.7:1 and effectively invisible.
     */
    val tierBronze: Color,
    val tierSilver: Color,
    val tierGold: Color,
    val tierBronzeInk: Color,
    val tierSilverInk: Color,
    val tierGoldInk: Color,

    // Camera viewport. Already near-black, so it barely moves.
    val viewport: Color,
    val viewportChip: Color,

    // Fit ramp
    val fitPerfect: Color,
    val fitGood: Color,
    val fitWorkable: Color,
    val fitPoor: Color,
    val fitBlocked: Color,

    // Semantic surface slots
    val surfaceApp: Color,
    val surfaceCard: Color,
    val surfaceSunken: Color,
    val surfaceDark: Color,
    val surfaceDarkest: Color,
    val surfaceBorder: Color,
    val surfaceBorderStrong: Color,

    /**
     * The light chip drawn over a photograph or a camera feed, and the ink on it.
     *
     * Identical in both palettes on purpose: a photograph does not get darker
     * when someone turns on dark mode, so a save button sitting on one must not
     * either. Same reasoning as hi-vis, arrived at from the other direction.
     */
    val overImage: Color,
    val onOverImage: Color,

    /** The semantic pair used on [overImage], which is always a light surface. */
    val successOnImage: Color,
    val dangerOnImage: Color,

    // Semantic text slots
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textOnDark: Color,
    val textOnDarkMuted: Color,
    val textOnHiVis: Color,
)

/** The palette the app was designed in. */
val LightPalette = GearPalette(
    isDark = false,

    pine900 = Color(0xFF0A211C),
    pine800 = Color(0xFF10352E),
    pine700 = Color(0xFF17493F),
    pine600 = Color(0xFF206153),
    pine500 = Color(0xFF2C7D6B),

    chalk000 = Color(0xFFFFFFFF),
    chalk50 = Color(0xFFF7F8F5),
    chalk100 = Color(0xFFEDEFEA),
    chalk200 = Color(0xFFDFE2DB),
    chalk300 = Color(0xFFC7CCC2),
    chalk400 = Color(0xFFA2A99C),
    chalk500 = Color(0xFF7C8478),
    chalk600 = Color(0xFF5A6256),
    chalk700 = Color(0xFF3E453B),

    hiVis600 = Color(0xFFB8C400),
    hiVis500 = Color(0xFFD6E32E),
    hiVis400 = Color(0xFFE4EF5C),
    hiVis200 = Color(0xFFF1F8A8),
    hiVis100 = Color(0xFFF8FBD8),

    marine700 = Color(0xFF0E4A5C),
    marine500 = Color(0xFF17708A),
    marine300 = Color(0xFF7FB6C6),
    marine100 = Color(0xFFE1F0F4),

    // Ink on tint measures 4.5:1 or better; the 500s stay as drawn.
    success700 = Color(0xFF1A6B4A),
    success500 = Color(0xFF1E7A55),
    success100 = Color(0xFFE4F2EB),
    // Amber is the one hue that cannot carry text at the weight it was drawn:
    // #C98A17 measures 2.9:1 even on pure white. The accent darkens just enough
    // to work as a graphic, and text goes a step further to 700.
    warning700 = Color(0xFF916311),
    warning500 = Color(0xFFB87E15),
    warning100 = Color(0xFFFAF0DC),
    danger700 = Color(0xFFB03428),
    danger600 = Color(0xFFA22F25),
    danger500 = Color(0xFFC33A2E),
    danger100 = Color(0xFFFAE7E5),

    tierBronze = Color(0xFF9C6B3F),
    tierSilver = Color(0xFF7C8478),
    tierGold = Color(0xFFB8860B),
    // Each metal taken down until it clears 4.5:1 both on a white card and on
    // its own 14 %-alpha tint, which is the quieter of the two.
    tierBronzeInk = Color(0xFF7A5230),
    tierSilverInk = Color(0xFF565C52),
    tierGoldInk = Color(0xFF7E5905),

    viewport = Color(0xFF071713),
    viewportChip = Color(0xFF0E2A24),

    // The fit stamp draws its border and its label in one colour, so the label
    // sets the bar: 4.5:1 against the app surface, the quietest background it
    // sits on. Only good and workable had to move; the rest already cleared it.
    fitPerfect = Color(0xFF1E7A55),
    fitGood = Color(0xFF3D775C),
    fitWorkable = Color(0xFF916311),
    fitPoor = Color(0xFFC33A2E),
    fitBlocked = Color(0xFF5A6256),

    surfaceApp = Color(0xFFEDEFEA),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF7F8F5),
    surfaceDark = Color(0xFF10352E),
    surfaceDarkest = Color(0xFF0A211C),
    surfaceBorder = Color(0xFFDFE2DB),
    surfaceBorderStrong = Color(0xFFC7CCC2),

    overImage = Color(0xFFFFFFFF),
    onOverImage = Color(0xFF0A211C),
    successOnImage = Color(0xFF1E7A55),
    dangerOnImage = Color(0xFFC33A2E),

    textPrimary = Color(0xFF0A211C),
    textBody = Color(0xFF3E453B),
    textSecondary = Color(0xFF5A6256),
    // A step darker than chalk/500, which it used to borrow. Tertiary carries
    // timestamps and captions at small sizes, and chalk/500 measured 3.3:1 on
    // the app surface. Chalk/500 itself is unchanged — it still does the icon
    // and inactive-state work, where 3:1 is the bar.
    textTertiary = Color(0xFF686E64),
    textPlaceholder = Color(0xFFA2A99C),
    textOnDark = Color(0xFFFFFFFF),
    textOnDarkMuted = Color(0xFFC7CCC2),
    textOnHiVis = Color(0xFF0A211C),
)

/**
 * The dark palette.
 *
 * Built by asking what each ramp step is *for* rather than by inverting numbers.
 * `chalk000` means "the brightest neutral surface" in light and "the brightest
 * neutral ink" in dark, so it flips. `pine800` means "a dark structural surface"
 * in both, so in dark it only lifts enough to sit above the page behind it.
 */
val DarkPalette = GearPalette(
    isDark = true,

    // Pine's darkest step was carrying primary text, so in dark it becomes ink.
    // The rest stay structural and lift just enough to separate from the page.
    pine900 = Color(0xFFE8EDE9),
    pine800 = Color(0xFF164137),
    pine700 = Color(0xFF1D5447),
    pine600 = Color(0xFF2A6D5D),
    pine500 = Color(0xFF3C9280),

    // Chalk inverts: it is the ink ramp now, and chalk000 is the deepest surface.
    chalk000 = Color(0xFF07130F),
    chalk50 = Color(0xFF0B1A16),
    chalk100 = Color(0xFF0E211B),
    chalk200 = Color(0xFF1B3A32),
    chalk300 = Color(0xFF2A4E44),
    chalk400 = Color(0xFF5E7168),
    chalk500 = Color(0xFF8B978D),
    chalk600 = Color(0xFFA9B3A9),
    chalk700 = Color(0xFFCBD2CA),

    // Unchanged. Hi-vis is the safety colour and does not follow the theme.
    // Only its two tint containers darken, because they are surfaces.
    hiVis600 = Color(0xFFB8C400),
    hiVis500 = Color(0xFFD6E32E),
    hiVis400 = Color(0xFFE4EF5C),
    hiVis200 = Color(0xFF4A5410),
    hiVis100 = Color(0xFF2E340A),

    // Marine lightens so links stay legible; its container darkens.
    marine700 = Color(0xFF9CD2E2),
    marine500 = Color(0xFF5FB3CB),
    marine300 = Color(0xFF3A7C91),
    marine100 = Color(0xFF12313C),

    // Lightened, not inverted: same hues, enough luminance for a dark card.
    // On a dark tint the accent is already legible as ink, so 700 and 500 meet.
    success700 = Color(0xFF4FBF8F),
    success500 = Color(0xFF4FBF8F),
    success100 = Color(0xFF12312A),
    warning700 = Color(0xFFE0A93F),
    warning500 = Color(0xFFE0A93F),
    warning100 = Color(0xFF3A2E12),
    danger700 = Color(0xFFE4685A),
    danger600 = Color(0xFFF08A7E),
    danger500 = Color(0xFFE4685A),
    danger100 = Color(0xFF3D1D1A),

    tierBronze = Color(0xFFC79463),
    tierSilver = Color(0xFFA9B3A9),
    tierGold = Color(0xFFD9AE3C),
    // On a dark tint the metal is already legible as ink, so the two steps meet
    // — the same thing success, warning and danger do in this palette.
    tierBronzeInk = Color(0xFFC79463),
    tierSilverInk = Color(0xFFA9B3A9),
    tierGoldInk = Color(0xFFD9AE3C),

    // The camera screens were already near-black; they barely move.
    viewport = Color(0xFF040C0A),
    viewportChip = Color(0xFF0B211C),

    fitPerfect = Color(0xFF4FBF8F),
    fitGood = Color(0xFF6FC9A0),
    fitWorkable = Color(0xFFE0A93F),
    fitPoor = Color(0xFFE4685A),
    fitBlocked = Color(0xFF8B978D),

    surfaceApp = Color(0xFF07130F),
    surfaceCard = Color(0xFF0E211B),
    surfaceSunken = Color(0xFF0B1A16),
    // These two mean "a deliberately dark surface" in either theme — the cover,
    // the camera screens, a toast. They stay dark; they just stop being a
    // contrast against the page and start being part of it.
    surfaceDark = Color(0xFF164137),
    surfaceDarkest = Color(0xFF0B1A16),
    surfaceBorder = Color(0xFF1F463C),
    surfaceBorderStrong = Color(0xFF2E5A4E),

    // Identical to the light palette. The photograph underneath is the same one.
    overImage = Color(0xFFFFFFFF),
    onOverImage = Color(0xFF0A211C),
    successOnImage = Color(0xFF1E7A55),
    dangerOnImage = Color(0xFFC33A2E),

    textPrimary = Color(0xFFEDF2EE),
    textBody = Color(0xFFCBD2CA),
    textSecondary = Color(0xFFA9B3A9),
    textTertiary = Color(0xFF8B978D),
    textPlaceholder = Color(0xFF6B786F),
    textOnDark = Color(0xFFEDF2EE),
    textOnDarkMuted = Color(0xFFA9B3A9),
    // Unchanged, and deliberately so. See the note on hi-vis above.
    textOnHiVis = Color(0xFF0A211C),
)

/**
 * The wireframe palette — the app rendered as the structural drawing it came from.
 *
 * A third instance of the same shape, which is the whole argument for tokenising
 * colour in the first place: not one screen changes, and every one of the 42 comes
 * out as a wireframe. Photographs are handled separately, in `GearImage`, because
 * a greyed photograph is still a photograph and a wireframe should show a slot.
 *
 * The greys are picked, not computed. Desaturating the real palette by luminance
 * puts hi-vis at 87 % white, which would make every primary button vanish into
 * the card behind it — so the ramps are re-spaced around what each one has to do
 * here: surfaces stay near-white, borders stay visible, and anything that was an
 * action keeps enough weight to read as one.
 *
 * Two things deliberately do NOT collapse to a single grey:
 *  - **The five hi-vis steps stay distinct**, so a primary button, its pressed
 *    state and its tint container are still three different things.
 *  - **The fit ramp keeps five steps**, because the fit stamp encodes its verdict
 *    in colour and a wireframe that flattens it would lose the information.
 *
 * The semantic ramps DO collapse — success, warning and danger share their greys.
 * At wireframe fidelity a banner is a banner; which kind it is comes from its
 * icon and its words, both of which survive.
 */
val WirePalette = GearPalette(
    isDark = false,

    // Structure, darkest to lightest. pine900 carries primary text.
    pine900 = Color(0xFF1A1A1A),
    pine800 = Color(0xFF3D3D3D),
    pine700 = Color(0xFF525252),
    pine600 = Color(0xFF6B6B6B),
    pine500 = Color(0xFF858585),

    chalk000 = Color(0xFFFFFFFF),
    chalk50 = Color(0xFFFAFAFA),
    chalk100 = Color(0xFFF2F2F2),
    chalk200 = Color(0xFFE3E3E3),
    chalk300 = Color(0xFFCCCCCC),
    chalk400 = Color(0xFFABABAB),
    chalk500 = Color(0xFF8A8A8A),
    chalk600 = Color(0xFF666666),
    chalk700 = Color(0xFF454545),

    // Action. Kept as five separate greys so a filled button, its hover and its
    // tint container do not become the same rectangle.
    hiVis600 = Color(0xFFB0B0B0),
    hiVis500 = Color(0xFFCFCFCF),
    hiVis400 = Color(0xFFDCDCDC),
    hiVis200 = Color(0xFFECECEC),
    hiVis100 = Color(0xFFF5F5F5),

    marine700 = Color(0xFF4A4A4A),
    marine500 = Color(0xFF6B6B6B),
    marine300 = Color(0xFFB0B0B0),
    marine100 = Color(0xFFEDEDED),

    // The three semantic ramps share one set of greys — see the note above.
    success700 = Color(0xFF4A4A4A),
    success500 = Color(0xFF707070),
    success100 = Color(0xFFEDEDED),
    warning700 = Color(0xFF4A4A4A),
    warning500 = Color(0xFF707070),
    warning100 = Color(0xFFEDEDED),
    danger700 = Color(0xFF4A4A4A),
    danger600 = Color(0xFF5C5C5C),
    danger500 = Color(0xFF707070),
    danger100 = Color(0xFFEDEDED),

    tierBronze = Color(0xFF8A8A8A),
    tierSilver = Color(0xFF8A8A8A),
    tierGold = Color(0xFF8A8A8A),
    // The three metals share one grey here, the way the semantic ramps do — a
    // wireframe says "there is a tier badge", not which metal it is. The ink
    // step is still separate, because it still has to be readable.
    tierBronzeInk = Color(0xFF4A4A4A),
    tierSilverInk = Color(0xFF4A4A4A),
    tierGoldInk = Color(0xFF4A4A4A),

    // The camera screens stay dark. A viewport is a hole in the page, and in a
    // wireframe that reads better as a dark block than as another white panel.
    viewport = Color(0xFF2B2B2B),
    viewportChip = Color(0xFF3D3D3D),

    // Five steps kept, because the stamp says its verdict in colour.
    //
    // The obvious ramp — an even spread from #3D to #A8 — fails: the stamp draws
    // its 2px border and its label in this one colour, so it is held to the text
    // bar, and #6E is the lightest grey that still clears 4.5:1 on the app
    // surface. The five steps are compressed into what passes rather than spread
    // across what looks even. The verdict is safety information in the wireframe
    // for the same reason it is in the finished screen.
    fitPerfect = Color(0xFF2E2E2E),
    fitGood = Color(0xFF3F3F3F),
    fitWorkable = Color(0xFF505050),
    fitPoor = Color(0xFF5F5F5F),
    fitBlocked = Color(0xFF6E6E6E),

    surfaceApp = Color(0xFFF2F2F2),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEBEBEB),
    surfaceDark = Color(0xFF4A4A4A),
    surfaceDarkest = Color(0xFF333333),
    // A shade stronger than the real palette's. The design separates cards with a
    // 1px border instead of a shadow, and that border is the only thing drawing
    // the layout once the colour is gone.
    surfaceBorder = Color(0xFFCFCFCF),
    surfaceBorderStrong = Color(0xFF9E9E9E),

    overImage = Color(0xFFFFFFFF),
    onOverImage = Color(0xFF1A1A1A),
    successOnImage = Color(0xFF4A4A4A),
    dangerOnImage = Color(0xFF4A4A4A),

    textPrimary = Color(0xFF1A1A1A),
    textBody = Color(0xFF333333),
    textSecondary = Color(0xFF5C5C5C),
    // #6A is the ceiling, set by the sunken panel rather than the page: tertiary
    // carries timestamps and captions at small sizes, so it is held to the body
    // bar, and the quietest surface it sits on is the one that decides. The same
    // trap the light palette fell into when tertiary borrowed chalk/500.
    textTertiary = Color(0xFF6A6A6A),
    textPlaceholder = Color(0xFFA6A6A6),
    textOnDark = Color(0xFFFFFFFF),
    textOnDarkMuted = Color(0xFFCFCFCF),
    textOnHiVis = Color(0xFF1A1A1A),
)
