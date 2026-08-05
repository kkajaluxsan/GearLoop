# GearLoop — agent context

Folder-scoped context for GearLoop. The `README.md` is written for a human examiner; this file is for an agent, and exists to prevent the specific damage that is easy to do here.

## What this is

A **UI/UX design deliverable, not an application.** 42 screens, 26 components, three walkable flows, built in Jetpack Compose from a design brief.

There is no backend, no database, no network call, no login, and no persistence. Every screen renders from a fixed dataset in `data/Demo.kt`. It runs offline and starts instantly, which is the point — it gets demonstrated live in a room.

## Constraints (MUST NOT)

- **Never hardcode a colour or a text style.** Colours come from `GearColor` / `GearSurface` / `GearText` (Color.kt), type from `GearType` (Type.kt). A raw `Color(0xFF…)` anywhere outside `Palette.kt` is a defect.
- **A colour cannot be read outside composition.** Every token is a `@Composable @ReadOnlyComposable` getter that resolves against the active `GearPalette`. Two consequences: a `DrawScope` (any `Canvas`) must have its colours read *before* the draw lambda and passed in — see `MapCanvas.kt` for the shape of it — and a domain type can't carry a `Color` in its constructor, which is why `Fit.color` and `TrustTier.accent` are composable extensions in `ui/theme/TokenColors.kt`.
- **Never hardcode a layout dimension or a border width.** Page rhythm comes from `Space`, border weights from `Border`, corners from `Radius` / `GearShape`, shadows from `Elevation`, fixed geometry from `Metrics` (all Metrics.kt). Sub-8dp values that tune the *interior* of a component — the 3.dp under a card title, the 6.dp between an icon and its label — are written literally at the point of use; they are optical corrections, not layout, and naming them would imply a rhythm that isn't there. This is the documented policy, not an oversight: don't "tokenise" them.
- **Never read a system bar height from a constant.** `Metrics` has no `statusBar`; use `statusBarsPadding()` / `navigationBarsPadding()`.
- **Never hand-write a currency or measurement string.** Use `lkr()`, `grouped()`, `signed()`, `km()`, `kmAway()` from `data/Format.kt`. `"LKR ${grouped(x)}"` is `lkr(x)` written the long way. `grouped()` and `signed()` both emit a real minus sign (−, U+2212), not a hyphen — don't "fix" that.
- **Never call `remember { mutableStateOf(…) }` for screen state.** Use `rememberSaveable`, or `rememberSavedStateList` / `rememberSavedScrollState` from `ui/components/Foundation.kt`. State that resets when you navigate back looks like a bug in a live demo. `remember` is still correct for things that genuinely can't be saved — `MutableInteractionSource`, `derivedStateOf`.
- **Never load a photograph with a bare `painterResource`.** Go through `GearImage`, which measures its slot and reaches for the 600 px copy when the full 1200×900 frame would be wasted. See `data/Thumbs.kt`.
- **Never add an interactive modifier without a `role`.** `clickable`/`selectable`/`toggleable` all take one. Colour and stroke weight don't reach a screen reader.
- **Never use Material Icons.** Icons are Lucide, kept as raw SVG path geometry in `ui/icons/Lucide.kt` (202 icons) and drawn through `GearIcon(icon, …, strokeWidth = …)`. There are currently zero `material.icons` imports in the codebase; keep it that way. Stroke weight is how the active tab-bar state is expressed, since Lucide ships no filled variants.
- **Never put a shadow between cards.** Cards are separated by a 1px border. `Elevation` is reserved for things that genuinely float above content (knob, floating card, toast, sticky footer).
- **Never use white text on hi-vis.** Text on hi-vis is always `GearText.onHiVis` (Pine900, 11.9:1).
- **Never use hi-vis decoratively.** It marks action and safety only — the same discipline as a life jacket. **Hi-vis is also identical in both themes**, and `GearText.onHiVis` with it: safety information should not change because someone flipped a setting. Don't "improve" the dark palette by shifting it.
- **Never let a chip that sits on a photograph follow the theme.** A photograph doesn't get darker in dark mode, so the save heart, the pager dots, the shutter ring and the quality badge use `GearSurface.overImage` / `GearText.onOverImage`, which are constant. Using `Chalk000` there is the bug this rule exists to prevent.
- **`Chalk000` means "the brightest surface", not "white".** In dark it is the deepest surface. Ink on a dark chip is `GearText.onDark`; a deliberately dark screen is `GearSurface.darkest`, not `Pine900`.
- **Never add a real photograph of a person.** Users are monogram avatars on a pine chip. Putting real faces behind people who don't exist is the wrong call, and it keeps the people layer quiet so the gear photography carries the screen.
- **Never introduce a map SDK or API key.** The map is drawn procedurally in `ui/components/MapCanvas.kt`.
- **No interactive element below 44.dp** (`Metrics.tapTarget`).
- **Don't add persistence, networking, auth, or a DI framework.** Out of scope by design, not by omission.

