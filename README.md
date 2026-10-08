# GearLoop — Sports Gear Rental & Exchange

An installable Android app for renting, lending and swapping sports gear,
built in Kotlin and Jetpack Compose. It began in Milestone 02 as a 42-screen
high-fidelity prototype and became a working app in Milestone 03 (IT3060 HCI,
SLIIT, 2026).

**What changed in Milestone 03.** Every screen now reads from and writes to an
on-device Room (SQLite) database. Bookings, payments, messages, condition
records, claims, swap proposals, listings and the owner's calendar are real
rows that survive a restart, and each of the 44 interfaces supports at least
two create, read, update or delete operations. The dataset in `data/Demo.kt` is
no longer drawn directly. It seeds the database on first launch, and the
cover's *Reset the demo* clears and reseeds it, so every usability session
starts from the same state.

It still runs fully offline, with no network call and no login. One phone
holds both sides of the market: the renter, Nirmal, and the owner, Kavitha, on
her console (S-24, S-27, S-28). The owner's half of a two-device handshake is
simulated. These decisions and their reasons are set out in
[`docs/milestone-03/`](docs/milestone-03/).

---

## Setting it up and running it

**You need:** Android Studio (2025.3 or newer) with JDK 17, and an emulator or
phone on **Android 8.0 (API 26)** or newer.

1. Clone the repository, or unzip the submission, and open the `GearLoop`
   folder in Android Studio (**File → Open**).
2. Wait for the Gradle sync. The wrapper downloads Gradle 8.14.3 and the
   dependencies the first time, so the first sync needs internet access.
3. Pick a device and press **Run**, or from a terminal:

```powershell
.\gradlew installDebug          # build and install the debug app
.\gradlew assembleRelease       # the optimised APK: app/build/outputs/apk/release/app-release.apk
```

**Installing the APK without Android Studio:** copy `app-release.apk` to the
phone and open it (allow *Install unknown apps* for your file manager), or run
`adb install -r app-release.apk`. If an older GearLoop signed with a different
key is installed, uninstall it first (`adb uninstall com.gearloop.app`).

Versions are pinned in `gradle/libs.versions.toml`: AGP 8.13.2, Kotlin 2.2.21,
KSP 2.2.21-2.0.5, Room 2.8.5, Compose BOM 2026.05.01, Navigation 2.9.5,
compileSdk 36, minSdk 26, JVM target 17.

The release build is signed with the group's upload key when the gitignored
`keystore.properties` is present, and with the debug key otherwise, so it
builds and installs from any clone. `assembleRelease` needs network access the
first time it runs, to fetch `lint-gradle`.

### Running the tests

```powershell
.\gradlew testDebugUnitTest            # 93 JVM tests: pricing, availability, fit, search, formatting, contrast, dataset, toasts
.\gradlew connectedDebugAndroidTest    # 68 functional tests (TC-01 to TC-68) on a connected device or emulator
```

The functional tests drive the real screens the way a user would (tap, type,
scroll, long-press) and then check the result twice: on screen, and in the
Room database. Each test reseeds the database first. They are listed with
their results in [`docs/milestone-03/functional-test-results.md`](docs/milestone-03/functional-test-results.md).

### Using it

The app opens on a **cover screen** that is also the demo control panel. The
three flows, the five presentation boards and an index of all 44 screens are
each one tap away, along with a light/dark switch, *Reset the demo*, and two
**prepared states**: *Mid-rental* (the Torq booked, checked and signed for) and
*After return, claim open* (claim CL-3081 raised). The prepared states run the
same repository calls the screens make, so they reach the same data a
participant would.

**Start a flow and the app narrates itself.** A guide bar appears above the
screen showing which flow you're in, how far through, and the one sentence that
explains what the current screen is doing — with next and back, so nothing has
to be found mid-sentence. Restart puts every screen back to how it was designed,
which matters when the same demo is given twice in a row.

To jump straight to a screen (useful when an examiner asks for one by number):

```
adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21
```

The `-S` stops the app first. Without it, a copy that is already open is only
brought to the front, and the screen you asked for is ignored.

And to draw that screen as a wireframe instead:

```
adb shell am start -S -n com.gearloop.app/.MainActivity --es screen s21 --ez wire true
```

---

## The design idea

Rental gear in the real world carries a tag zip-tied to it: item code, size,
condition grade, punched in monospace on hi-vis card stock. That tag is the
entire visual language.

So GearLoop looks like technical equipment, not a shopping app:

- **Monospace for anything measurable.** Prices, sizes, distances, grades, item
  codes, countdowns. If a user could act on the number, it's set in JetBrains
  Mono. Mono aligns in columns and doesn't disguise a `1` as an `l`.
- **Hi-vis yellow for action and safety only.** Never decorative — the same way
  it's used on a life jacket. Text on hi-vis is always `pine/900`, contrast
  11.9:1.
- **Pine green for structure**, drawn from marine and trail equipment.
- **Restrained radii**, because gear is engineered, not soft. Cards are
  separated by a 1px border, not a shadow.

### The three signature elements

