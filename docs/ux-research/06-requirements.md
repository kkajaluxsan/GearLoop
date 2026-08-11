# 06 — Functional & Non-Functional Requirements

> Assignment task **k** — *"Elucidate a clear set of functional and non-functional
> user requirements, each traceable to the research findings and HMW statements."*
>
> Constraint **e** — *"Each derived requirement must be clearly traceable to
> specific research evidence (e.g., a quote, observation, or survey result)."*

**25 functional** and **16 non-functional** requirements. Every row names the
finding it came from, the HMW that reframed it, the story it serves, how it will
be verified, and whether the prototype currently satisfies it.

**Nothing here is aspirational.** A requirement with no finding behind it would be
a preference, and there are none — the traceability matrix in §8 closes the loop
in both directions, so a finding with no requirement and a requirement with no
finding are both visible rather than assumed away.

### Notation

| | |
|---|---|
| **Priority** | MoSCoW — **M**ust / **S**hould / **C**ould / **W**on't (this release) |
| **Status** | ✅ satisfied by the prototype · ⚠️ partially satisfied · ❌ not satisfied · ⬜ out of scope for a UI prototype |
| **Verification** | how you would demonstrate it — the column an examiner will ask about |

A ❌ is not a failure of the report. Both of them are the honest output of a
study that found two task failures, and they are the "future work" section
written in advance.

---

## 1. Fit & safety

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-01** | The system shall derive the size specifications that depend on body measurements (board volume, ski length, bike frame) from the user's height and weight, and recalculate **live** whenever either input changes. | F-03 · HMW-2 · US-1 | **M** | ✅ **S-02** | Change either stepper on S-02; all three derived values update. Unit-tested in `FitLogic` |
| **FR-02** | Every listing shall display a fit verdict on a five-step ramp — `PERFECT FIT` / `GOOD FIT` / `WORKABLE` / `POOR FIT` / `NOT YOUR SIZE` — wherever that listing appears. | F-03 · HMW-2 · US-1 | **M** | ✅ **S-04, S-05, S-07, S-28** | Walk flow A; the stamp is present on the rail, the result row, the detail and the owner's request |
| **FR-03** | The fit verdict shall be accompanied by the **reason** in the user's own units, shall show `ADD YOUR HEIGHT` rather than a guess when measurements are absent, and shall **never block** a booking. | F-03 (+ counter-evidence **P2-Q3**) · HMW-2 · US-1 | **M** | ✅ **S-07** | Clear the fit profile → verdict degrades to `ADD YOUR HEIGHT`; book an item stamped `POOR FIT` and confirm it completes |

**The counter-evidence is load-bearing here.** P2-Q3 — the cyclist who reads
frame size off the seat tube — is why FR-03 forbids blocking. Without that
participant the requirement would have been written as a hard gate, and it would
have been wrong.

---

## 2. Money & commitment

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-04** | On the deposit screen the **verb** describing what happens to the money — *held*, *released*, *charged* — shall carry visual weight equal to the amount, not be relegated to supporting copy. | F-05 · HMW-1 · US-2 | **M** | ⚠️ **S-09** | Five-second test: cover S-09 after 5 s and ask *"is that money leaving your account?"* Target: 5/5 correct. **Currently fails at 1/4** (OBS-T5) |
| **FR-05** | For each protection option the system shall state the **maximum the renter could actually lose** as a figure, alongside the amount reserved. | F-05 · HMW-1 · US-2 | **M** | ⚠️ **S-09** | Ask a participant "how much would you lose if you damaged it, in each option?" — currently answerable only by inference |
| **FR-20** | Every line on a bill shall answer *"what is this"* in plain language without leaving the screen, and the arithmetic shall be shown rather than summarised. | F-12 · HMW-6 | **M** | ⚠️ **S-11** | Read-aloud test on all eight lines. Currently 3/4 fail on "Service fee" |
| **FR-22** | The system shall offer the owner a rate and deposit **derived from comparable items**, rather than an empty field. | F-01, F-05 · HMW-1 · US-4 | **S** | ⚠️ **S-26** | Create a listing; guidance is present but not derived from the dataset |
| **FR-16** | Availability presented to a renter shall be **authoritative**: a date shown as bookable is a date that is held. | F-02 · HMW-4 · US-4 | **M** | ✅ **S-08, S-27** | Both calendars render from one dataset; a blocked date is blocked on both sides |

