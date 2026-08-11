# 07 — Report

> Assignment task **l** — *"Prepare the report and viva will be taken in the lab
> session."*

The assembled submission. Sections 1–10 are the report; §11 is viva preparation
and should not be submitted — it exists so you are not surprised in the room.

> **⚠ Before submitting.** Every quote in this chain is still marked `⟨proxy⟩`.
> Run the sessions, fill the appendix, replace the quotes keeping the IDs, then
> search the folder for `⟨proxy⟩` and confirm zero results. Submitting with the
> markers in place would be worse than submitting late.

---

## 1. Executive summary

GearLoop is a peer-to-peer platform for renting and exchanging sports equipment.
This study asked whether the three problems the design brief assumed — that
people can't judge fit, that owners won't lend to strangers, and that damage
disputes have no neutral record — are real, and whether they are the right three.

**Seven participants across three stakeholder categories**, in two strands
(discovery interviews, n=7; task-based usability walkthroughs, n=4) plus a
screening survey (n=18). Twelve findings, three personas, three journey maps,
eight How Might We statements, and 41 requirements — every one traceable to a
participant.

**The four things worth knowing:**

1. **The three assumed problems are real, but their weighting is wrong.** Nobody
   complained about *finding* equipment. Four of seven complained, unprompted,
   about *arranging to collect it* (**F-11**). The brief spent four screens on
   discovery and two on handover.

2. **The highest-severity problem in the product is a single word.** The deposit
   screen scored **3.4 out of 7** for ease — the lowest of ten tasks — and two
   participants said they would abandon the booking there. The amount was not the
   issue: the median acceptable deposit moved from **LKR 5,000 to LKR 20,000**
   when the same question was reworded from *"deposit"* to *"a hold that is
   released"* (**F-05**, **SUR-Q6**).

3. **Trust is a stack of three, not a rating.** No owner named a single condition
   for lending to a stranger. They named identity held by the platform, money at
   stake, *and* a condition record — and every owner-side participant invented
   the photograph mechanism unprompted before being shown it (**F-04**, **F-06**).

4. **The product's most distinctive screen is its clearest failure.** Three of
   four participants could not say who ends up with what after reading the
   three-way swap screen (**F-08**). The two-party version worked for everyone.
   The mechanism is sound and the explanation is not.

---

## 2. The problem and the research questions

Sports equipment is bought for a season and then sits idle. A surfboard used
eight times a year occupies a corridor for the other 357 days, while the person
who wants to try surfing has to buy one they may not keep using or find a shop
with their size on the day they want it.

Lending to each other is the obvious answer and it fails on three things that
have nothing to do with matching supply to demand: nobody can tell whether the
equipment fits, nobody will lend an expensive item to a stranger without proof,
and when something comes back damaged the argument is settled by whoever is more
stubborn.