## The rule that keeps the app coherent

**If a user could act on the number, it is set in mono.** Prices, sizes, distances, condition grades, item codes, countdowns → `GearType.data*` (JetBrains Mono). Mono aligns in columns and doesn't disguise a `1` as an `l`.

`GearType.dataS` and `GearType.labelXs` are **always rendered UPPERCASE** — pass already-uppercased text or apply it at the call site.

## Conventions

- **`data/Demo.kt` is the single source of truth.** The Torq Mod Fish costs LKR 2,800 a day on all eleven screens it appears on. If you change a value, change it there — never at a call site.
- **Assemble from the existing 26 components** in `ui/components/` (Buttons, Cards, Inputs, Feedback, Navigation, Foundation, Signature, MapCanvas). Nothing is drawn twice. Read the component before writing a new one.
- **Every component has previews at the bottom of its file.** Add one for anything new, using `@GearPreview` or `@GearTypePreview` from `ui/preview/GearPreview.kt` and the `PreviewFrame` wrapper. `@GearPreview` renders light *and* dark; `@GearTypePreview` adds 200 % font scale. Between them they are how both themes actually get reviewed — 26 components in a split pane beats 42 screens walked twice — so use `@GearTypePreview` for anything with a fixed height.
- **The three signature elements** — `GearTag`, `FitStamp`, `FairnessMeter` (`ui/components/Signature.kt`) — carry the concept. Treat them as load-bearing, not decoration. All three carry hand-written semantics; if you change what they display, change what they announce.
- **Adding a screen takes two edits:** register a `ScreenSpec` in `Catalogue` (`nav/Destinations.kt`) *and* add a `composable(...)` route in `nav/GearLoopApp.kt` (51 routes today). The catalogue drives the in-app screen index and the flow walkthroughs — a route registered in only one place is half-added. `VerifyCatalogue` in `GearLoopApp.kt` checks both directions on launch and fails the debug build if they disagree, so getting this wrong is loud rather than silent.
- **A flow's captions live in the `Catalogue`, once.** `Flow` / `FlowStep` in `nav/Destinations.kt` carry the sentence to say at each stop; both the journey board and the guided demo bar read them from there. `VerifyCatalogue` fails on a blank caption, because an empty bar in front of an audience is the failure mode.
- **The guided bar is chrome, not product.** `nav/GuidedBar.kt` draws on `DarkPalette` regardless of the app's theme, on purpose — nobody in the room should mistake presentation furniture for a screen someone designed.
- **Resetting the demo means remounting.** `DemoState.generation` keys the `NavHost`; bumping it throws away every screen's `rememberSaveable`. That is the only thing that puts the fairness slider back where it was designed.
- **Screens that scroll through content use `GearLazyScreen`;** screens that are a fixed set of sections use `GearScreen`. The lazy shell exists because a row usually carries a photograph, and the eager one composes all of them at once.
- **Navigation goes through the `Nav` wrapper** (`nav/GearLoopApp.kt:29`), not a raw `NavHostController`.
- Screen transitions are 250 ms, sheets 300 ms.
- Shapes are applied per component from `Radius` tokens, deliberately *not* through Material's shape slots, so nothing inherits a default corner.

## Locked decisions — do not re-litigate