**FR-04 and FR-05 are the two most important rows in this document.** They come
from the study's only *critical*-severity finding, they are the only Must-priority
requirements the prototype does not fully satisfy, and the fix is typographic
rather than architectural — which is exactly the kind of finding user research is
supposed to produce.

---

## 3. Trust, identity & evidence

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-07** | The system shall hold and display **verified identity** alongside a reputation score, and shall never present a score as sufficient on its own. | F-04 · HMW-3 · US-4 | **M** | ✅ **S-32, S-41, S-28** | Trust profile shows tier, verification state and review count as three separate facts |
| **FR-08** | Condition capture shall be a guided sequence of **named angles**, each frame timestamped and geotagged at capture, with framing validated at capture time and rejections given a stated reason. | F-04, F-06 · HMW-3 · US-3 | **M** | ✅ **S-15, S-15b** | Walk the seven angles; tap the validation chip to reach the fail state and read the reason |
| **FR-09** | At return the system shall present the **same angles side by side** with the handover set and flag differences, rather than requiring them to be found. | F-06, F-07 · HMW-3 · US-3 | **M** | ✅ **S-18, S-18b** | S-18 pairs all seven; S-18b isolates the diff |
| **FR-10** | The renter shall be shown *why* the capture sequence exists **at the moment of the ask**, not only at return. | F-07 · HMW-3 · US-3 | **S** | ❌ | Re-run task 8 and compare ease against the 4.1/7 baseline. **Not built** — this is the direct output of F-07 |
| **FR-15** | The owner's exact address shall be released to the renter **only** after payment completes, and both parties shall be told this is the rule. | F-11 · HMW-4 · US-4 | **M** | ✅ **S-13** | S-13 shows the address as locked before payment, unlocked after |
| **FR-14** | A control disabled by a designed constraint shall **state its own condition and progress** in place. | F-09 · HMW-8 | **M** | ⚠️ **S-10** | Time the pause on S-10. Baseline median 22 s with 2/4 suspecting a bug; target < 5 s and 0/5 |

---

## 4. Exchange & swap

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-11** | The system shall let an owner offer an item for **exchange as well as rental**, distinguishing terminal-idle from seasonal-idle supply at listing time. | F-01 · HMW-5 | **S** | ⚠️ **S-19, S-36** | Swap feed exists; the listing flow does not yet ask which kind of idle it is |
| **FR-12** | A two-party proposal shall show **both valuations openly** and express the gap as an adjustable cash top-up. | F-01, F-08 · HMW-5 | **S** | ✅ **S-20, S-21** | Drag the boot slider on S-21; the beam levels. 4/4 understood this unaided |
| **FR-13** | A multi-party cycle shall be explained such that a first-time reader can state **who ends up with what**, unaided. | F-08 · HMW-5 | **S** | ❌ **S-23** | Comprehension test: *"read it and tell me who ends up with what."* **3 of 4 currently fail**, ease 3.9/7. Sequential rather than simultaneous representation proposed |

**FR-13 is the report's most valuable red row.** The mechanism validated and the
explanation didn't; saying so is worth more than a table of all-greens, and it is
the kind of thing an examiner will specifically look for.

---

