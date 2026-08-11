# 03 — User Stories

> Assignment task **h** — *"Write 5 user stories (in the format "As a [user], I
> want [goal], so that [benefit]") derived directly from the research findings."*
>
> Constraint **g** — *"User stories must be traceable to specific research
> evidence (interview/survey findings), not invented from assumption."*

Five stories. Each carries the findings it came from, the raw evidence behind
those findings, the persona it belongs to, acceptance criteria, and where the
prototype answers it. **No story here was written before its evidence.** The
order in which this file was produced was findings → HMW → stories, and the
sixth candidate story at the bottom is included precisely because it shows what
was cut and why.

Coverage: two renter stories, two owner stories, one operator story — all three
stakeholder categories, weighted toward the two sides of the market that have to
both be present for the product to exist at all.

---

## US-1 — Fit as a safety verdict

> **As a** beginner renter who doesn't know what size equipment I need,
> **I want** the app to tell me whether a specific item fits my body and *why*,
> **so that** I don't spend a paid day fighting equipment that was never going to
> work for me — or get hurt on it.

| | |
|---|---|
| **Persona** | [Kavitha Sivanesan](02-personas-empathy-maps.md#persona-1--kavitha-sivanesan--the-occasional-renter) · SC-1 |
| **From findings** | **F-03** |
| **Evidence** | **P1-Q3** ⟨proxy⟩ *"The board was way too small for me… then it pitched me into the reef. I've still got the scar."* · **P7-Q3** *"A helmet that's loose is worse than no helmet, because you think you're wearing one."* · **SUR-Q5** — 11/18 had used equipment they believed was the wrong size; 6 of those 11 described it in terms of injury or fear |
| **Counter-evidence** | **P2-Q3** — an experienced cyclist reads frame size off the seat tube and does not need this. The story must not obstruct him |
| **Reframed by** | [HMW-2](05-hmw-statements.md#hmw-2--fit-without-expertise) |
| **Priority** | **Must** |

**Acceptance criteria**

1. Given height and weight, the app derives the sizes that depend on them
   (board volume, ski length, frame size) and recalculates **live** as either
   input changes.
2. Every listing carries a fit verdict on a five-step ramp —
   `PERFECT FIT` / `GOOD FIT` / `WORKABLE` / `POOR FIT` / `NOT YOUR SIZE`.
3. The verdict is accompanied by the **reason**, in the user's units, not just
   the label.
4. A user who has not given height and weight sees `ADD YOUR HEIGHT`, not a
   guess.
5. The verdict is announced to a screen reader as a sentence, because the ramp is
   carried partly by colour.
6. The verdict never blocks the booking. It informs it. *(Directly from
   **P2-Q3** — an expert overriding it must not be treated as an error.)*

**Where the prototype answers it** — **S-02** Fit Profile (live derivation via
`data/FitLogic.kt`), **S-05**/**S-07** fit stamp on every listing, **S-28** the
renter's fit shown to the owner on the request.
**Requirements** — [FR-01](06-requirements.md), [FR-02](06-requirements.md), [FR-03](06-requirements.md), [NFR-08](06-requirements.md)

---

## US-2 — A deposit that reads as a hold

> **As a** renter deciding whether to book,
> **I want** to see immediately whether the deposit is money taken from me or
> money temporarily held,
> **so that** I can commit to the booking without fearing that LKR 15,000 is
> about to leave my account.

| | |
|---|---|
| **Persona** | [Kavitha Sivanesan](02-personas-empathy-maps.md#persona-1--kavitha-sivanesan--the-occasional-renter) · SC-1 |
| **From findings** | **F-05** |
| **Evidence** | **P1-Q6** ⟨proxy⟩ *"Fifteen thousand? For a weekend? No. I don't have fifteen thousand spare sitting there."* then, once told it was a hold: *"Oh — so it's not actually leaving? That's completely different. Why doesn't it say that?"* · **OBS-P1-T5**, **OBS-P2-T5**, **OBS-P5-T5** — 3 of 4 read the figure as a debit; **task 5 scored 3.4/7, the lowest of ten** · **SUR-Q6** — median acceptable deposit LKR 5,000, rising to LKR 20,000 when reworded as *"a hold that is released"* |
| **Reframed by** | [HMW-1](05-hmw-statements.md#hmw-1--hold-versus-charge) |
| **Priority** | **Must** — this is the study's highest-severity finding; two participants said they would abandon here |

**Acceptance criteria**

1. On the deposit screen, the word describing what happens to the money —
   *held*, *released*, *charged* — is given the same visual weight as the amount
   itself, not relegated to body copy.
2. The renter is shown, for each option, **the maximum they could actually lose**,
   as a figure. *(From **P1-Q6**: the acceptable number moved by 4× when the verb
   changed.)*
3. Protection and deposit are presented as a comparison, not a default with an
   opt-out.
4. The release condition and timeframe are stated on the same screen, before
   payment.
5. The same figure and the same verb appear identically on the deposit screen,
   the final bill, and the wallet.

**Where the prototype answers it** — **S-09** Protection & Deposit, **S-11**
Review & Pay, **S-35** Wallet & Deposits.
**Status:** *partially satisfied.* The prototype states "a hold, not a charge" in
copy; the research says it needs to be at the size of the number. This is a
recorded gap, not a claim. See [`07-report.md`](07-report.md#6-evaluation-findings-and-what-changed).
**Requirements** — [FR-04](06-requirements.md), [FR-05](06-requirements.md), [NFR-02](06-requirements.md)

---

## US-3 — Evidence instead of an argument

> **As an** owner lending equipment I paid a lot for to someone I have never met,
> **I want** a timestamped photographic record of its condition at handover and
> the same angles again at return,
> **so that** if it comes back damaged the question is settled by evidence rather
> than by whoever is more stubborn.

| | |
|---|---|
| **Persona** | [Roshan de Silva](02-personas-empathy-maps.md#persona-2--roshan-de-silva--the-reluctant-lender) · SC-2 |
| **From findings** | **F-04**, **F-06**, **F-07** |
| **Evidence** | **P4-Q4** ⟨proxy⟩ *"Honestly? I'd want photos. Mine and theirs, at the start, so nobody can invent a scratch afterwards."* · **P3-Q7** *"You'd take pictures, wouldn't you. That's what you'd do."* — volunteered unprompted by every owner-side participant · **P7-Q4** — the club coordinator already does a manual walk-around · **OBS-P4-T8** *"Right — that's what all the photos were for"*, on seeing the paired return screen |
| **Counter-evidence** | **P1-Q8** ⟨proxy⟩ *"Seven? I'm standing on a beach with a guy I met on an app… He's going to think I don't trust him."* Renter ease on this task: **4.1/7**. Owner ease on the same task: **6.5/7**. The story is written from the owner's side and the tension is reported, not hidden |
| **Reframed by** | [HMW-3](05-hmw-statements.md#hmw-3--proof-without-a-photoshoot) |
| **Priority** | **Must** |

**Acceptance criteria**

1. Condition capture is a guided sequence of named angles, not a free-form
   camera, so that the return set can be compared like for like.
2. Each frame is timestamped and geotagged at capture.
3. Capture requires both parties to be present and is recorded as such.
4. At return, the same angles are presented **side by side** with the handover
   set, with differences flagged rather than left to be found.
5. A frame that fails validation (blur, framing, darkness) is rejected at capture
   time, with the reason, not silently accepted.
6. The renter is shown *why* the sequence exists, at the moment of the ask, not
   only at return. *(Directly from **F-07** — the payoff currently arrives days
   after the cost.)*

**Where the prototype answers it** — **S-15** Condition Capture (seven guided
angles), **S-15b** Damage Marking, **S-18** Return & Settlement, **S-18b**
Evidence Diff.
**Requirements** — [FR-08](06-requirements.md), [FR-09](06-requirements.md), [FR-10](06-requirements.md), [NFR-09](06-requirements.md), [NFR-10](06-requirements.md)

---

## US-4 — Lending that costs minutes, not evenings

> **As an** owner with a full-time job,
> **I want** every booking, buffer day and pickup window for all my items on one
> calendar, with each request already carrying the renter's verified identity and
> fit,
> **so that** lending my equipment doesn't cost me more coordination than it
> earns me — and I don't double-book myself.

| | |
|---|---|
| **Persona** | [Roshan de Silva](02-personas-empathy-maps.md#persona-2--roshan-de-silva--the-reluctant-lender) · SC-2 |
| **From findings** | **F-01**, **F-02**, **F-04**, **F-11** |
| **Evidence** | **P4-Q1** ⟨proxy⟩ — declines requests he cannot schedule around work; stops silently rather than complaining · **P3-Q4** *"Stars don't mean anything to me… I want to know the app has their ID"* and *"I'm not giving my address to someone I've never met before they've paid."* · **P6-Q2** *"I can't hold a board for someone who might not turn up. Saturday I'll turn away four people and Tuesday I'll rent nothing."* · **P2-Q2** *"Finding it isn't the hard part. The hard part is 'I finish work at five and he's in Point Pedro'."* |
| **Reframed by** | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) |
| **Priority** | **Must** |

**Acceptance criteria**

1. One calendar shows every unit the owner lists, every booking, and the
   **buffer days** between them — so turnaround time is visible rather than
   assumed.
2. A booking request arrives with the renter's trust tier, verification status
   and fit verdict already attached; the owner does not have to go and look.
3. The owner's exact address is released to the renter only once payment has
   completed. *(Verbatim from **P3-Q4**.)*
4. A neutral collection point is offered as an alternative to a meeting.
   *(From **P2-Q2** and **P6-Q2** — the shack operator reframed the app as a way
   to fill weekday capacity, which makes them a plausible collection host.)*
5. Accepting or declining a request is a single action from the inbox.
6. Availability shown to renters is authoritative: a date that is bookable is a
   date that is held.

**Where the prototype answers it** — **S-24** Owner Dashboard, **S-27** Master
Calendar (buffer days), **S-28** Requests Inbox (fit + tier attached), **S-13**
Handover Hub (address unlock on payment, GearPoint Nallur 08:00–20:00, LKR 200),
**S-08** Date Picker (buffer days hatched).
**Requirements** — [FR-06](06-requirements.md), [FR-15](06-requirements.md), [FR-16](06-requirements.md), [FR-17](06-requirements.md), [FR-21](06-requirements.md)

---

## US-5 — A claim that closes with its reason recorded

> **As an** equipment custodian handling a damage claim,
> **I want** the before-and-after evidence, the policy and the decision in one
> queue,
> **so that** I can resolve the claim in minutes and both parties can afterwards
> see why it was decided that way.

| | |
|---|---|
| **Persona** | [Mala Thangarajah](02-personas-empathy-maps.md#persona-3--mala-thangarajah--the-custodian) · SC-3 |
| **From findings** | **F-10**, **F-12** |
| **Evidence** | **P7-Q10** ⟨proxy⟩ *"If it's in WhatsApp it's gone. I need to be able to open a thing, see what was agreed, and close it."* · **P6-Q10** *"The argument always happens three days later, when the board's already back on the rack."* · **P7-Q4** — already runs this process manually on a paper register of ~60 items · **OBS-P1-T7** — an unexplained line item is read as suspicion, and the same principle applies to an unexplained decision |
| **Reframed by** | [HMW-7](05-hmw-statements.md#hmw-7--the-claim-that-closes) |
| **Priority** | **Should** — SC-3 is not on the critical path to a first transaction, but the product loses its trust argument without them |

**Acceptance criteria**

1. Claims arrive in a **queue** with a state, an age and an owner — not as
   messages in a thread.
2. Opening a claim shows the handover evidence and the return evidence together,
   already paired, with the flagged differences.
3. The applicable policy and deposit or excess figure are shown on the same
   screen as the evidence.
4. A decision requires a **stated reason**, and that reason is visible to both
   parties afterwards.
5. Every state change is recorded with a timestamp and an actor, and the record
   is immutable.
6. The interface is laid out for a desk, because this is desk work.
   *(From **P7-Q10** — she wants to open a thing and close it, at a laptop.)*

**Where the prototype answers it** — **S-40** Admin Claims Console (the one
1440×1024 desktop screen), **S-34** Damage Claim, **S-18b** Evidence Diff.
**Requirements** — [FR-18](06-requirements.md), [FR-19](06-requirements.md), [FR-22](06-requirements.md), [NFR-11](06-requirements.md), [NFR-12](06-requirements.md)

---

## The sixth story, and why it isn't one of the five

The brief asks for five. This one was the strongest candidate that didn't make
the cut, and it is recorded here rather than deleted because *what you left out*
is a defensible viva answer and a silent omission isn't.

> **As an** owner of equipment I have permanently outgrown,
> **I want** to trade it for something I would actually use, and to see plainly
> whether the trade is even,
> **so that** I get value out of it without the administration of renting
> something I no longer want anything to do with.

**From** **F-01** (terminal-idle vs seasonal-idle owners), **F-08**.
**Evidence:** **P5-Q1** ⟨proxy⟩ *"I bought that board when I was fifteen kilos
lighter. I'm not going back to it. I'd rather have something I'd actually ride."*

**Why it was cut:** it is the product's **differentiator**, not its volume. The
five stories above cover the transaction that has to work before a swap is worth
building — and **F-08** found that the three-way case, which is the most
distinctive part of it, is currently *not understood from the screen alone*
(3 of 4 participants failed task 9, ease 3.9/7). Promoting a story whose flagship
interaction is a known failure would overstate the evidence.

The need is real and it is carried forward regardless — as
[HMW-5](05-hmw-statements.md#hmw-5--trading-with-someone-who-doesnt-want-your-thing)
and as requirements [FR-11](06-requirements.md), [FR-12](06-requirements.md) and
[FR-13](06-requirements.md), where FR-13 is explicitly marked as **not satisfied**
by the current design.

---

## Story → evidence coverage

| Story | Persona | Category | Findings | Priority | Prototype status |
|---|---|---|---|---|---|
| **US-1** Fit as a safety verdict | Kavitha | SC-1 | F-03 | Must | ✅ Satisfied — S-02, S-07 |
| **US-2** A deposit that reads as a hold | Kavitha | SC-1 | F-05 | Must | ⚠️ Partial — S-09 needs the verb at the weight of the number |
| **US-3** Evidence instead of an argument | Roshan | SC-2 | F-04, F-06, F-07 | Must | ✅ Satisfied — S-15, S-18; AC-6 outstanding |
| **US-4** Lending that costs minutes | Roshan | SC-2 | F-01, F-02, F-04, F-11 | Must | ✅ Satisfied — S-24, S-27, S-28, S-13 |
| **US-5** A claim that closes with its reason | Mala | SC-3 | F-10, F-12 | Should | ✅ Satisfied — S-40, S-34 |
| *(cut)* Trading what you've outgrown | — | SC-2 | F-01, F-08 | Could | ❌ S-23 fails comprehension |

Every finding except **F-02**, **F-09** and **F-12** is picked up directly by a
story; those three are carried by HMW-4, HMW-8 and HMW-6 respectively and land as
requirements without needing a story of their own — a story is a unit of user
value, and "the waiver shouldn't look broken" is a defect, not a value.
