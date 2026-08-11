# GearLoop — Sports Gear Rental & Exchange

A UI/UX prototype built in Jetpack Compose from the `gearloop-brief.md` design
brief. 42 screens, 26 components, one design system, three walkable flows.

**This is a design deliverable, not an application.** There is no backend, no
database, no network call and no login. Every screen renders from a fixed
dataset in `data/Demo.kt`, which is why the Torq Mod Fish costs LKR 2,800 a day
on all eleven screens it appears on. It runs offline and starts instantly —
which is what you want when you're demonstrating it in a room.

---

## Opening it

1. Android Studio → **Open** → select this `GearLoop` folder.
2. Wait for the Gradle sync (the wrapper pulls Gradle 8.14.3 the first time).
3. Run on any emulator or device on **Android 8.0 (API 26)** or newer.

Built and verified with Android Studio 2025.3.4, AGP 8.13.2, Kotlin 2.2.21,
compileSdk 36, JVM target 17.

`assembleRelease` produces a 10 MB APK — half the debug build, after R8 and
resource shrinking — signed with the debug key so it installs directly.
`testDebugUnitTest` runs the 51 tests over the search predicate, the fit
derivation and the contrast of all three palettes.

The app opens on a **cover screen** that is also the demo control panel — the
three flows, the five presentation boards, and an index of all 42 screens are
each one tap away, along with a light/dark switch and a "reset the demo" button.

**Start a flow and the app narrates itself.** A guide bar appears above the
screen showing which flow you're in, how far through, and the one sentence that
explains what the current screen is doing — with next and back, so nothing has
to be found mid-sentence. Restart puts every screen back to how it was designed,
which matters when the same demo is given twice in a row.

To jump straight to a screen (useful when an examiner asks for one by number):

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s21
```

And to draw that screen as a wireframe instead:

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s21 --ez wire true
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
├── nav/
│   ├── Destinations.kt      the 42-screen catalogue: id, page, tier, flows
│   └── GearLoopApp.kt       NavHost + transitions (250 ms screens, 300 ms sheets)
│   ├── DemoState.kt         which flow is running, and the demo reset
│   ├── GuidedBar.kt         the walkthrough chrome above the app bar
├── data/
│   ├── Model.kt             the shapes behind the screens
│   ├── Demo.kt              the §5 dataset — one source of truth
│   ├── FitLogic.kt          live size derivation (S-02 recalculates from this)
│   ├── SearchFilters.kt     the shared search predicate S-04/05/06 all use
│   ├── Format.kt            rupee, distance and number formatting
│   ├── Thumbs.kt            generated full-frame → 600 px thumbnail map
│   └── PhotoCredits.kt      generated attribution table
├── ui/
│   ├── theme/               Palette (light + dark) · Color · Type · Metrics · Theme
│   ├── icons/               Lucide geometry + a stroke-weight-aware renderer
│   ├── preview/             the multipreview annotations and preview frame
│   └── components/          the 26-component library, each file with its previews
└── screens/
    ├── onboarding/  discovery/  booking/  handover/
    ├── exchange/    owner/      account/  admin/
    └── boards/      cover, screen index, presentation boards
```

Open any file in `ui/components/` in Android Studio and every variant of that
component renders in the split pane — light, dark, and at 200 % font scale,
which is where a fixed-height row gives itself away. That pane is how the dark
palette was reviewed: 26 components side by side rather than 42 screens walked
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

**Interactive where it earns its keep.** The fit profile, fairness slider,
waiver scroll gate, calendar states, tab bar and photo carousel all really work.
Everything else is a designed still — this is a UI prototype, and pretending
otherwise would be padding.

**Search and filtering are real.** The home chips, the filter sheet and the
results list share one model, so the count on the sheet's button is the number
of results you will actually get, removing a chip re-runs the search, and typing
in the search bar filters by title, sport or item code. Filter hard enough and
you reach the empty state by doing so — rather than by tapping a button that
admits it is a mock-up. The two pieces of logic behind this, the search
predicate and the fit derivation, have 27 unit tests, because a filter that
quietly excludes something looks exactly like a filter that works.

**Both themes are built.** Not by inverting the palette, but by asking what each
token is *for*: chalk is the surface ramp in light and the ink ramp in dark, and
pine trades places with it. Two things deliberately do not move — hi-vis, because
safety information should not depend on a setting, and anything sitting on a
photograph, because the photograph doesn't change either.

**The app can draw its own wireframes.** There is a third palette, and switching
to it renders all 42 screens as the structural drawings they came from — flat
greys, photographs as crossed boxes, layout and copy untouched. Not one screen
and not one of the 26 components needed changing to make it work, which is the
argument for tokenising colour stated more plainly than any rule could: a colour
that is never written at a call site can be replaced everywhere at once. The
wireframes in this repo are generated, not drawn, so they cannot fall out of date
with the screens the way a separate Figma file would.

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
  [Openverse](https://openverse.org). Only licences permitting derivative works
  were used, because every frame is re-cropped and colour-graded. Full
  attribution is in the app under **Cover → Image credits**, and in
  `data/PhotoCredits.kt`.