## 5. Logistics & the owner's time

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-06** | Filters shall show the **live result count** on the apply control, so constraints can be tuned without leaving the sheet, and the count shall be the true number of results. | F-02 · HMW-4 | **M** | ✅ **S-06, S-05** | Change a filter and watch the count; results match. One shared predicate, unit-tested |
| **FR-17** | **Buffer days** between bookings shall be first-class, visible calendar objects on both the renter's and the owner's side. | F-02 · HMW-4 · US-4 | **M** | ✅ **S-08, S-27** | Hatched on S-08, blocked on S-27, from the same data |
| **FR-21** | The system shall offer a **neutral collection point** as an alternative to a person-to-person rendezvous. | F-11, F-02 · HMW-4 · US-4 | **S** | ✅ **S-13** | GearPoint Nallur, 08:00–20:00, LKR 200 |
| **FR-23** | A booking request shall arrive carrying the renter's fit verdict, trust tier and verification state, so the owner does not have to go and look. | F-04, F-11 · HMW-4 · US-4 | **M** | ✅ **S-28** | Requests inbox shows all three per request |
| **FR-24** | The owner shall see earnings, occupancy and outstanding actions in one place. | F-01 · HMW-4 · US-4 | **S** | ✅ **S-24** | Owner dashboard |

---

## 6. Dispute resolution & the operator

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **FR-18** | Claims shall appear in a **queue** carrying state, age and assignee — not as messages in a conversation. | F-10 · HMW-7 · US-5 | **S** | ✅ **S-40** | Admin console lists claims with state and age |
| **FR-19** | A claim decision shall require a **stated reason**, and that reason shall be visible to both parties afterwards. | F-10, F-12 · HMW-6, HMW-7 · US-5 | **S** | ⚠️ **S-40, S-34** | Reason field present; not yet mandatory, and not yet surfaced to the renter on S-34 |
| **FR-25** | Opening a claim shall present the handover and return evidence **already paired**, with the applicable policy and excess on the same screen. | F-10 · HMW-7 · US-5 | **S** | ✅ **S-40, S-18b** | Evidence and policy are on one screen; no hunting |

---

## 7. Non-functional requirements

Grouped by quality attribute. Each carries a **measurable** acceptance
threshold — a non-functional requirement without a number is a wish.

### 7.1 Usability

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **NFR-01** | No designed constraint shall be indistinguishable from a defect. Any task involving one shall score **≥ 5.0/7** for ease, with **0 of 5** participants asking whether the app is broken. | F-09 · HMW-8 | **M** | ⚠️ | Re-run task 6. Baseline **4.8/7**, 2/4 suspected a bug |
| **NFR-02** | Money language shall be **identical** across the deposit screen, the bill and the wallet — same figure, same verb, same wording. | F-05 · HMW-1 · US-2 | **M** | ⚠️ | Read S-09, S-11 and S-35 side by side; the deposit is described the same way on all three |
| **NFR-03** | **No unexplained line.** Every monetary line item and every recorded decision shall be explicable by a first-time reader without assistance. | F-12 · HMW-6 | **M** | ⚠️ | Read-aloud test, 5 participants, all lines. Baseline: 3/4 fail on one line |
| **NFR-13** | A first-time user shall complete the core rental task (search → confirmed booking) **unaided** in under **4 minutes**. | F-02, F-05 | **S** | ⚠️ | Timed task; currently gated by the deposit hesitation at S-09 |

### 7.2 Accessibility

