# GearLoop — agent context

Folder-scoped context for GearLoop. The `README.md` is written for a human examiner; this file is for an agent, and exists to prevent the specific damage that is easy to do here.

## What this is

An installable Android app (IT3060 HCI, Milestone 03) grown from a 42-screen hi-fi prototype: 44 screens, the component library, three walkable flows, in Jetpack Compose.

Since Milestone 03 every screen reads and writes an on-device **Room** database (`data/db/`, 26 tables, schema version 1 exported to `app/schemas/`). `data/Demo.kt` is no longer drawn directly: `data/repo/Seed.kt` turns it into rows on first launch, and *Reset the demo* clears and reseeds. There is still no backend, no network call and no login. One device holds both accounts — the renter Nirmal (`Seed.ME`) and the owner Kavitha (`Seed.OWNER`, the S-24/27/28 console) — and the owner's half of a handshake or signature is simulated after a short delay. It runs offline and starts instantly, because it gets demonstrated live in a room.

**The data path is fixed:** screen → ViewModel (`ui/vm/ViewModels.kt`, one per member area) → repository (`data/repo/`, one per member area) → DAO. No screen touches a DAO or the database. `AppContainer` in `GearLoopApplication.kt` builds everything once; there is no DI framework.

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
- **An accent and its ink are different tokens.** A colour picked to read as a *shape* — a ring, a spine, a 2px border, held to 3:1 — is almost never dark enough to read as a *word* on its own tint. The semantic ramps have carried a `700` step for this since amber failed; `TrustTier` now does too. Use `tier.accent` for the ring round an avatar and `tier.ink` for the word inside the badge (`ui/theme/TokenColors.kt`). Writing a tier in its metal put GOLD on its own chip at 2.9:1, and the trust screen wrote SILVER in hi-vis/600, which measures 1.7:1.
- **Never use hi-vis decoratively.** It marks action and safety only — the same discipline as a life jacket. **Hi-vis is also identical in both themes**, and `GearText.onHiVis` with it: safety information should not change because someone flipped a setting. Don't "improve" the dark palette by shifting it.
- **Never let a chip that sits on a photograph follow the theme.** A photograph doesn't get darker in dark mode, so the save heart, the pager dots, the shutter ring and the quality badge use `GearSurface.overImage` / `GearText.onOverImage`, which are constant. Using `Chalk000` there is the bug this rule exists to prevent.
- **`Chalk000` means "the brightest surface", not "white".** In dark it is the deepest surface. Ink on a dark chip is `GearText.onDark`; a deliberately dark screen is `GearSurface.darkest`, not `Pine900`.
- **`Pine800` is not ink.** In light it reads as a deep green on white; in dark it is a *surface* step (`#164137`) and measures 1.66:1 on `Chalk000`. A label, outline or icon that sits on a theme-following surface is `GearText.primary` — that is what the secondary/ghost button skins and the default icon-button tone use since 1.1.1 (QA-08). `Pine800` stays correct only where the background is constant across themes: hi-vis, or the over-image chip. Nine screen-level icon tints still use it (QA-09, open) — check each one's background before changing it.
- **Never add a real photograph of a person.** Users are monogram avatars on a pine chip. Putting real faces behind people who don't exist is the wrong call, and it keeps the people layer quiet so the gear photography carries the screen.
- **Never introduce a map SDK or API key.** The map is drawn procedurally in `ui/components/MapCanvas.kt`.
- **Never import `androidx.compose.material3.Text` into a screen or a component.** Import `com.gearloop.app.ui.theme.Text` — the app's own shim (`ui/theme/BlockText.kt`), signature-identical to Material's, which draws every string as a bar in wireframe capture mode. A screen that imports Material's `Text` directly still compiles and still renders, and silently keeps its letters when the wireframe set is captured. `nav/GuidedBar.kt` is the one deliberate exception, because the guided bar is chrome rather than product.
- **No interactive element below 44.dp** (`Metrics.tapTarget`).
- **Persistence is Room on the device and nothing else.** Don't add networking, auth, a remote backend or a DI framework. Out of scope by design, not by omission (the reasoning is in the README).
- **Never write a row as two concurrent read-modify-writes.** Each repository has an `edits` Mutex, and every edit that reads a row and writes it back goes through it (`editDraft`, `editProposal`, `editBooking`). Two `act {}` calls launched from one tap used to read the same row and the second write put back what the first changed — S-21 sent proposals at LKR 0 (D-14), S-25 lost typed titles (D-15). When one tap changes two fields, add one repository call that writes both (`send(boot)`, `setDetails(title, size)`), and never nest one locked function inside another: the Mutex is not reentrant.
- **Never decide a write from a hot `StateFlow.value`.** The flows lag the database by a dispatch. Write paths read through the DAO (`spansNow`, `blocksNow`, `marksFor`, `claimRefs`…); `.value` is for drawing.
- **Never key recomposition on a parameter the composable doesn't read.** The Compose compiler ignores unused parameters when deciding to skip, so a `version: Int` "to force a redraw" does nothing — S-27 kept a blocked day drawn as free (D-16). Pass the data and use it.
- **Every custom control carries its own semantics.** A switch is the whole `ToggleRow` (`Role.Switch`, label included), not a bare 32 dp track (D-11). `GearSlider` and `GearRangeSlider` take a `label` and expose progress plus `setProgress` (D-13) — pass a label at every new call site. The functional tests address controls by these names, so a control a test cannot find is usually one TalkBack cannot either.
- **Match records by id or a named constant, never by a substring of display text.** `contains("address")` also matched "Email address" (D-12); the home-address step is `VerificationStep.HOME_ADDRESS`.

