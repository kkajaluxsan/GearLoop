# Appendix A4 — Usability Scoring Sheets

Strand B, n = 4. This is the appendix that turns *"some people found filtering
hard"* into *"task 3 averaged 4.5 out of 7, the second-lowest in the study."*

> **Fill this from real sessions.** The figures reproduced in
> [`01-research-data.md §6`](../01-research-data.md#6-summary--usability-task-scores)
> are the proxy set; replace them here first, then propagate.

---

## Scoring, and when to do it

Score each task **the moment it ends, before you discuss it.** Discussion
contaminates the rating — people revise upward once they've understood
something.

- **Outcome** — `✓` completed unaided · `H` completed after a hint · `✗` gave up
- **Ease** — ask them: *"How easy or difficult was that task?"* **1 = very
  difficult, 7 = very easy.** One number per task per person is what makes the
  evaluation chapter look like evaluation
- **Time** — start when you finish reading the task, stop when they say they're
  done
- **Hesitation** — the screen ID and what they said

**Never ask "was that easy?"** People say yes to be kind. Ask *"what were you
expecting there?"*

---

## Per-participant sheet — copy per person

```
Participant: P_     Date: ______     Device: ______     Theme: light / dark
Font scale: default / 200%           Reset the demo before starting: ☐

| Task | Outcome | Ease 1–7 | Time | Hesitated at | Quote |
|------|---------|----------|------|--------------|-------|
| T1  First impression   |   |   |   |   |   |
| T2  Fit stamp          |   |   |   |   |   |
| T3  Filtering          |   |   |   |   |   |
| T4  Gear tag           |   |   |   |   |   |
| T5  Deposit            |   |   |   |   |   |
| T6  Waiver             |   |   |   |   |   |
| T7  Price breakdown    |   |   |   |   |   |
| T8  Condition capture  |   |   |   |   |   |
| T9  Fairness / cycle   |   |   |   |   |   |
| T10 Owner + close      |   |   |   |   |   |

Closing three, in this order:
1. If you could change one thing, what would it be?
2. Was there any moment you didn't trust it, or felt unsure?
3. Would you use this — renting, lending, or swapping?
And: Was there anything you expected to be here that wasn't?
```

---

## Task-success matrix

Put this table early in the report body. One glance shows an examiner where the
design breaks.

| Task | Screens | P1 | P2 | P4 | P5 | Unaided | Mean ease | Finding |
|---|---|---|---|---|---|---|---|---|
| T1 First impression | S-04 | | | | | /4 | | — |
| T2 Fit stamp | S-04 → S-02 | | | | | /4 | | F-03 |
| T3 Filtering | S-04/06/05 | | | | | /4 | | F-02 |
| T4 Gear tag | S-07 | | | | | /4 | | — |
| **T5 Deposit** | **S-09** | | | | | **/4** | | **F-05** |
| T6 Waiver | S-10 | | | | | /4 | | F-09 |
| T7 Price breakdown | S-11 | | | | | /4 | | F-12 |
| T8 Condition capture | S-13 → S-15 | | | | | /4 | | F-06, F-07 |
| **T9 Fairness / cycle** | **S-21, S-23** | | | | | **/4** | | **F-08** |
| T10 Owner + close | S-24 → S-28 | | | | | /4 | | F-01 |

**Rank the tasks by mean ease when you have the numbers. Your worst two are your
findings.** In the proxy set those are T5 (3.4) and T9 (3.9); if your real data
puts different tasks at the bottom, the report follows the data — rewrite §6 of
[`07-report.md`](../07-report.md), don't reshape the data.

---

## Severity rating

Rate every problem. A list of problems is an opinion; a rated list is an
analysis.

| | Definition | Report it as |
|---|---|---|
| **Critical** | Stopped them completing the task, or they said they'd abandon | Fix before anything else |
| **Major** | Completed, but slowly or with the wrong mental model | Fix next |
| **Minor** | Noticed, mildly irritated, carried on | Note it |

Then write each finding as: **problem → severity → evidence (how many people,
one quote) → the specific change → whether you made it.** That is the structure
used in [`07-report.md §6`](../07-report.md#6-evaluation-findings-and-what-changed).

---

## Running the sessions

**Setup.** App on a phone, in *their* hands. Sit beside them, not opposite. Open
the cover screen and start **Flow A — Rent** so everyone begins in the same
place. Tap **Reset the demo** between participants — that is what puts the
fairness slider and the fit profile back where they were designed.

**The two rules.**

1. **Say nothing.** Every second of their confusion is a finding, and the moment
   you rescue them you have deleted it.
2. **Never defend the design.** If they misread a screen, write the misreading
   down word for word and move on.

**Cheap and impressive:** run one participant through task 3 at maximum system
font size, and one in dark mode. Both are built. Noticing that you checked
accessibility and theming under real use costs ten minutes and reads as
thoroughness.

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s09
```
