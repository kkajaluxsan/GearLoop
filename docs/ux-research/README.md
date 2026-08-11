# GearLoop — User Research & Requirements

Deliverables **h** to **l** of the UX assignment, for the GearLoop peer-to-peer
sports gear rental and exchange app.

---

## ⚠ Evidence status — read this before you submit

**The participant data in [`01-research-data.md`](01-research-data.md) is
synthesised proxy data.** It is modelled on the two interview guides already in
this repo ([Version A](../interview-questions.md), [Version B](../interview-questions-v2.md))
and on the prototype's actual screens, prices and copy — but it is *scaffolding,
not research*. No one has been interviewed yet.

Your brief is explicit on this point:

> *"Raw research data (recordings/notes/completed questionnaires) must be
> retained and referenced in the appendix; unsupported claims about user needs
> will not be credited."*

So nothing in this folder is submittable as primary research until you have run
the sessions. **The good news is that swapping real data in costs you an hour,
not a rewrite** — because everything downstream references stable IDs rather
than prose:

```
P3-Q4          participant 3, interview guide A, question 4
OBS-P2-T5      observation of participant 2 during usability task 5
SUR-Q6         survey instrument, question 6
F-05           finding 5, derived from the above
HMW-1          how-might-we statement 1
US-2           user story 2
FR-07 / NFR-03 functional / non-functional requirement
```

Run the interviews, fill in [`appendix/A2-participant-log.md`](appendix/A2-participant-log.md)
and [`appendix/A3-session-notes.md`](appendix/A3-session-notes.md), then replace
the quotes in `01-research-data.md` **keeping the same IDs**. Where a real
finding contradicts a proxy one, say so in the report — a contradicted
assumption is a better page than a confirmed one, and markers know it.

Every proxy quote in this folder is marked `⟨proxy⟩`. Search for that string;
when it returns nothing, you're done.

---

## Assignment coverage

| Task | Requirement | Where |
|---|---|---|
| **h** | 5 user stories in *As a… I want… so that…* form, derived from research | [`03-user-stories.md`](03-user-stories.md) |
| **i** | User journey per persona, pain points + opportunities | [`04-journey-maps.md`](04-journey-maps.md) |
| **j** | How Might We statements reframing pain points | [`05-hmw-statements.md`](05-hmw-statements.md) |
| **k** | Functional + non-functional requirements, traceable | [`06-requirements.md`](06-requirements.md) |
| **l** | Assembled report + viva preparation | [`07-report.md`](07-report.md) |

### The "things to concern" checklist

| # | Constraint | Status |
|---|---|---|
| b | Minimum 5 participants (real or proxy) | **7 planned** — P1–P7 |
| c | At least 2 stakeholder categories | **3 identified** — SC-1 Renter, SC-2 Owner, SC-3 Operator |
| d | Raw data retained and referenced in appendix | Structure in [`appendix/`](appendix/) — **you must fill it** |
| e | Each requirement traceable to specific evidence | Traceability matrix, [`06-requirements.md`](06-requirements.md#8-traceability-matrix) |
| f | Personas cover the 3 categories, each with an empathy map | [`02-personas-empathy-maps.md`](02-personas-empathy-maps.md) — 3 personas, 3 empathy maps |
| g | User stories traceable to evidence, not assumption | Every story in [`03-user-stories.md`](03-user-stories.md) carries an evidence row |
| h | At least one journey map with pain points | **3 maps** — one per persona |
| i | At least 3 HMW statements, distinct pain points | **8 statements** across 8 distinct pains |

Item **a** — topic selection and the module coordinator's sign-off — is
administrative and sits outside this folder. Confirm GearLoop's problem
("peer-to-peer sharing of underused sports equipment") is on the issued list and
that your group holds one of the five slots for it, before you spend any more
time here.

---

## Reading order

1. [`01-research-data.md`](01-research-data.md) — method, participants, findings **F-01…F-12**. Everything else is derived from this file; read it first or nothing downstream will make sense.
2. [`02-personas-empathy-maps.md`](02-personas-empathy-maps.md) — three personas, three empathy maps.
3. [`03-user-stories.md`](03-user-stories.md) — the five stories.
4. [`04-journey-maps.md`](04-journey-maps.md) — three journeys with emotion curves.
5. [`05-hmw-statements.md`](05-hmw-statements.md) — eight reframed opportunities.
6. [`06-requirements.md`](06-requirements.md) — 25 functional, 16 non-functional, with the traceability matrix.
7. [`07-report.md`](07-report.md) — the assembled submission and the viva questions you should expect.

## Relationship to the prototype

The prototype in this repository is the *design response* to these requirements,
not their source. Where a requirement is already satisfied by a built screen the
requirement table names the screen (`S-07`, `S-15`, `S-40`…) so you can walk from
a research quote to a pixel in front of an examiner. Where a requirement is
**not** yet satisfied, the table says so — those rows are the honest "future
work" section, and they are worth more than a table of all-greens.

Jump to any screen during the viva:

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s15
```