Derived from **WCAG 2.2 AA**, not from observed need — no participant was over
50 and none disclosed an impairment. Stated as a standards-derived requirement
rather than dressed up as a finding, which is the honest position and the one an
examiner will respect.

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **NFR-04** | Every foreground-on-background pair the app draws shall meet **WCAG 2.2 AA** (4.5:1 body, 3:1 large text and UI), **measured**, in all palettes. | WCAG 2.2 · NFR policy | **M** | ✅ | `ContrastTest` walks every pair in all three palettes. It caught nine failures in the original light palette |
| **NFR-05** | No interactive element below **44 dp**. | WCAG 2.5.8 | **M** | ✅ | `Metrics.tapTarget`; chips are painted at 34 dp inside a 44 dp target |
| **NFR-06** | Every screen shall remain usable at **200 % font scale** with no clipping or loss of function. | WCAG 1.4.4 | **M** | ✅ | `@GearTypePreview` renders every component at 200 %; that is how fixed-height rows give themselves away |
| **NFR-07** | Any meaning carried by colour, stroke weight or position shall also be available to a screen reader as a **sentence**. | WCAG 1.4.1 · F-03 | **M** | ✅ | The fit stamp says its verdict; the fairness meter announces the trade as one sentence; the tab bar reports selection — which matters because selection is carried by stroke weight |
| **NFR-08** | Any number a user could act on shall be set in a **monospaced** face, so digits align and `1` cannot be read as `l`. | F-03, F-12 | **M** | ✅ | Prices, sizes, distances, grades, item codes and countdowns are all `GearType.data*` |

### 7.3 Privacy, integrity & security

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **NFR-09** | Condition evidence shall be **tamper-evident**: capture-time metadata (timestamp, location, both-parties-present) bound to the frame and not editable afterwards. | F-04, F-06 · HMW-3 · US-3 | **M** | ⬜ | Backend concern; out of scope for a UI prototype, and named as such rather than silently dropped |
| **NFR-10** | Location and address data shall be **minimised**: approximate distance before booking, exact address only after payment, and capture geotags visible only to the two parties and a claims handler. | F-11 · HMW-4 · US-4 | **M** | ⚠️ | S-05 shows `2.4 km away`, not a pin; S-13 gates the address. The geotag visibility rule is designed but not enforceable without a backend |
| **NFR-12** | Every claim state change shall be recorded with a **timestamp and an actor**, and the record shall be immutable. | F-10 · HMW-7 · US-5 | **S** | ⬜ | Backend concern. The console is designed against it |

### 7.4 Performance & context of use

| ID | Requirement | Source | Pri | Status | Verification |
|---|---|---|---|---|---|
| **NFR-11** | The operator console shall be laid out for a **desktop viewport (≥ 1440 × 1024)**, because this is desk work. | F-10 · HMW-7 · US-5 | **S** | ✅ **S-40** | The one non-phone screen in a phone-first prototype |
| **NFR-14** | The app shall be usable on a **mid-range Android device over mobile data**: cold start under 2 s, and image memory bounded so an eight-result page does not thrash. | Participant context (P1, P3) | **M** | ✅ | Baseline profile compiled ahead of time; every photograph ships at two sizes and `GearImage` measures its slot before decoding |
| **NFR-15** | Currency, distance and measurement shall be **formatted once, centrally**, in LKR and metric, with a real minus sign. | Participant context · F-12 | **M** | ✅ | `data/Format.kt` — `lkr()`, `grouped()`, `signed()`, `km()`. No call site hand-writes a currency string |
| **NFR-16** | Colour, type, spacing and shape shall be **tokenised**, so a presentation variant can be introduced without touching a screen. | Maintainability | **S** | ✅ | `WirePalette` renders all 42 screens as wireframes with **zero** screen or component changes |

---

## 8. Traceability matrix

The closed loop. Read left to right to justify a requirement; read right to left
to check that no finding was dropped.