## The rule that keeps the app coherent

**If a user could act on the number, it is set in mono.** Prices, sizes, distances, condition grades, item codes, countdowns → `GearType.data*` (JetBrains Mono). Mono aligns in columns and doesn't disguise a `1` as an `l`.

`GearType.dataS` and `GearType.labelXs` are **always rendered UPPERCASE** — pass already-uppercased text or apply it at the call site.

## Conventions

- **`data/Demo.kt` is the single source of truth for the seed.** The Torq Mod Fish costs LKR 2,800 a day on all eleven screens it appears on. If you change a value, change it there — never at a call site — and reset the demo (or clear app data), because an existing database keeps the old rows. Prices are computed by `data/Pricing.kt` and held to Demo's designed basket by `PricingTest` (9,240 at checkout, 9,576 on the listing footer).
- **Changing an entity changes the schema.** It is version 1 with no migrations. Bump the version, let KSP export the new JSON to `app/schemas/`, and add a migration or a destructive fallback (the app reseeds when the profile row is missing).
- **Dates go through `data/Format.kt` in `Locale.ENGLISH`,** not `Locale.UK`, which abbreviates September to "Sept" (D-17). `DemoClock` is the only clock: the world starts at 09:14 on Wed 5 Aug 2026 and runs in real time from a persisted anchor.
- **Assemble from the existing 26 components** in `ui/components/` (Buttons, Cards, Inputs, Feedback, Navigation, Foundation, Signature, MapCanvas). Nothing is drawn twice. Read the component before writing a new one.
- **Every component has previews at the bottom of its file.** Add one for anything new, using `@GearPreview` or `@GearTypePreview` from `ui/preview/GearPreview.kt` and the `PreviewFrame` wrapper. `@GearPreview` renders light *and* dark; `@GearTypePreview` adds 200 % font scale. Between them they are how both themes actually get reviewed — 26 components in a split pane beats 42 screens walked twice — so use `@GearTypePreview` for anything with a fixed height.
- **The three signature elements** — `GearTag`, `FitStamp`, `FairnessMeter` (`ui/components/Signature.kt`) — carry the concept. Treat them as load-bearing, not decoration. All three carry hand-written semantics; if you change what they display, change what they announce.
- **Adding a screen takes two edits:** register a `ScreenSpec` in `Catalogue` (`nav/Destinations.kt`) *and* add a `composable(...)` route in `nav/GearLoopApp.kt` (53 routes today: the 44 catalogue screens plus 9 shell routes — cover, index, five boards, credits, privacy). The catalogue drives the in-app screen index and the flow walkthroughs — a route registered in only one place is half-added. `VerifyCatalogue` in `GearLoopApp.kt` checks both directions on launch and fails the debug build if they disagree, so getting this wrong is loud rather than silent.
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
- **Greeked copy is a `Text` shim, not a type token.** `--ez blocks true` draws every string as a bar from cap height to baseline — the wireframe convention, and what the module lecturer asked for. It lives in `ui/theme/BlockText.kt`, which defines the app's own `Text`; screens and components import that instead of Material's, so 458 call sites are intercepted by 27 import lines. **Do not "simplify" this into `GearType`.** It was tried: a transparent colour with a background on the `TextStyle` does not work, because 421 call sites pass `color =` explicitly and an explicit colour argument beats `style.color` inside Compose's `Text`. Intercepting the call is the only place that sees both the style and the colour the design asked for — which is also why the bar is drawn in *the ink colour the words would have had*, so a bar on the camera viewport comes out light and a bar on a hi-vis button comes out pine, with no token of its own. `blockText` is a separate flag from `wireframe` on purpose: one takes the colour out, the other takes the reading out.
- **Every screen is wired, at least two CRUD operations each** (the Milestone 03 requirement). Discovery shares one `SearchFilters` (`data/SearchFilters.kt`) across S-04/05/06, now saved to the database. Deletes offer Undo through the `Toaster`. Where the design and the working app differ, the difference is commented at the point of change and listed in `docs/milestone-03/deviations.md` — keep both in step.
- **A filter that counts toward `activeCount` must actually filter.** `delivery` used to be decorative; it now has a field on `GearItem` and a test that fails if it stops doing anything.
- **Other named scope cuts:** tablet and foldable layouts (phone-first, plus the one 1440×1024 desktop admin screen S-40; the activity is locked to portrait), and the partner GearPoint POS terminal.
- **Tests cover logic, colour, cross-screen consistency and behaviour, not layout.** `app/src/test/` holds 93 JVM tests over `Pricing`, `Availability`, `FitLogic`, `SearchFilters`, `Format`, the `Toaster`, the palettes and `Demo` itself — the places where a wrong answer is invisible. `app/src/androidTest/` holds the 68 functional tests TC-01 to TC-68, one class per member, on the `FunctionalTest` base: each reseeds, launches a screen by route, drives it by text, content description and semantics actions, and checks the result on screen *and* in the database. The TC numbers are cited in the report and in `docs/milestone-03/`; don't renumber them. `CaptureScreens` is not a test case: it is skipped unless run with `-Pandroid.testInstrumentationRunnerArguments.capture=true`, and it writes the report's 44 screenshots to the app's files (pull them with `run-as`, so run with `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`). `DemoConsistencyTest` is the newest of the four and guards the failure this dataset is actually prone to: two screens quoting the same fact differently. It checks that the price lines add to the total they print, that the review histogram sums to the count beside it, that a request is priced for the nights it asks for, and that the booking three screens call "no deposit" has no hold against it in the wallet. Screens are judged by eye and by preview; don't add snapshot tests.
- **Contrast is measured, not eyeballed.** `ContrastTest` walks every foreground-on-background pair the app draws, in all three palettes, against WCAG. Changing a palette value without running it is how the ramp quietly breaks — it is what caught amber failing as text on nine pairings, and later caught three of the wireframe fit verdicts sitting below 4.5:1 and its tertiary text failing on the two quieter surfaces.
- **A chip is painted at 34.dp inside a 44.dp target.** The pill is drawn in a `Box` that carries the `selectable`. Don't move the click back onto the pill to save a layer; don't grow the pill to match the target, which would make it read as a button.
- **The baseline profile is checked in** at `app/src/release/generated/baselineProfiles/`. Regenerate with `:app:generateBaselineProfile` against a connected device — the `:baselineprofile` module suppresses macrobenchmark's emulator refusal, because recording which methods compile is not a timing measurement.
- **Condition photos are exempt from the colour grade.** They are evidence, not marketing, and the design shows them unstyled.

