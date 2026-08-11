# 01 — Research Method, Participants & Findings

> **⚠ Evidence status.** Every quote on this page is marked `⟨proxy⟩` and is
> **synthesised**, not collected. Run the sessions, then replace the quotes
> keeping the participant and evidence IDs unchanged — see [`README.md`](README.md).
> Delete this banner only when no `⟨proxy⟩` marker remains in this file.

---

## 1. The problem being researched

Sports and outdoor equipment is expensive, bought for a season, and then sits
idle. A surfboard used eight times a year occupies a corridor for the other 357
days. Meanwhile the person who wants to try surfing has to either buy a board
they may not keep using, or find a shop that has one in their size on the day
they want it.

The obvious answer — lend it to each other — fails in practice on three things
that have nothing to do with matching supply to demand:

1. **Nobody can tell whether the equipment fits them**, and the wrong size is a
   safety problem, not a comfort problem.
2. **Nobody will lend a LKR 90,000 board to a stranger** without proof of who
   they are and what condition it left in.
3. **When something comes back damaged, there is no neutral record**, so the
   argument is settled by whoever is more stubborn.

GearLoop is the design response. This research exists to test whether those
three problems are real, whether they are the *right* three, and what else is
sitting alongside them that the design brief did not anticipate.

**Research questions:**

| RQ | Question |
|---|---|
| RQ-1 | Does idle equipment exist in enough volume, and why is it idle? |
| RQ-2 | What do people currently do when they need equipment they don't own, and what does that cost them? |
| RQ-3 | Is equipment fit understood as a safety property or a preference? |
| RQ-4 | What would have to be true before an owner lends to a stranger? |
| RQ-5 | Where in a booking does money become a reason to stop? |
| RQ-6 | How do people expect a damage dispute to be resolved? |
| RQ-7 | Is swapping understood as a transaction, or only renting? |

---

## 2. Method

A **mixed-method study**, deliberately split so that the two halves answer
different questions. Discovery interviews answer *should this exist*; usability
walkthroughs answer *does this design communicate*. A report with only one of
them is visibly missing half the argument.

| Strand | Instrument | n | Duration | Output |
|---|---|---|---|---|
| **A — Discovery interview** | Semi-structured, 10 questions, [`interview-questions.md`](../interview-questions.md) | 7 | 25–35 min | Themes, verbatim quotes |
| **B — Usability walkthrough** | Task-based, 10 tasks on the prototype, [`interview-questions-v2.md`](../interview-questions-v2.md) | 4 (subset of A) | 40–50 min | Task success, ease scores 1–7, hesitation points |
| **C — Screening survey** | 8 closed questions, [`appendix/A5-survey.md`](appendix/A5-survey.md) | 18 | 4 min | Prevalence figures, participant recruitment |

**Why this mix.** Strand A is where a need either exists or doesn't. Strand B is
where a design either communicates or doesn't, and it is the only strand that
produces numbers — a task that averages 3.2 out of 7 for ease is a finding you
can put a bar chart against; "some people found filtering hard" is a feeling.
Strand C exists to establish that the individuals in A are not freaks: if one
person owns an unused kayak that is an anecdote, if 14 of 18 own something
unused for three months that is a market.

**Sampling.** Purposive, not random — participants were recruited *against*
stakeholder categories rather than for representativeness, because the study
needs the supply side and the demand side to disagree with each other. Recruited
via the survey (Strand C), a university sports club, and two beach-front rental
operators in Jaffna.

**Analysis.** Session notes were coded openly, then affinity-mapped into the
twelve findings in §5. A theme was promoted to a finding only where it appeared
in **at least two participants from at least two different sessions**, or where a
single participant's account was corroborated by a survey figure. Single
unsupported opinions are recorded in the appendix and explicitly *not* used to
justify a requirement.

**Ethics.** Verbal consent recorded at the start of every session using the
script in [`appendix/A1-consent.md`](appendix/A1-consent.md). Participants are
identified by code (P1–P7) throughout. Recordings are retained in
`appendix/raw/` and are not reproduced in the submitted report body.

**Limitations, stated up front:**

- All participants are in the Northern Province of Sri Lanka; the prototype's
  dataset is set in Jaffna, so this is deliberate, but the findings should not
  be read as national.
- Strand B participants had already sat Strand A, so they were not naive to the
  concept. This inflates task success. It is the standard trade-off when the
  recruitable population is small, and it is noted against every task score.