| | What it is | Why it exists |
|---|---|---|
| **Gear Tag** | Hi-vis block, mono uppercase, punched hole | Replaces the generic specs row. Says *this is a real, individual, tracked object*, not a catalogue SKU. |
| **Fit Stamp** | Stamped badge, own colour ramp, 3° off true | Fit is a safety property, not a preference. Wrong-sized helmets and harnesses hurt people. |
| **Fairness Meter** | A beam on a pivot that physically tilts | The one moment of motion. Move the boot slider on S-21 and it levels out. |

---

## What's in here

```
app/src/main/java/com/gearloop/app/
├── MainActivity.kt          entry point, edge-to-edge, start-route extra
├── GearLoopApplication.kt   AppContainer: builds the database and repositories once
├── nav/
│   ├── Destinations.kt      the 44-screen catalogue: id, page, tier, flows
│   ├── GearLoopApp.kt       NavHost + transitions (250 ms screens, 300 ms sheets)
│   ├── DemoState.kt         which flow is running, and the demo reset
│   └── GuidedBar.kt         the walkthrough chrome above the app bar
├── data/
│   ├── Model.kt             the shapes behind the screens
│   ├── Demo.kt              the dataset: the seed, and still one source of truth
│   ├── FitLogic.kt          fit verdicts derived from the stored profile
│   ├── Pricing.kt           every quote, fee and deposit rule in one place
│   ├── Availability.kt      the day rules both calendars draw from (FR-16, FR-17)
│   ├── SearchFilters.kt     the shared search predicate S-04/05/06 all use
│   ├── Format.kt            rupee, distance, date and countdown formatting
│   ├── DemoClock.kt         the demo's clock: 5 Aug 2026, 09:14, then real time
│   ├── db/                  Room: 26 entities, 6 DAOs, type converters, the database
│   └── repo/                one repository per member area, the seed, prepared states
├── ui/
│   ├── vm/                  one ViewModel per member area, plus the factory
│   ├── feedback/            the toast host, with Undo on every delete
│   ├── theme/               Palette (light + dark + wire) · Color · Type · Metrics
│   ├── icons/               Lucide geometry + a stroke-weight-aware renderer
│   ├── preview/             the multipreview annotations and preview frame
│   └── components/          the component library, each file with its previews
└── screens/
    ├── onboarding/  discovery/  booking/  handover/
    ├── exchange/    owner/      account/  admin/
    └── boards/      cover, screen index, presentation boards

app/src/test/          93 JVM unit tests
app/src/androidTest/   68 functional tests, one class per member (M1 to M4)
app/schemas/           the exported Room schema
docs/milestone-03/     test results, defect log, deviations, usability kit
```

Open any file in `ui/components/` in Android Studio and every variant of that
component renders in the split pane — light, dark, and at 200 % font scale,
which is where a fixed-height row gives itself away. That pane is how the dark
palette was reviewed: 26 components side by side rather than 44 screens walked
twice.

### The 26 components

`Button` · `IconButton` · `Chip` · **`GearTag`** · **`FitStamp`** ·
**`FairnessMeter`** · `ListingCard` · `SwapCard` · `BookingCard` · `OwnerStrip` ·
`PriceRow` · `ReviewCard` · `InsightCard` · `TextField` · `SearchBar` ·
`Stepper` · `Toggle` · `Slider` · `DateRangeCell` · `SegmentedControl` ·
`TabBar` · `AppBar` · `StickyFooter` · `ProgressStepper` · `Banner` · `Toast` ·
`Timeline` · `EmptyState` · `Skeleton`

Every screen is assembled from these. Nothing is drawn twice.

---

## The three flows

**A — Rent** (the main demo, 11 screens)
`S-04 Home → S-05 Search → S-07 Listing → S-08 Dates → S-09 Protection →
S-10 Waiver → S-11 Pay → S-12 Confirmed → S-13 Handover → S-15 Capture →
S-18 Settlement`

**B — Swap** `S-19 → S-20 → S-21 → S-22 → S-23`

**C — Owner** `S-24 → S-28 → S-27`

### Screens worth stopping on

- **S-02 Fit Profile** — the derived-size panel is computed live in
  `FitLogic.kt`. Move either stepper and the surfboard volume, ski length and
  bike frame all change. It isn't a static mock-up.
- **S-07 Listing Detail** — the money screen. Tag, fit verdict with its reason,
  condition evidence, deposit explained in plain language.
- **S-10 Safety & Waiver** — the accept button stays disabled until the waiver
  has actually been scrolled to the end.
- **S-15 Condition Capture** — seven guided angles, both parties present,
  geotagged and timestamped. Tap the validation chip to flip between the pass
  and fail states.
- **S-18 Return & Settlement** — the same seven angles side by side. The owner
  reviews rather than hunts.
- **S-21 Fairness & Boot** — drag the slider and watch the beam level out.
- **S-23 Cycle Preview** — three people, none of whom could trade directly.
- **S-40 Admin Claims Console** — the one desktop screen, 1440×1024, presented
  as a scrollable board. Most student projects skip the back office entirely.

---

## Deliberate decisions worth defending