## Build

AGP 8.13.2 · Kotlin 2.2.21 · JVM target 17 · compileSdk 36 · minSdk 26 · Compose BOM 2026.05.01 · Navigation Compose 2.9.5 · Gradle wrapper 8.14.3. Versions are centralised in `gradle/libs.versions.toml` — change them there, not in `app/build.gradle.kts`.

```powershell
.\gradlew assembleDebug        # 21 MB, the one to develop against
.\gradlew assembleRelease      # 10 MB, R8 + resource shrinking; upload key if keystore.properties exists, else debug key
.\gradlew testDebugUnitTest    # 93 JVM tests
.\gradlew connectedDebugAndroidTest   # 68 functional tests; needs a device or the GearLoop_Pixel9Pro AVD
```

If `installDebug` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a release build signed with the upload key is installed: `adb uninstall com.gearloop.app` first (it wipes the demo database, which reseeds).

Release is signed with the **upload key** when the gitignored `keystore.properties` is present, and falls back to the debug keystore when it is not — on purpose, so the optimised APK still builds and installs on any clone. Never commit the key or that file; `keystore.properties.example` documents the four keys it must hold. `assembleRelease` needs network access the first time to fetch `lint-gradle`; `--offline` fails on that task alone.

`local.properties` holds the machine-local SDK path and is gitignored. The folder is a git repository on `main`; commit or push only when asked.

