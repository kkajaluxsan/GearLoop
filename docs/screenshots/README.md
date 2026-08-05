# GearLoop — UI Screenshots

Screenshots of every screen in the GearLoop prototype, captured from the debug
build running on a Pixel 9 Pro emulator (1280 × 2856, light theme, Android 16).

Each screen was opened directly by its route so the capture shows the screen as
designed, with no guided-demo bar over it:

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s07
```

## How they're split

44 screens, 11 per person, kept in catalogue order so each person's set is one
coherent part of the app rather than a random sample.

| Folder | Screens | Covers |
|---|---|---|
| `Member_1` | S-01 → S-11 | Onboarding, Discovery, start of Booking |
| `Member_2` | S-12 → S-20 | Rest of Booking, all of Handover, start of Exchange |
| `Member_3` | S-21 → S-31 | Rest of Exchange, Owner tools, Search Map / Trip Planner |
| `Member_4` | S-32 → S-42 | Account, Trust, Admin console |

`Member_2` also holds S-15b (Damage Marking) and S-18b (Evidence Diff), the two
sub-screens that sit inside the handover flow — which is why 42 numbered screens
come to 44 files.

## Naming

`S-07_Listing-Detail.png` — screen ID first so the files sort in flow order, then
the title from the catalogue in `nav/Destinations.kt`.

The wireframes in [`../wireframes`](../wireframes) use the same filenames and the
same split, so any screen can be put next to its wireframe.