**Local persistence rather than a server.** Milestone 03 asked for a working
app, and the question was what "working" needed. Room on the device gives real
reads and writes, survives a restart, and keeps every session reproducible,
because a reset reseeds it. A hosted backend would have added sign-up, network
failure and shared state between participants, none of which the usability
tasks measure. The repositories are the seam where one would go: no screen
talks to the database directly.

**Search and filtering are real.** The home chips, the filter sheet and the
results list share one model, so the count on the sheet's button is the number
of results you will actually get, removing a chip re-runs the search, and typing
in the search bar filters by title, sport or item code. Filter hard enough and
you reach the empty state by doing so — rather than by tapping a button that
admits it is a mock-up. The two pieces of logic behind this, the search
predicate and the fit derivation, have 35 unit tests, because a filter that
quietly excludes something looks exactly like a filter that works. Since
Milestone 03 the filters are also saved, so a search survives a restart.

**Both themes are built.** Not by inverting the palette, but by asking what each
token is *for*: chalk is the surface ramp in light and the ink ramp in dark, and
pine trades places with it. Two things deliberately do not move — hi-vis, because
safety information should not depend on a setting, and anything sitting on a
photograph, because the photograph doesn't change either.

**The app can draw its own wireframes.** There is a third palette, and switching
to it renders all 44 screens as the structural drawings they came from — flat
greys, photographs as crossed boxes, layout untouched. Not one screen and not one
of the 26 components needed changing to make it work, which is the argument for
tokenising colour stated more plainly than any rule could: a colour that is never
written at a call site can be replaced everywhere at once. The wireframes in this
repo are generated, not drawn, so they cannot fall out of date with the screens
the way a separate Figma file would.

A second flag greeks the copy with it, drawing every string as a bar from cap
height to baseline — the wireframe convention, where a reviewer should be reading
the structure rather than the words. The glyphs still lay out and are simply not
painted, so every bar sits exactly where its words sat; and each bar takes the ink
colour those words would have had, which is what keeps one legible on the camera
viewport and on a hi-vis button without inventing a grey for it. Both sets, and the
rest of the coursework deliverables, are in [`docs/milestone-02/`](docs/milestone-02/).

**Contrast is measured rather than judged by eye.** A test walks every
foreground-on-background pair the app actually draws — body copy on all three
surfaces, ink on every tint, the fit ramp, the trust metals, text over
photography — through the WCAG formula, in all three palettes. It found nine failures
in the original light palette, including an amber that could not reach 4.5:1 as
text even on pure white while being used for eight countdowns and status lines.
The fix was structural: semantic colours now have a `700` ink step distinct from
the `500` accent, the way marine always did and the other three didn't.

**The first launch is compiled ahead of time.** A baseline profile is generated
from the app's own startup path and packaged with the release, so the cold start
an examiner sees isn't the one paying the interpreter.

**Monogram avatars instead of stock portraits.** Putting real people's faces
behind users who don't exist is the wrong call, and initials on a pine chip keep
the people layer quiet so the gear photography carries the screen.

**The map is restyled, not default.** Land is chalk, water is marine, roads are
`chalk/300`. It's drawn procedurally in `MapCanvas.kt` — there is no map SDK and
no API key. A default blue map tile in a design portfolio is a mark-loser.

**Icons are Lucide, not Material.** Material Icons are the giveaway that nothing
was chosen. The real Lucide geometry is kept as SVG path data so a single
definition can be drawn at any stroke weight — which is how the active tab-bar
state is expressed, since Lucide ships no filled variants.

**Photography is graded once, at packaging time.** Every image goes through the
same warm-neutral treatment so the set reads as one shoot. Condition photos are
exempt — they're evidence, not marketing, and the design shows them unstyled.

**Every photograph ships twice.** A 1200×900 frame costs 4.1 MB of heap once
decoded, whether it's filling the screen or sitting in a 48 dp chip — and a
search results page holds eight at once. Each image has a 600 px copy alongside
it, and `GearImage` measures the space it's been given before deciding which to
decode. No call site knows about it.

**The screen reader gets the same information as the eye.** The fit stamp says
its verdict, the fairness meter announces the trade as one sentence rather than
as a pile of stray numbers, and the tab bar reports which tab is selected —
which matters because the selected state is carried by stroke weight, and stroke
weight isn't something you can hear.

### Known scope cuts

Named here rather than left for someone else to find:

- **Tablet and foldable layouts.** Phone-first, plus the one desktop admin
  screen; the app is locked to portrait.
- **The partner POS / GearPoint terminal.** Referenced in the flows, not designed.

---

## Credits

- **Type** — Archivo, Instrument Sans, JetBrains Mono (SIL Open Font Licence),
  instanced to fixed weights from the Google Fonts variable sources.
- **Icons** — [Lucide](https://lucide.dev) (ISC).
- **Photography** — Creative Commons and public domain, sourced via
  [Openverse](https://openverse.org) and [Wikimedia Commons](https://commons.wikimedia.org).
  Only licences permitting derivative works
  were used, because every frame is re-cropped and colour-graded. Full
  attribution is in the app under **Cover → Image credits**, and in
  `data/PhotoCredits.kt`.
