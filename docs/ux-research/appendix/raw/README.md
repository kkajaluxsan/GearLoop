# raw/ — recordings and completed questionnaires

Constraint **d**: *"Raw research data (recordings/notes/completed questionnaires)
must be retained and referenced in the appendix."*

This is where the retained data goes. It is **referenced** from
[`../A2-participant-log.md`](../A2-participant-log.md), which carries the
filename for every session.

## What belongs here

| File | What it is |
|---|---|
| `P1-a.m4a` … `P7-a.m4a` | Strand A discovery interview audio |
| `P1-b.mp4` … `P5-b.mp4` | Strand B screen recordings (screen only, not the participant) |
| `survey-responses.csv` | Strand C raw export |
| `consent/` | Scans of any signed paper consent forms |

Naming: `P<id>-<strand>.<ext>`. Nothing else — the filename is the join key back
to A2.

## Not committed to git

These files contain identifiable voices. They are excluded from version control
and are submitted alongside the report on whatever medium your module accepts —
a shared drive folder, a USB drive, or an archive attached to the submission,
according to what the coordinator asks for.

Add to `.gitignore` at the repository root:

```gitignore
docs/ux-research/appendix/raw/*
!docs/ux-research/appendix/raw/README.md
```

## Retention

Delete once the module result is confirmed, or sooner if a participant withdraws.
[`../A1-consent.md`](../A1-consent.md) records who agreed to what.

## If a participant declined recording

Written notes are still raw data and still belong here — `P4-a-notes.md`. Mark
their quotes in the report as *reconstructed from notes* rather than verbatim.
Reconstructed evidence is usable; reconstructed evidence presented as a verbatim
quote is not.