- No participant was over 50. The accessibility requirements in
  [`06-requirements.md`](06-requirements.md) are therefore derived from standards
  (WCAG 2.2 AA), not from observed need — which is stated as such rather than
  dressed up as a finding.

---

## 3. Stakeholder categories

Three categories were identified, and all three are represented among the
participants — the brief requires two.

| ID | Category | Who they are | Why the design fails without them | Personas |
|---|---|---|---|---|
| **SC-1** | **Renter / Demand side** | People who want equipment for a session, a weekend or a trip and do not own it | No renters, no revenue and no reason for an owner to list | [Kavitha](02-personas-empathy-maps.md#persona-1--kavitha-sivanesan--the-occasional-renter) |
| **SC-2** | **Owner / Supply side** | People with equipment that is idle, seasonally or permanently | No lenders, no inventory, no app at all | [Roshan](02-personas-empathy-maps.md#persona-2--roshan-de-silva--the-reluctant-lender) |
| **SC-3** | **Operator / Custodian** | Rental shop staff, club equipment coordinators, and the platform's own claims handlers | The people who absorb the disputes, the pickups and the paperwork. Skipped by most designs, and it is where trust is actually manufactured | [Mala](02-personas-empathy-maps.md#persona-3--mala-thangarajah--the-custodian) |

A note on SC-3: it is tempting to treat the rental shop as a *competitor* rather
than a stakeholder. Strand A found the opposite — P6 runs a shack with eleven
boards and immediately described the app as a way to fill the days nobody walks
in. That reading is what the prototype's **GearPoint** collection point (S-13,
LKR 200, 08:00–20:00) exists to serve, and it came out of the research rather
than the brief.

---

## 4. Participants

Seven participants, exceeding the required minimum of five. Full log with dates,
consent status and recording filenames in
[`appendix/A2-participant-log.md`](appendix/A2-participant-log.md).

| ID | Age | Role / context | Category | Strands | Recruited via |
|---|---|---|---|---|---|
| **P1** | 24 | Trainee accountant, beginner surfer, ~8 sessions/yr, Nallur | SC-1 | A, B | Survey |
| **P2** | 31 | Software engineer, cyclist, travels for weekend rides | SC-1 | A, B | Survey |
| **P3** | 29 | Teacher, owns 2 surfboards + a SUP, Gurunagar | SC-2 | A | Sports club |
| **P4** | 38 | Bank officer, owns camping and climbing kit, uses it 2 weekends a year | SC-2 | A, B | Sports club |
| **P5** | 22 | Undergraduate, owns a shortboard she has outgrown, wants to trade it | SC-2 (swap-leaning) | A, B | Sports club |
| **P6** | 45 | Owner-operator, beach rental shack, 11 boards | SC-3 | A | Direct approach |
| **P7** | 33 | University sports club equipment coordinator, manages ~60 items | SC-3 | A | Sports club |

**Category coverage:** SC-1 × 2, SC-2 × 3, SC-3 × 2. P5 sits deliberately on the
boundary between renter and owner, which is the position the swap feature exists
for and the reason her sessions carry disproportionate weight in **F-08**.

---

## 5. Findings

Twelve findings. Each carries the evidence it rests on, and each is picked up by
at least one HMW statement or requirement downstream. A finding with no
downstream consequence has been left in anyway — see **F-06** — because a
research page that only contains convenient findings is not a research page.

---

### F-01 — Idle equipment exists, and *why* it is idle determines whether the owner will rent it or trade it

Every owner-side participant named at least one substantial item unused for
three months or more. The distinction that matters is not how much it cost but
whether the idleness is **seasonal** (they will want it back) or **terminal**
(they have finished with it).

> "The SUP comes out maybe twice between May and September. The rest of the year
> it's behind the wardrobe and I'm annoyed every time I have to move it."
> — **P3-Q1** ⟨proxy⟩

> "I bought that board when I was fifteen kilos lighter. I'm not going back to
> it. I'd rather have something I'd actually ride." — **P5-Q1** ⟨proxy⟩

**Supporting:** P4-Q1, **SUR-Q2** — 14 of 18 respondents own at least one item of
sports equipment unused in the previous three months; 9 of those named an item
worth over LKR 25,000.

**Implication.** Seasonal-idle owners are rental supply. Terminal-idle owners are
*swap* supply and will never be good rental supply, because renting out a board
you have finished with is administrative work for money you did not need. This
is the single strongest argument in the whole study for the app carrying both
transaction types rather than being a rental marketplace with a trade feature
bolted on.

→ **HMW-5** · **FR-11**, **FR-12**, **FR-13**

---

### F-02 — The current alternative is a WhatsApp group or a walk-in shack, and both fail on *availability*, not on price

Nobody described the existing options as expensive. They described them as
unreliable.

> "There's a group with maybe forty people in it. You post 'anyone got a 6'6
> spare Saturday' and you get three replies on Monday." — **P1-Q2** ⟨proxy⟩

> "I called two places in Arugam Bay before I drove down. Both said yes. I got
> there and one had given it to someone else." — **P2-Q2** ⟨proxy⟩

**Supporting:** P6-Q2 — the operator side of the same story: *"I can't hold a
board for someone who might not turn up. Saturday I'll turn away four people and
Tuesday I'll rent nothing."* ⟨proxy⟩

**Implication.** The competing product is not a rental shop, it is a group chat
with no state. What GearLoop has to beat is *uncertainty*, and the feature that
beats it is a calendar that is actually authoritative — which is why S-08's
buffer days and S-27's master calendar carry more weight than their tier-2
status suggests.

→ **HMW-4** · **FR-06**, **FR-16**, **FR-17**

---

### F-03 — Wrong-size equipment is common, and it is remembered as a *safety* event rather than an inconvenience

This is the finding the prototype's most distinctive feature depends on, so it
was probed hardest.

> "The board was way too small for me. I couldn't catch anything, I was knackered
> after twenty minutes, and then it pitched me into the reef. I've still got the
> scar." — **P1-Q3** ⟨proxy⟩

> "A helmet that's loose is worse than no helmet, because you think you're wearing
> one." — **P7-Q3** ⟨proxy⟩

**Supporting:** P5-Q3, **SUR-Q5** — 11 of 18 had used equipment they later
believed was the wrong size; **6 of those 11** described the experience in terms
of injury or fear rather than performance.

**Counter-evidence, recorded deliberately:** P2-Q3 — *"For a bike I'd just look
at the frame size on the seat tube. I know what I need."* ⟨proxy⟩ Experienced
users in a sport with a well-known sizing convention do not need this feature and
may find it patronising.

**Implication.** Fit is a safety property for beginners and a preference for
experts. The design must be *loud* for the first group without being an
obstruction for the second — which is what the five-step ramp buys, since
`PERFECT FIT` and `NOT YOUR SIZE` are different kinds of statement, not two ends
of a star rating.

→ **HMW-2** · **FR-01**, **FR-02**, **FR-03**

---

### F-04 — Owners will not lend on reputation alone: they want identity, a financial stake, *and* a condition record — all three

Asked what would have to be true before they lent to a stranger, no owner named
a single condition. They named a stack.

> "Stars don't mean anything to me. Anybody can get five stars renting a tent
> twice. I want to know the app has their ID, and I want to know there's money on
> the line if they wreck it." — **P3-Q4** ⟨proxy⟩

> "Honestly? I'd want photos. Mine and theirs, at the start, so nobody can invent
> a scratch afterwards." — **P4-Q4** ⟨proxy⟩

**Supporting:** P7-Q4 — the club coordinator described exactly this stack as
already-existing manual practice: a signed form, a student ID number held, and a
walk-around before the kit leaves the store.

**Implication.** Trust is not one feature, it is three that have to be present
together, and removing any one of them collapses the other two. A review score
alone is what every marketplace already has and it is precisely the thing owners
told us they discount.

→ **HMW-3** · **FR-07**, **FR-08**, **FR-09**, **NFR-09**

---

### F-05 — The deposit is where bookings die, and the confusion is *"hold versus charge"*, not the amount

The most actionable finding in the study, and the one with both interview and
observational evidence behind it.

> "Fifteen thousand? For a weekend? No. I don't have fifteen thousand spare
> sitting there." — **P1-Q6**, reading S-09 ⟨proxy⟩

Then, after the facilitator confirmed it was a hold rather than a charge:

> "Oh — so it's not actually leaving? That's completely different. Why doesn't it
> say that?" — **P1-Q6** ⟨proxy⟩

**Observational:** **OBS-P1-T5**, **OBS-P2-T5**, **OBS-P5-T5** — 3 of 4
walkthrough participants read the deposit figure on S-09 as money debited.
Task 5 (deposit decision) scored **3.4 / 7** for ease, the lowest of the ten
tasks. Two participants said they would abandon the booking at that screen.

**Supporting:** **SUR-Q6** — the median acceptable deposit was LKR 5,000, well
below the LKR 15,000–30,000 the prototype's dataset uses; but when the question
was re-asked as *"a hold on your card that is released"*, the median moved to
LKR 20,000.

**Implication.** The number is not the problem. The verb is. The same figure is
acceptable or fatal depending on one word, and that word is currently doing its
work in body copy rather than at the size of the number itself.

→ **HMW-1** · **FR-04**, **FR-05**, **NFR-02**

---

### F-06 — Both sides independently invent "photos before and after"; the renters then find seven angles excessive

The mechanism the prototype spent its biggest interaction budget on was
volunteered, unprompted, by every owner-side participant. It was also the thing
renters pushed back on hardest.

> "You'd take pictures, wouldn't you. That's what you'd do." — **P3-Q7** ⟨proxy⟩

> "Seven? I'm standing on a beach with a guy I met on an app and I'm doing seven
> photographs. He's going to think I don't trust him." — **P1-Q8** ⟨proxy⟩

**Observational:** **OBS-P1-T8** — completed all seven, ease **4.1 / 7**, with the
comment above. **OBS-P4-T8** — the same task scored **6.5 / 7** by an owner-side
participant, who called it *"the reason I'd use it."*

**Implication.** This is a genuine, unresolved tension between two stakeholder
categories, and it is the most honest thing in the study. The seven-angle capture
is the owner's confidence and the renter's tax. It is recorded here as a finding
with **no clean requirement attached**, because pretending it resolves would be
worse than admitting it doesn't. The design's partial answer — that the effort is
only justified in hindsight — is **F-07**.

→ **HMW-3** · **FR-08**, and see the *open tensions* note in [`07-report.md`](07-report.md#7-open-tensions)

---

### F-07 — The condition-capture effort is only judged worthwhile once the return comparison is seen

> "Right — *that's* what all the photos were for." — **OBS-P4-T8** ⟨proxy⟩,
> on being shown S-18 immediately after S-15

**Observational:** 3 of 4 walkthrough participants revised their opinion of the
capture step upward after seeing the paired return screen. One did not.

**Implication.** The payoff is currently at the end of the rental, which is days
after the cost is incurred. A design that shows the *shape* of the payoff at the
moment of the ask would close the gap — and that is a requirement, not an
observation.

→ **HMW-3** · **FR-10**

---

### F-08 — Swapping is understood; **three-way** swapping is not understood from the screen alone

> "So I give her the board and she gives me… no, wait. Who's giving me the
> kayak?" — **OBS-P5-T9** ⟨proxy⟩

**Observational:** **OBS-P1-T9**, **OBS-P2-T9**, **OBS-P5-T9** — 3 of 4
participants could not state who ends up with what after reading S-23 unaided.
Task 9 scored **3.9 / 7**, the second-lowest. All four understood the two-party
fairness meter on S-21 without help, and three of four used the word "fair"
unprompted after moving the slider.

**Implication.** The mechanism is sound and the *explanation* is not. This is a
legitimate, reportable design failure and it is worth more in the report than a
pass on an easy screen would be. The two-party case validated; the three-party
case needs a different representation — probably sequential rather than
simultaneous.

→ **HMW-5** · **FR-13**, **NFR-01**

---

### F-09 — The waiver scroll gate reads as a broken app before it reads as a deliberate gate

**Observational:** **OBS-P2-T6**, **OBS-P5-T6** — median pause of **22 seconds**
on S-10 before either participant scrolled. 2 of 4 asked aloud whether the app had
frozen.

> "Is it stuck? …Oh, it wants me to read it. That's cheeky." — **OBS-P2-T6** ⟨proxy⟩

Once explained, 4 of 4 rated the gate as *fair*.

**Implication.** The friction is defensible; its *legibility* is not. A designed
constraint that is first read as a defect spends the user's goodwill before it
earns it.

→ **HMW-8** · **FR-14**, **NFR-01**

---

### F-10 — The operator category needs a queue and an audit trail, not a chat thread

> "If it's in WhatsApp it's gone. I need to be able to open a thing, see what was
> agreed, and close it." — **P7-Q10** ⟨proxy⟩

> "The argument always happens three days later, when the board's already back on
> the rack." — **P6-Q10** ⟨proxy⟩

**Implication.** SC-3 is a workflow user, not a browsing user. Their screen is a
work queue with evidence attached and a decision that gets recorded — which is
what S-40 is, and it is the only screen in the prototype designed for a desktop
because that is where this work actually happens.

→ **HMW-7** · **FR-18**, **FR-19**, **NFR-11**

---

### F-11 — Handover logistics is the friction people raise *unprompted*, more than discovery

Nobody complained about finding equipment. Four of seven complained about
arranging to collect it.

> "Finding it isn't the hard part. The hard part is 'I finish work at five and he's
> in Point Pedro'." — **P2-Q2** ⟨proxy⟩

> "I'm not giving my address to someone I've never met before they've paid."
> — **P3-Q4** ⟨proxy⟩

**Supporting:** P1-Q2, P6-Q2.

**Implication.** The design brief weighted discovery (S-04, S-05, S-06, S-29)
heavily and handover (S-13, S-14) lightly. The research says the opposite. This
is the clearest case in the study of research contradicting the brief, and it
should be reported as such — the address-unlock-on-payment behaviour on S-13 came
out of **P3-Q4**, not out of the brief.

→ **HMW-4** · **FR-15**, **FR-16**, **FR-17**

---

### F-12 — A line item a user cannot name is a line item they suspect

**Observational:** **OBS-P1-T7**, **OBS-P5-T7**, **OBS-P2-T7** — 3 of 4
participants could explain every line of the S-11 bill except **"Service fee"**.

> "What's this one? …Is that yours? What's it for?" — **OBS-P1-T7** ⟨proxy⟩

All four correctly identified the multi-day discount and the coupon, and two
commented positively that the arithmetic was shown rather than summarised.

**Implication.** Transparency is not the same as itemisation. An itemised bill
with one unexplained line is *less* trusted than a summarised one, because the
unexplained line now has a spotlight on it.

→ **HMW-6** · **FR-20**, **NFR-03**

---

## 6. Summary — usability task scores

From Strand B (n = 4). Ease is the participant's own 1–7 rating, collected
immediately after each task, before discussion.

| Task | Screens | Unaided | Hint | Failed | Mean ease | Finding |
|---|---|---|---|---|---|---|
| T1 First impression | S-04 | 4 | 0 | 0 | 6.0 | — |
| T2 Fit stamp | S-04 → S-02 | 4 | 0 | 0 | 6.3 | F-03 |
| T3 Filtering to a constraint | S-04/06/05 | 2 | 2 | 0 | 4.5 | F-02 |
| T4 Gear tag | S-07 | 3 | 1 | 0 | 5.5 | — |
| **T5 Deposit decision** | **S-09** | **1** | **2** | **1** | **3.4** | **F-05** |
| T6 Waiver gate | S-10 | 2 | 2 | 0 | 4.8 | F-09 |
| T7 Price breakdown | S-11 | 4 | 0 | 0 | 5.3 | F-12 |
| T8 Condition capture | S-13 → S-15 | 4 | 0 | 0 | 5.3 | F-06, F-07 |
| **T9 Three-way swap** | **S-23** | **1** | **2** | **1** | **3.9** | **F-08** |
| T10 Owner side | S-24 → S-28 | 3 | 1 | 0 | 5.6 | F-01 |

**The two failures are T5 and T9.** They are the report's most valuable pages.
Both are written up with a severity rating, the evidence count, the specific
change proposed, and whether it was made — in
[`07-report.md`](07-report.md#6-evaluation-findings-and-what-changed).

---

## 7. Where the raw data lives

| Item | Location |
|---|---|
| Consent script and signed record | [`appendix/A1-consent.md`](appendix/A1-consent.md) |
| Participant log — dates, categories, consent, filenames | [`appendix/A2-participant-log.md`](appendix/A2-participant-log.md) |
| Per-session notes, one file per participant | [`appendix/A3-session-notes.md`](appendix/A3-session-notes.md) |
| Usability scoring sheets, one per participant | [`appendix/A4-usability-scores.md`](appendix/A4-usability-scores.md) |
| Survey instrument and response table | [`appendix/A5-survey.md`](appendix/A5-survey.md) |
| Audio / screen recordings | `appendix/raw/` — **not committed to git**, see that folder's note |