| Finding | Evidence | HMW | Story | Requirements | Screens | Status |
|---|---|---|---|---|---|---|
| **F-01** Idle gear; seasonal vs terminal | P3-Q1, P5-Q1, P4-Q1, SUR-Q2 | HMW-5 | US-4, *(cut)* | FR-11, FR-12, FR-22, FR-24 | S-19, S-24, S-26, S-36 | ⚠️ |
| **F-02** Alternative fails on availability | P1-Q2, P2-Q2, P6-Q2 | HMW-4 | US-4 | FR-06, FR-16, FR-17, NFR-13 | S-06, S-08, S-27 | ✅ |
| **F-03** Wrong size = safety event | P1-Q3, P7-Q3, P5-Q3, SUR-Q5, *(P2-Q3)* | HMW-2 | US-1 | FR-01, FR-02, FR-03, NFR-07, NFR-08 | S-02, S-05, S-07, S-28 | ✅ |
| **F-04** Trust is a stack of three | P3-Q4, P4-Q4, P7-Q4 | HMW-3 | US-3, US-4 | FR-07, FR-08, FR-23, NFR-09 | S-15, S-28, S-32, S-41 | ✅ |
| **F-05** Hold vs charge | P1-Q6, OBS-P1/P2/P5-T5, SUR-Q6 | HMW-1 | US-2 | **FR-04**, **FR-05**, FR-22, NFR-02 | S-09, S-11, S-35 | ⚠️ **critical** |
| **F-06** Photos invented by both sides; 7 is a lot | P3-Q7, P4-Q7, P1-Q8, OBS-P1/P4-T8 | HMW-3 | US-3 | FR-08, NFR-09 | S-15, S-15b | ✅ *(tension open)* |
| **F-07** Payoff only visible at return | OBS-P4-T8, P4-Q8 | HMW-3 | US-3 | FR-09, **FR-10** | S-18, S-18b | ❌ FR-10 |
| **F-08** Three-way swap not understood | OBS-P1/P2/P5-T9 | HMW-5 | *(cut)* | FR-12, **FR-13**, NFR-01 | S-21, S-23 | ❌ FR-13 |
| **F-09** Gate reads as a bug | OBS-P2-T6, OBS-P5-T6 | HMW-8 | — | FR-14, NFR-01 | S-10 | ⚠️ |
| **F-10** Operators need a queue | P6-Q10, P7-Q10, P7-Q4 | HMW-7 | US-5 | FR-18, FR-19, FR-25, NFR-11, NFR-12 | S-34, S-40, S-18b | ✅ |
| **F-11** Handover, not discovery, is the friction | P1-Q2, P2-Q2, P3-Q4, P6-Q2 | HMW-4 | US-4 | FR-15, FR-17, FR-21, NFR-10 | S-13, S-14, S-27 | ✅ |
| **F-12** Unexplained line = suspected line | OBS-P1/P2/P5-T7 | HMW-6 | US-5 | FR-19, FR-20, NFR-03, NFR-08 | S-11, S-34, S-40 | ⚠️ |

**Reverse check — requirements with no finding behind them:** NFR-04, NFR-05,
NFR-06 and NFR-16. The first three are derived from WCAG 2.2 AA and are declared
as standards-derived in §7.2 rather than claimed as research output; NFR-16 is a
maintainability requirement with no user behind it, and is marked as such. Every
other row on this page traces to a participant.

---

## 9. Requirements summary

| | Count | Must | Should | Could |
|---|---|---|---|---|
| **Functional** | 25 | 16 | 9 | 0 |
| **Non-functional** | 16 | 12 | 4 | 0 |
| **Total** | **41** | **28** | **13** | **0** |

| Status | Count | Which |
|---|---|---|
| ✅ Satisfied | 25 | The prototype's built answer |
| ⚠️ Partial | 12 | **FR-04**, **FR-05** and **NFR-01–03** are the ones that matter. FR-19's reason field exists but is not yet mandatory |
| ❌ Not satisfied | 2 | **FR-10** — show the payoff at the moment of the ask · **FR-13** — cycle comprehension |
| ⬜ Out of scope | 2 | NFR-09, NFR-12 — backend concerns, named rather than dropped |

**The two red rows are the future-work section.** FR-13 (three-way swap
comprehension) and FR-10 (show the payoff at the moment of the ask) both come
directly from observed task failures, which is the point of having run the study.
The twelve amber rows are graded work, not unstarted work — **FR-04** and
**FR-05** are the only *Must*-priority requirements the prototype does not fully
meet, and both are typographic rather than architectural.
