# 05 — How Might We Statements

> Assignment task **j** — *"Formulate How Might We (HMW) statements to reframe key
> pain points as actionable design opportunities."*
>
> Constraint **i** — *"At least 3 How Might We (HMW) statements must be
> formulated, each addressing a distinct pain point identified in the research."*

Eight statements, each addressing a **distinct** pain point. The brief asks for
three; eight is what the twelve findings actually produced, and cutting five to
hit the minimum would have meant discarding evidence.

**How these were written.** A good HMW is neither a solution in disguise
("How might we add a tooltip explaining deposits") nor so broad it is useless
("How might we make renting better"). Each one below is stated at the altitude of
the *user's problem*, and the "too narrow" and "too broad" versions are shown for
the first one so the calibration is visible rather than asserted.

Each statement carries the pain point it reframes, the evidence behind it, the
journey stage it sits at, and the requirements it becomes.

---

## HMW-1 — Hold versus charge

> ### How might we make a refundable deposit *feel* like money standing still rather than money leaving — at the exact moment a renter decides whether to book?

| | |
|---|---|
| **Pain point** | [PP-1.1](04-journey-maps.md#pain-points) — the deposit figure is read as a debit |
| **Finding** | **F-05** |
| **Evidence** | **P1-Q6** ⟨proxy⟩ *"Fifteen thousand? For a weekend? No."* → *"Oh — so it's not actually leaving? That's completely different. Why doesn't it say that?"* · **OBS-P1/P2/P5-T5** — 3 of 4 read it as a charge · task ease **3.4/7**, lowest of ten · **SUR-Q6** — median acceptable deposit moved **LKR 5,000 → 20,000** when the wording changed from "deposit" to "a hold that is released" |
| **Journey stage** | JM-1 stage 7 — the single lowest point on the renter's curve |
| **Severity** | **Critical.** 2 of 4 participants said they would abandon the booking here |
| **Becomes** | FR-04, FR-05, NFR-02 · story [US-2](03-user-stories.md#us-2--a-deposit-that-reads-as-a-hold) |

**Calibration.** *Too narrow:* "How might we add a tooltip to the deposit
amount?" — that is a solution, and it presumes the fix is explanatory text when
the evidence says it is typographic weight. *Too broad:* "How might we make
payment easier?" — unfalsifiable, and payment tested fine (**T7, 5.3/7**). The
statement above is at the right altitude because it names the *misreading*
(leaving vs standing still) and the *moment* (deciding whether to book), which
between them constrain the solution space without dictating it.

**Directions this opens**
- Give the verb the same visual weight as the amount, so `HELD` is as large as
  `15,000` rather than a caption beneath it
- State the **worst case as a figure**, per option — the number the renter could
  actually lose, not the number being reserved
- Present protection and deposit as a side-by-side comparison rather than a
  default with an opt-out
- Show the release condition and timeframe *before* payment, on the same screen
- Carry the identical figure and verb through to the bill and the wallet, so
  nothing has to be re-learned

---

## HMW-2 — Fit without expertise

> ### How might we let someone judge whether equipment is safe for their body, without requiring them to already know the specifications?

| | |
|---|---|
| **Pain point** | Sizing advice assumes vocabulary the beginner doesn't have — [Kavitha's frustrations](02-personas-empathy-maps.md#frustrations) |
| **Finding** | **F-03** |
| **Evidence** | **P1-Q3** ⟨proxy⟩ *"The board was way too small for me… then it pitched me into the reef. I've still got the scar."* · **P7-Q3** *"A helmet that's loose is worse than no helmet, because you think you're wearing one."* · **SUR-Q5** — 11/18 used wrong-sized equipment; **6 of those 11** framed it as injury or fear rather than performance |
| **Counter-evidence** | **P2-Q3** — an experienced cyclist reads frame size off the seat tube. The answer must not obstruct him |
| **Journey stage** | JM-1 stages 2 and 5 |
| **Severity** | **Major** — not a task failure in testing (**T2, 6.3/7**), but the highest-consequence one when it goes wrong |
| **Becomes** | FR-01, FR-02, FR-03, NFR-08 · story [US-1](03-user-stories.md#us-1--fit-as-a-safety-verdict) |

**Directions this opens**
- Derive the specification from what the user *does* know — height and weight —
  rather than asking for the specification itself
- Recalculate live and visibly, so the derivation is evidently a calculation and
  not a stored guess
- State a **verdict with its reason**, in the user's units: not "6'6"" but "0.4 L
  under your range"
- Treat the scale as a *safety ramp*, not a rating: `NOT YOUR SIZE` is a
  different kind of statement from `PERFECT FIT`, and the two ends should not
  look like ends of a star rating
- Inform without blocking — the expert who overrides it is not making an error
- Say the verdict aloud to a screen reader, since the ramp is carried partly by
  colour

---

## HMW-3 — Proof without a photoshoot

> ### How might we give an owner enough proof of condition that lending to a stranger feels no riskier than lending to a friend — without making the renter feel accused on a beach?

| | |
|---|---|
| **Pain points** | [PP-2.3](04-journey-maps.md#pain-points-1) (no neutral record) **and** [PP-1.5](04-journey-maps.md#pain-points) (seven angles feels like an accusation) — deliberately one statement, because these are two ends of the same trade |
| **Findings** | **F-04**, **F-06**, **F-07** |
| **Evidence** | **P4-Q4** ⟨proxy⟩ *"I'd want photos. Mine and theirs, at the start, so nobody can invent a scratch afterwards."* · **P3-Q7** *"You'd take pictures, wouldn't you."* — volunteered unprompted by every owner-side participant · **P1-Q8** *"Seven? …He's going to think I don't trust him."* · **OBS-P4-T8** *"Right — that's what all the photos were for"* · renter ease **4.1/7** vs owner ease **6.5/7** on the same task |
| **Journey stage** | JM-1 stage 11 (cost) · JM-2 stages 8 and 10 (payoff) · JM-3 stage 2 (precondition) |
| **Severity** | **Major** — an unresolved tension between two stakeholder categories, honestly reported |
| **Becomes** | FR-08, FR-09, FR-10, NFR-09, NFR-10 · story [US-3](03-user-stories.md#us-3--evidence-instead-of-an-argument) |

**Why this is one statement and not two.** Splitting it would let the design
optimise each side separately and quietly lose the trade — fewer angles makes the
renter comfortable and the owner unwilling to lend at all. The statement is
written with both constraints inside it because the constraint *is* the design
problem. This is the study's most honest finding and it should be defended, not
resolved by pretending one side didn't say what it said.

**Directions this opens**
- Guided, named angles rather than a free camera, so the return set is comparable
  like for like
- Timestamp and geotag at capture; validate framing at capture time and reject
  with a reason rather than accepting silently
- **Show the payoff at the moment of the ask** — a preview of the paired return
  comparison while the renter is being asked for the seventh photo, since **F-07**
  found the opinion only revises upward once the payoff is seen
- Frame the capture as *mutual protection*, both parties present, both signing —
  it protects the renter from an invented scratch as much as the owner from a
  real one
- Consider whether all seven angles are needed at every value tier, or whether
  the sequence should scale with the deposit at risk

---

## HMW-4 — The handover problem

> ### How might we make exchanging valuable equipment with a stranger feel scheduled and safe, rather than negotiated?

| | |
|---|---|
| **Pain points** | [PP-1.6](04-journey-maps.md#pain-points), [PP-2.2](04-journey-maps.md#pain-points-1), [PP-2.4](04-journey-maps.md#pain-points-1), [PP-2.6](04-journey-maps.md#pain-points-1), [PP-3.5](04-journey-maps.md#pain-points-2) |
| **Findings** | **F-02**, **F-11** |
| **Evidence** | **P2-Q2** ⟨proxy⟩ *"Finding it isn't the hard part. The hard part is 'I finish work at five and he's in Point Pedro'."* · **P3-Q4** *"I'm not giving my address to someone I've never met before they've paid."* · **P6-Q2** *"I can't hold a board for someone who might not turn up. Saturday I'll turn away four people and Tuesday I'll rent nothing."* · **P1-Q2** — the WhatsApp group answers on Monday |
| **Journey stage** | JM-1 stages 1, 4, 10 · JM-2 stages 5, 7 · JM-3 stage 5 |
| **Severity** | **Major** — **4 of 7 participants raised handover unprompted; none raised discovery** |
| **Becomes** | FR-06, FR-15, FR-16, FR-17, FR-21 · story [US-4](03-user-stories.md#us-4--lending-that-costs-minutes-not-evenings) |

**This is where the research contradicted the design brief.** The brief weighted
discovery heavily — S-04, S-05, S-06, S-29 — and handover lightly. The research
says nobody complained about *finding* equipment. That contradiction is reported
in [`07-report.md`](07-report.md#5-where-the-research-contradicted-the-brief)
rather than smoothed over, and the address-unlock-on-payment behaviour on S-13
exists because of **P3-Q4**, not because of the brief.

**Directions this opens**
- Availability that is a **commitment**: a date shown as bookable is a date that
  is held, because the product being displaced is a group chat with no state
- Buffer days as first-class, visible calendar objects on both sides, so
  turnaround time is a fact rather than an assumption
- Address released on completed payment, and both parties told that is the rule
- A **neutral collection point** as an alternative to a rendezvous — which also
  answers **PP-3.5**, since the operator turning four people away on Saturday is
  the obvious host
- Requests that arrive pre-vetted, so accepting is one action rather than an
  evening of messaging

---

## HMW-5 — Trading with someone who doesn't want your thing

> ### How might we let two people who cannot trade directly still trade — and understand, from the screen alone, what they have just agreed to?

| | |
|---|---|
| **Pain point** | [PP-2.7](04-journey-maps.md#pain-points-1) — terminal-idle equipment is bad rental supply — plus the comprehension failure at S-23 |
| **Findings** | **F-01**, **F-08** |
| **Evidence** | **P5-Q1** ⟨proxy⟩ *"I bought that board when I was fifteen kilos lighter. I'm not going back to it. I'd rather have something I'd actually ride."* · **OBS-P5-T9** *"So I give her the board and she gives me… no, wait. Who's giving me the kayak?"* · **OBS-P1-T9**, **OBS-P2-T9** — **3 of 4 could not state who ends up with what**; task ease **3.9/7**, second-lowest · all 4 understood the two-party fairness meter unaided, 3 of 4 used the word "fair" unprompted |
| **Journey stage** | JM-2 stages 1–2 (the alternative to listing) |
| **Severity** | **Major** — a known, reportable design failure on the product's most distinctive screen |
| **Becomes** | FR-11, FR-12, FR-13 · the [sixth, cut user story](03-user-stories.md#the-sixth-story-and-why-it-isnt-one-of-the-five) |

**The finding splits cleanly and that is useful.** The *mechanism* validated —
two-party fairness with a cash top-up was understood by everyone and produced the
word "fair" unprompted. The *three-party explanation* failed. That is a
representation problem, not a concept problem, and it is a far more valuable page
in the report than a pass would have been.

**Directions this opens**
- Show value openly on both sides, and make the gap a **quantity** rather than a
  judgement — the participants who moved the slider called the result "fair"
  because they had moved it themselves
- For the three-party case, try **sequential** rather than simultaneous: "you
  give X to A → A gives Y to B → B gives Z to you", one step at a time, rather
  than a ring shown all at once
- State the outcome as a single sentence — *"you end up with the kayak and pay
  LKR 4,000"* — before showing the mechanism
- Separate *terminal-idle* from *seasonal-idle* supply at listing time, so the
  owner who is finished with an item is offered a trade rather than a tenancy

---

## HMW-6 — The bill that explains itself

> ### How might we make every line of a bill self-describing, so that nobody pays while quietly suspicious?

| | |
|---|---|
| **Pain points** | [PP-1.4](04-journey-maps.md#pain-points), [PP-3.3](04-journey-maps.md#pain-points-2) |
| **Finding** | **F-12** |
| **Evidence** | **OBS-P1-T7** ⟨proxy⟩ *"What's this one? …Is that yours? What's it for?"* — of "Service fee" · **OBS-P5-T7**, **OBS-P2-T7** — 3 of 4 could explain every line **except** that one · all 4 correctly identified the multi-day discount and the coupon; 2 praised the arithmetic being shown |
| **Journey stage** | JM-1 stage 9 · JM-3 stages 5–7 |
| **Severity** | **Major** |
| **Becomes** | FR-19, FR-20, NFR-03 · supports [US-5](03-user-stories.md#us-5--a-claim-that-closes-with-its-reason-recorded) |

**The mechanism generalises past money.** An itemised bill with one unexplained
line is trusted *less* than a summarised one, because itemisation puts a
spotlight on the line nobody can name. The identical mechanism applies to a
claim decision with no stated reason — which is why this HMW feeds both the
payment screen and the admin console, and why **FR-19** requires a reason on
every decision rather than treating it as good manners.

**Directions this opens**
- Every line answers "what is this" in plain language, on tap, without leaving
  the screen
- Show the arithmetic rather than the summary — participants rewarded it
- Name fees by what they buy, not by what they are
- Apply the same rule to non-monetary decisions: a claim outcome carries its
  reason, and both parties can read it

---

## HMW-7 — The claim that closes

> ### How might we give the person judging a dispute the evidence, the policy and the decision in one place, so a claim closes in minutes with its reasoning intact?

| | |
|---|---|
| **Pain points** | [PP-3.1](04-journey-maps.md#pain-points-2), [PP-3.2](04-journey-maps.md#pain-points-2), [PP-3.4](04-journey-maps.md#pain-points-2), [PP-3.6](04-journey-maps.md#pain-points-2) |
| **Finding** | **F-10** |
| **Evidence** | **P7-Q10** ⟨proxy⟩ *"If it's in WhatsApp it's gone. I need to be able to open a thing, see what was agreed, and close it."* · **P6-Q10** *"The argument always happens three days later, when the board's already back on the rack."* · **P7-Q4** — already runs this manually on a paper register of ~60 items |
| **Journey stage** | JM-3, all stages |
| **Severity** | **Major** — SC-3 is off the critical path to a first transaction, but the product's trust argument collapses without them |
| **Becomes** | FR-18, FR-19, FR-22, NFR-11, NFR-12 · story [US-5](03-user-stories.md#us-5--a-claim-that-closes-with-its-reason-recorded) |

**Directions this opens**
- A **queue**, with state, age and assignee — not an inbox and not a thread
- Evidence already paired and differences already flagged when the claim opens;
  no hunting
- Policy and excess figures on the same screen as the evidence
- A **mandatory stated reason** on every decision, visible to both parties
- An immutable, timestamped, attributable trail that survives the six-month
  question
- A desk-shaped layout, because this is desk work — which is why the prototype's
  one non-phone screen is this one

---

## HMW-8 — Friction that explains itself

> ### How might we deliberately slow someone down at a safety-critical moment without them concluding the app is broken?

| | |
|---|---|
| **Pain point** | [PP-1.2](04-journey-maps.md#pain-points) |
| **Finding** | **F-09** |
| **Evidence** | **OBS-P2-T6** ⟨proxy⟩ *"Is it stuck? …Oh, it wants me to read it. That's cheeky."* · **OBS-P5-T6** · median pause **22 s**; **2 of 4 asked aloud whether the app had frozen**; task ease **4.8/7** · once explained, **4 of 4 rated the gate as fair** |
| **Journey stage** | JM-1 stage 8 |
| **Severity** | **Major** — the constraint is defensible; its legibility is not |
| **Becomes** | FR-14, NFR-01 |

**The distinction worth defending in the viva.** The gate was *not* rejected —
every participant endorsed it once they understood it. What failed was that a
disabled control with no stated cause is indistinguishable from a bug, and a
designed constraint that is first read as a defect spends the user's goodwill
before it earns it. That is a communication fix, not a decision to reverse.

**Directions this opens**
- The disabled control states its own condition, in place: *"Read to the end to
  accept"* rather than a grey rectangle
- Show progress through the gate, so the requirement is measurable rather than
  mysterious
- Reserve this pattern for genuinely safety-critical content, so it does not
  become ambient friction the user learns to resent

---

## Coverage: every pain point is reframed

| HMW | Distinct pain point | Findings | Journeys | Severity | Requirements |
|---|---|---|---|---|---|
| **HMW-1** | Deposit read as a charge | F-05 | JM-1 | **Critical** | FR-04, FR-05, NFR-02 |
| **HMW-2** | Sizing needs expertise the user lacks | F-03 | JM-1 | Major | FR-01–03, NFR-08 |
| **HMW-3** | No neutral condition record ↔ capture feels accusatory | F-04, F-06, F-07 | JM-1, JM-2, JM-3 | Major | FR-08–10, NFR-09, NFR-10 |
| **HMW-4** | Handover logistics and address exposure | F-02, F-11 | JM-1, JM-2, JM-3 | Major | FR-06, FR-15–17, FR-21 |
| **HMW-5** | Terminal-idle supply ↔ three-way trade not understood | F-01, F-08 | JM-2 | Major | FR-11–13 |
| **HMW-6** | An unexplained line item is a suspected one | F-12 | JM-1, JM-3 | Major | FR-19, FR-20, NFR-03 |
| **HMW-7** | Disputes have no queue and no memory | F-10 | JM-3 | Major | FR-18, FR-19, FR-22, NFR-11, NFR-12 |
| **HMW-8** | Designed friction reads as a defect | F-09 | JM-1 | Major | FR-14, NFR-01 |

Eight statements, eight distinct pain points, all twelve findings represented,
all three personas covered, and every statement lands on at least one
requirement. The brief's minimum of three is met by HMW-1, HMW-2 and HMW-3
alone — the other five are the ones the evidence produced anyway.