- **Both themes ship.** Dark mode was previously a named scope cut; it is now built. `GearPalette` in `ui/theme/Palette.kt` holds **three** instances of one shape — `LightPalette`, `DarkPalette` and `WirePalette` — and `LocalPalette` decides which is in scope. The app follows the system setting, with an override on the cover. If you are adding a colour, add it to *all three* palettes and ask what the token is *for* rather than inverting its value. A `GearPalette` is a data class with no defaults, so forgetting one is a compile error rather than a wrong colour.
- **Wireframe mode is a palette, not a mode.** `WirePalette` renders every screen as its structural drawing, reached with `--ez wire true` (see Build). It needed no change to any of the 42 screens or 26 components, which is the tokenised colour system paying for itself — so if you find yourself adding an `if (wireframe)` to a screen, the fix belongs in the palette instead. The one legitimate exception is already written: `GearImage` draws a crossed box, because a photograph is not a colour and greying it would still leave a photograph. `LocalWireframe` (Theme.kt) exists for that single call site.
- **Interactive only where it earns its keep.** The fit profile, fairness slider, waiver scroll gate, calendar states, tab bar and photo carousel really work — and **discovery is now genuinely wired**: S-04's chips, S-06's sheet and S-05's results all read and write one `SearchFilters` (`data/SearchFilters.kt`), so the counts are real and the empty state is reachable by filtering. Everything else is a designed still. Adding fake interactivity elsewhere is still padding.
- **A filter that counts toward `activeCount` must actually filter.** `delivery` used to be decorative; it now has a field on `GearItem` and a test that fails if it stops doing anything.
- **Other named scope cuts:** tablet and foldable layouts (phone-first, plus the one 1440×1024 desktop admin screen S-40; the activity is locked to portrait), and the partner GearPoint POS terminal.
- **Tests cover logic and colour, not layout.** `app/src/test/` holds 37 tests over `FitLogic`, `SearchFilters` and the palettes — the places where a wrong answer is invisible. Screens are judged by eye and by preview; don't add snapshot tests.
- **Contrast is measured, not eyeballed.** `ContrastTest` walks every foreground-on-background pair the app draws, in all three palettes, against WCAG. Changing a palette value without running it is how the ramp quietly breaks — it is what caught amber failing as text on nine pairings, and later caught three of the wireframe fit verdicts sitting below 4.5:1 and its tertiary text failing on the two quieter surfaces.
- **A chip is painted at 34.dp inside a 44.dp target.** The pill is drawn in a `Box` that carries the `selectable`. Don't move the click back onto the pill to save a layer; don't grow the pill to match the target, which would make it read as a button.
- **The baseline profile is checked in** at `app/src/release/generated/baselineProfiles/`. Regenerate with `:app:generateBaselineProfile` against a connected device — the `:baselineprofile` module suppresses macrobenchmark's emulator refusal, because recording which methods compile is not a timing measurement.
- **Condition photos are exempt from the colour grade.** They are evidence, not marketing, and the design shows them unstyled.

## Build

AGP 8.13.2 · Kotlin 2.2.21 · JVM target 17 · compileSdk 36 · minSdk 26 · Compose BOM 2026.05.01 · Navigation Compose 2.9.5 · Gradle wrapper 8.14.3. Versions are centralised in `gradle/libs.versions.toml` — change them there, not in `app/build.gradle.kts`.

```powershell
.\gradlew assembleDebug        # 21 MB, the one to develop against
.\gradlew assembleRelease      # 10 MB, R8 + resource shrinking, signed with the debug key
.\gradlew testDebugUnitTest    # 37 tests over FitLogic, SearchFilters and the three palettes
```

Release is signed with the debug keystore on purpose — this is a demonstration build, and the point is that the optimised APK installs. `assembleRelease` needs network access the first time to fetch `lint-gradle`; `--offline` fails on that task alone.

`local.properties` holds the machine-local SDK path and is gitignored. There are no tests and no git repository.

Jump straight to a screen:
```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s21
```

Draw any screen as a wireframe — `WirePalette`, with photographs as empty slots.
Overrides the light/dark setting, since a wireframe is a drawing on paper:
```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s21 --ez wire true
```

## Working in this folder

**Start Claude Code from inside `GearLoop/`, not from `Desktop/david/`.** The 16 skills in `.claude/skills/` are project-scoped: they load at startup only when the session starts here. Start one directory up, in `Desktop/david/`, and they become a *nested* skills directory — not loaded at startup and absent from autocomplete until an agent happens to read or edit a file inside this folder.

Most of those skills are directory junctions into `../skills/skills/` (the davidondrej/skills clone), so `git pull` there updates them in place. Two are real copies because they needed Windows/GearLoop patches: `folder-specific-claude-and-agents-md` (paths, and a hard link instead of `ln -s`) and `level-up` (writes `notes/learning/knowledge.md`).