Jump straight to a screen. Keep the `-S`: without it a running app is only brought to
the front ("intent delivered to currently running top-most instance") and the extras
are ignored, because `MainActivity` reads them in `onCreate` alone:
```
adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21
```

Draw any screen as a wireframe — `WirePalette`, with photographs as empty slots.
Overrides the light/dark setting, since a wireframe is a drawing on paper:
```
adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21 --ez wire true
```

Add `--ez blocks true` to greek the copy as well — every string drawn as a bar.
The two flags are independent; the submitted wireframe set uses both:
```
adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21 --ez wire true --ez blocks true
```

Both flags last only while the app stays on screen. Coming back from the background,
a launch from Recents, or a rebuilt activity always draws the real app
(`MainActivity.onRestart` / `onSaveInstanceState`), and `capture-wireframes.ps1`
force-stops the app when it finishes. Before that, the intent the capture script left
behind brought the live demo back as grey boxes. Don't make the flags a one-time read
in `setContent` again.

The 44-screen PNG sets and the assignment deliverables live in `docs/milestone-02/`
(sketches, wireframes, low-fi notes, hi-fi screenshots, the test plan and the issue
log). Both PNG sets are regenerated by the scripts in `docs/milestone-02/capture/`,
which read the filenames and the four-way member split from `04-hifi/` so the two
trees cannot drift apart.

## Working in this folder

**Start Claude Code from inside `GearLoop/`, not from `Desktop/david/`.** The 16 skills in `.claude/skills/` are project-scoped: they load at startup only when the session starts here. Start one directory up, in `Desktop/david/`, and they become a *nested* skills directory — not loaded at startup and absent from autocomplete until an agent happens to read or edit a file inside this folder.

Most of those skills are directory junctions into `../skills/skills/` (the davidondrej/skills clone), so `git pull` there updates them in place. Two are real copies because they needed Windows/GearLoop patches: `folder-specific-claude-and-agents-md` (paths, and a hard link instead of `ln -s`) and `level-up` (writes `notes/learning/knowledge.md`).
