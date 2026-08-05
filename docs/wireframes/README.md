# GearLoop — Wireframes

The same 44 screens as [`../screenshots`](../screenshots), rendered as
wireframes. Same filenames, same split across the four of us, so any screen can
be put side by side with its finished version.

## These are generated, not drawn

The app draws them itself. GearLoop never writes a colour at a call site — every
one resolves through `GearPalette` at render time — so adding a third, grey-only
palette turns all 42 screens into wireframes at once, with no screen and none of
the 26 components changed:

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s07 --ez wire true
```

That is worth saying out loud in the viva. It means the wireframes cannot drift
out of date with the screens — the usual failure of a separate Figma wireframe
file — because they *are* the screens, with the colour taken out.

Implementation is four small edits: `WirePalette` in `ui/theme/Palette.kt`, the
`wireframe` flag in `ui/theme/Theme.kt`, the placeholder branch in `GearImage`
(`ui/components/Foundation.kt`), and the intent extra in `MainActivity.kt`.

## The greys are measured, not eyeballed

`ContrastTest` walks every foreground-on-background pair the app draws, and the
wireframe palette now goes through the same audit as the light and dark ones —
a wireframe is still read, so it is held to WCAG rather than excused as a
working drawing. That was not a formality: it failed twice on the first run.

- **The fit ramp**, written first as an even spread from `#3D` to `#A8`, put
  three of its five verdicts below 4.5:1. The stamp draws its 2px border *and*
  its label in one colour, so it is held to the text bar — and `#6E` is the
  lightest grey that clears it on the page. The five steps were compressed into
  what passes rather than spread across what looks even.
- **Tertiary text** at `#757575` failed on the app and sunken surfaces, which is
  the same trap the light palette originally fell into when tertiary borrowed
  `chalk/500`. It sits at `#6A6A6A`, the ceiling set by the sunken panel.

Two further tests guard the palette: one asserts nothing in it has a hue left
(a value pasted across from the light set would contrast fine and still be
wrong), and one asserts the five fit steps stay far enough apart to tell apart,
so a later contrast fix cannot quietly collapse two of them.

## What changes, and why

| | In the wireframe |
|---|---|
| **Colour** | Gone. Flat greys only. |
| **Photographs** | A crossed box at the exact size the photo occupies — a photo greyed is still a photo, and a wireframe should show a slot. |
| **Layout, spacing, type** | Untouched. Every position is the real one. |
| **Copy** | Real. No lorem ipsum — the words are part of the design. |
| **Hi-vis actions** | Five distinct greys, so a primary button, its pressed state and its tint container stay three different things. |
| **The fit ramp** | Keeps all five steps. The fit stamp encodes its verdict in colour, and flattening it would lose information. |
| **Success / warning / danger** | Collapse to shared greys. At this fidelity a banner is a banner; which kind comes from its icon and its words. |

## How they're split

Identical to the screenshots — 11 each, in catalogue order.

| Folder | Screens | Covers |
|---|---|---|
| `Member_1` | S-01 → S-11 | Onboarding, Discovery, start of Booking |
| `Member_2` | S-12 → S-20 | Rest of Booking, all of Handover, start of Exchange |
| `Member_3` | S-21 → S-31 | Rest of Exchange, Owner tools, Search Map / Trip Planner |
| `Member_4` | S-32 → S-42 | Account, Trust, Admin console |