Seven research questions followed from that, listed in
[`01-research-data.md §1`](01-research-data.md#1-the-problem-being-researched).

---

## 3. Method

| Strand | Instrument | n | Output |
|---|---|---|---|
| **A** Discovery interview | Semi-structured, 10 questions | 7 | Themes, verbatim quotes |
| **B** Usability walkthrough | 10 tasks on the prototype | 4 | Success rates, ease 1–7, hesitations |
| **C** Screening survey | 8 closed questions | 18 | Prevalence, recruitment |

**Three stakeholder categories**, all represented — the brief requires two:

| | Category | Participants | Persona |
|---|---|---|---|
| **SC-1** | Renter / demand side | P1, P2 | Kavitha Sivanesan |
| **SC-2** | Owner / supply side | P3, P4, P5 | Roshan de Silva |
| **SC-3** | Operator / custodian | P6, P7 | Mala Thangarajah |

A theme was promoted to a finding only where it appeared in **at least two
participants from two different sessions**, or where one account was corroborated
by a survey figure. Single unsupported opinions are in the appendix and
explicitly not used to justify a requirement.

Full method, sampling rationale, ethics and limitations:
[`01-research-data.md §2`](01-research-data.md#2-method).

---

## 4. Personas

Three composites, one per category, each with an empathy map in which every cell
carries an evidence ID. Full versions:
[`02-personas-empathy-maps.md`](02-personas-empathy-maps.md).

| Persona | Category | In one line | Her/his single decisive quote |
|---|---|---|---|
| **Kavitha Sivanesan**, 24 | SC-1 | Not a surfer — someone who surfs. Doesn't know what size she needs and won't complete a booking she doesn't understand | *"Oh — so it's not actually leaving? That's completely different. Why doesn't it say that?"* |
| **Roshan de Silva**, 34 | SC-2 | Has ~LKR 180,000 of equipment behind a wardrobe and knows the figure. Would lend it; won't lend it on a star rating | *"Stars don't mean anything to me. Anybody can get five stars renting a tent twice."* |
| **Mala Thangarajah**, 41 | SC-3 | Already runs the whole trust process manually, on paper. Her problem isn't the process, it's that the process has no memory | *"If it's in WhatsApp it's gone. I need to be able to open a thing, see what was agreed, and close it."* |

---

## 5. Where the research contradicted the brief

The most useful pages in this report. Four places where the study said something
the design brief did not.

**5.1 — Handover matters more than discovery.** The brief weights discovery
heavily (S-04, S-05, S-06, S-29). Not one participant raised difficulty *finding*
equipment. Four of seven raised difficulty collecting it, unprompted (**F-11**).
The address-unlock-on-payment rule on S-13 exists because of **P3-Q4**, not
because of the brief.

**5.2 — The rental shop is a stakeholder, not a competitor.** P6 runs a shack
with eleven boards and immediately reframed the app as a way to fill the days
nobody walks in (**P6-Q2**). That produced the GearPoint collection point, and it
answers the renter's handover problem and the operator's idle-capacity problem
with one feature.

**5.3 — Fit is a safety feature for beginners and a nuisance for experts.**
**P2-Q3** — a cyclist who reads frame size off the seat tube — is why **FR-03**
forbids the fit verdict from blocking a booking. Without that one participant the
requirement would have been written as a hard gate and would have been wrong.

**5.4 — The deposit's problem is grammatical.** The brief treats the deposit
amount as a pricing decision. The research says it is a *wording* decision worth
a factor of four (**SUR-Q6**).

---

## 6. Evaluation findings and what changed

Ten tasks, four participants, ease rated 1–7 immediately after each task before
discussion. Full table:
[`01-research-data.md §6`](01-research-data.md#6-summary--usability-task-scores).

### The two failures

#### Finding 1 — The deposit is read as a charge · **Critical**

| | |
|---|---|
| **Evidence** | 3 of 4 read LKR 15,000 as debited. Task ease **3.4/7**, lowest of ten. **2 of 4 said they would abandon the booking.** Median acceptable deposit moved LKR 5,000 → 20,000 on rewording alone (**SUR-Q6**) |
| **Quote** | *"Fifteen thousand? For a weekend? No. I don't have fifteen thousand spare sitting there."* → *"Oh — so it's not actually leaving?"* (**P1-Q6**) |
| **Diagnosis** | The verb is doing its work in body copy while the amount is doing its work at 32 pt. The user reads the number and infers the verb |
| **Change proposed** | Give the verb weight equal to the amount; state the worst-case loss as a figure per option (**FR-04**, **FR-05**) |
| **Made?** | **No.** Recorded as the highest-priority outstanding change |

#### Finding 2 — The three-way swap cannot be read · **Major**

| | |
|---|---|
| **Evidence** | 3 of 4 could not state who ends up with what. Ease **3.9/7**, second-lowest. All 4 understood the two-party meter unaided; 3 of 4 said "fair" unprompted after moving the slider |
| **Quote** | *"So I give her the board and she gives me… no, wait. Who's giving me the kayak?"* (**OBS-P5-T9**) |
| **Diagnosis** | A cycle shown simultaneously asks the reader to hold three transfers at once. The concept is not the problem; the simultaneity is |
| **Change proposed** | Sequential representation, plus a one-sentence outcome — *"you end up with the kayak and pay LKR 4,000"* — before the mechanism (**FR-13**) |
| **Made?** | **No.** Recorded as future work |

### The three near-misses

| Finding | Severity | Evidence | Change | Made? |
|---|---|---|---|---|
| Waiver gate reads as a bug (**F-09**) | Major | Median **22 s** pause; 2/4 asked if it was frozen; 4/4 called it fair once explained | Disabled control states its own condition and progress (**FR-14**) | No |
| "Service fee" unexplainable (**F-12**) | Major | 3/4 could explain every line except one | Every line self-describing on tap (**FR-20**) | No |
| Three constraints, three controls (**F-02**) | Major | 2/4 needed a hint to find delivery; ease **4.5/7** | Live result count on the apply control (**FR-06**) | **Yes** — already built |

### What tested well, and why that matters less

Fit stamp **6.3/7**, first impression **6.0/7**, condition capture **5.3/7** on
the owner side. These are reported briefly and deliberately: *a test where
everything goes well is a test that was run badly*, and the two failures above
are worth more to this report than the eight passes.

---

## 7. Open tensions

One finding did not resolve, and it is the most honest thing in the study.

**The seven-angle condition capture is the owner's confidence and the renter's
tax.** Owner-side ease **6.5/7** — *"the reason I'd use it."* Renter-side ease
**4.1/7** — *"He's going to think I don't trust him."* Both were describing the
same screen (**F-06**).

This is not a design defect to be fixed. It is the cost of the product's core
promise landing on one party and the benefit landing on the other, and any
"solution" that satisfies both sides completely would be removing the mechanism.
Three partial answers are carried into the requirements — show the payoff at the
moment of the ask (**FR-10**), frame the capture as mutual rather than
one-directional, and scale the sequence with the value at risk — but the tension
is reported rather than resolved, because the research does not support claiming
it was.

---

## 8. Requirements

**25 functional, 16 non-functional.** Every one traceable to a finding, an HMW
statement and a participant. Full tables and the two-way traceability matrix:
[`06-requirements.md`](06-requirements.md).

| Status | Count | The ones that matter |
|---|---|---|
| ✅ Satisfied | 25 | The prototype's built answer |
| ⚠️ Partial | 12 | **FR-04**, **FR-05** — the deposit's verb. **NFR-01–03**. FR-19's reason field is not yet mandatory |
| ❌ Not satisfied | 2 | **FR-10** payoff at the ask · **FR-13** cycle comprehension |
| ⬜ Out of scope | 2 | NFR-09, NFR-12 — backend, named rather than dropped |

Four requirements — NFR-04, NFR-05, NFR-06, NFR-16 — are derived from WCAG 2.2 AA
and maintainability rather than from participants, and are declared as such. No
other requirement lacks a participant behind it.

---

## 9. Limitations

Stated plainly, because a limitations section that finds nothing wrong is not one.

1. **Geographic scope.** All participants are in the Northern Province. The
   prototype's dataset is set in Jaffna, so this is deliberate, but nothing here
   should be read as national.
2. **Strand B was not naive.** The four walkthrough participants had already sat
   a discovery interview, which inflates task success. Noted against every score.
   The two failures survived that inflation, which strengthens rather than
   weakens them.
3. **n = 4 for the usability strand.** Five participants find roughly 80 % of
   usability problems; four finds fewer. Percentages are reported as fractions —
   "3 of 4", never "75 %" — because a percentage implies a precision four people
   cannot carry.
4. **No participant over 50, and none disclosed an impairment.** The
   accessibility requirements are therefore standards-derived, not observed, and
   §7.2 of the requirements says so.
5. **Self-reported willingness is not behaviour.** Every participant who said
   they would lend said it without having done it. The supply side of this market
   remains the least-evidenced claim in the report.

---

## 10. Future work

In priority order, which is the order the evidence puts them in.

1. **FR-04 / FR-05 — the deposit's verb.** Highest severity, cheapest fix.
   Typography, not architecture.
2. **FR-13 — sequential three-way swap.** Re-test against the 3.9/7 baseline.
3. **FR-14 — the gate states its own condition.** Target: median pause under 5 s.
4. **FR-10 — show the payoff at the moment of the ask.** Directly from F-07.
5. **FR-20 — every bill line self-describing.**
6. **Re-run Strand B with naive participants** to remove the inflation in §9.2.
7. **Longitudinal evidence for the supply side** — §9.5 is the report's weakest
   claim, and only observed lending behaviour fixes it.

---

## 11. Viva preparation

*Not for submission.* The questions you should expect, and the honest answers.

**"Where did this requirement come from?"** — Open
[`06-requirements.md §8`](06-requirements.md#8-traceability-matrix) and read the
row. Every requirement has a finding, an HMW and a participant. This is the
question the whole traceability matrix exists to answer in one gesture.

**"How do you know users need this?"** — Name the evidence type, not the
conclusion. *"Three of four participants, in observation, task 5 — and the survey
figure moved by a factor of four on rewording."* Never *"users found it
confusing."*

**"Only seven participants?"** — Yes, exceeding the required five, purposively
sampled across three categories rather than randomly. Five participants find
about 80 % of usability problems; the value was in the *disagreement* between
categories, not in the count. Then point at **F-06**: the renter and the owner
rated the same screen 4.1 and 6.5, and that disagreement is only visible because
the sample was split deliberately.

**"What did you get wrong?"** — Have this one ready; it is the question that
separates grades. The brief weighted discovery over handover and the research
contradicted it (**§5.1**). The deposit was treated as a pricing decision and it
is a grammatical one (**§5.4**).

**"Which finding surprised you?"** — That owners invent the photograph mechanism
unprompted (**F-06**) — every owner-side participant, before being shown
anything. And that renters then find it accusatory. Same feature, opposite signs.

**"Why is the three-way swap screen still broken?"** — Because it is more useful
in the report than a fixed screen would be. The mechanism validated (4/4 on the
two-party case) and the explanation didn't (1/4). Naming the failure with a
number, a proposed fix and a re-test threshold is the finding; quietly patching
it before the viva would have deleted it.

**"Show me a requirement the prototype does not meet."** — **FR-04**. Then walk
to S-09 on the device and read it aloud. Volunteering a red row before being
asked is worth more than defending a green one.

**"What would you do next with more time?"** — §10, in order, with the reason the
order is what it is: severity first, then cost.

**Have the device ready.** Jump to any screen without hunting:

```
adb shell am start -n com.gearloop.app/.MainActivity --es screen s09
adb shell am start -n com.gearloop.app/.MainActivity --es screen s23
```

The three screens you will most likely be asked to open are **S-09** (the
critical finding), **S-23** (the failure) and **S-15** (the concept).

---

## 12. Appendices

| | Contents | File |
|---|---|---|
| **A1** | Consent script and signed record | [`appendix/A1-consent.md`](appendix/A1-consent.md) |
| **A2** | Participant log — dates, categories, consent, recording filenames | [`appendix/A2-participant-log.md`](appendix/A2-participant-log.md) |
| **A3** | Per-session notes, one section per participant | [`appendix/A3-session-notes.md`](appendix/A3-session-notes.md) |
| **A4** | Usability scoring sheets and the task-success matrix | [`appendix/A4-usability-scores.md`](appendix/A4-usability-scores.md) |
| **A5** | Survey instrument and response table | [`appendix/A5-survey.md`](appendix/A5-survey.md) |
| **A6** | Audio and screen recordings | `appendix/raw/` |
| **A7** | Interview guides as administered | [`../interview-questions.md`](../interview-questions.md) · [`../interview-questions-v2.md`](../interview-questions-v2.md) · [`../interview-questions-simple.md`](../interview-questions-simple.md) |

Constraint **d** requires the raw data to be *retained and referenced*. A2
carries the filename of every recording; A3 and A4 carry the notes. **Both must
be filled from real sessions before this report is submitted.**
