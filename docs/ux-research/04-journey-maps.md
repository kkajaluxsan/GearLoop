# 04 — User Journey Maps

> Assignment task **i** — *"Map user journeys for at least one key task/scenario
> per persona, highlighting pain points and opportunities."*
>
> Constraint **h** — *"At least one user journey map must be included, covering a
> key task or scenario and highlighting pain points."*

Three maps, one per persona, one key scenario each. The brief asks for one; three
is what it takes to show that the two sides of a two-sided market have different
journeys through the same transaction — which is the argument the whole product
rests on.

**How to read these.** Emotion is scored **−2 to +2** and plotted; every pain
point carries the evidence ID it came from; every opportunity carries the HMW
statement that reframes it and the requirement it becomes. A stage with no pain
point recorded is a stage where the research found none — not a stage nobody
looked at.

The dashed line at **0** is neutral. Where the curve crosses it downward, that is
where the journey is at risk of ending.

---

## JM-1 — Kavitha rents a surfboard for a weekend

**Persona:** [Kavitha Sivanesan](02-personas-empathy-maps.md#persona-1--kavitha-sivanesan--the-occasional-renter) · SC-1 Renter
**Scenario:** It is Wednesday. She and two friends are going to Casuarina on
Saturday. She needs a board for two days, she does not own one, and she does not
know what size she should be on.
**Success:** she is holding a board that fits her on Saturday morning, having
spent what she expected to spend.

### Emotion curve

```
 +2 │                                                        ╭──╮
    │                                             ╭──────────╯  ╰──╮
 +1 │        ╭──╮                    ╭────────────╯                ╰──╮
    │   ╭────╯  ╰──╮        ╭────────╯                                ╰───
  0 ├───╯          ╰────╮   │                                              → time
    │                   ╰───╯
 −1 │                    ▼
    │                 DEPOSIT
 −2 │
    └──┬────┬─────┬─────┬─────┬─────┬─────┬──────┬──────┬──────┬──────┬───
       1    2     3     4     5     6     7      8      9      10     11
     Trigger Search Filter Decide Dates Deposit Waiver Pay  Collect Use  Return
```

### The journey

| # | Stage | What she does | What she thinks | Emotion | Screens |
|---|---|---|---|---|---|
| 1 | **Trigger** | Friends confirm Saturday. Opens the app instead of the WhatsApp group | *Last time I posted in the group I got replies on Monday* (**P1-Q2**) | **0** | — |
| 2 | **Onboard / fit** | Enters height and weight | *I don't actually know what size I need* (**P1-Q3**) | **+1** | S-01, S-02, S-03 |
| 3 | **Search** | Browses the home rails, which are ranked by fit | *These are sorted for me, not for whoever paid* | **+1** | S-04 |
| 4 | **Filter** | Needs ≤ LKR 3,000/day, delivery, and a fit — three constraints, three controls | *Where's the delivery one?* — 2 of 4 needed a hint here | **0** | S-06, S-05 |
| 5 | **Decide** | Reads the listing: tag `6'6" · GRADE B · TQ-4471`, fit verdict, condition photos | *Grade B — is that good? Out of what?* | **+1** | S-07, S-42 |
| 6 | **Dates** | Picks Sat–Sun. Buffer days are hatched | *Why can't I have Friday?* — visible, so answerable | **+1** | S-08 |
| 7 | **Deposit** | Sees **LKR 15,000** | ***"Fifteen thousand? For a weekend? No."*** (**P1-Q6**) | **−1** ⚠️ | **S-09** |
| 8 | **Waiver** | Accept button is greyed. Pauses. Median **22 s** | *Is it stuck?* (**OBS-P2-T6**) | **0** ⚠️ | **S-10** |
| 9 | **Pay** | Reads all eight lines. Cannot name "Service fee" | *What's this one? Is that yours?* (**OBS-P1-T7**) | **+1** | S-11, S-12 |
| 10 | **Collect** | Address unlocks. Chooses GearPoint Nallur over a meeting | *I don't have to arrange a rendezvous with a stranger* | **+2** | S-13, S-14 |
| 11 | **Capture** | Seven guided angles with the owner watching | *He's going to think I don't trust him* (**P1-Q8**) | **0** ⚠️ | S-15, S-16 |
| 12 | **Use → return** | Surfs. Returns. Same seven angles. Deposit released | *That's what the photos were for* | **+2** | S-17, S-18, S-35 |

### Pain points

| # | Stage | Pain | Evidence | Severity |
|---|---|---|---|---|
| **PP-1.1** | 7 Deposit | Reads LKR 15,000 as money debited, not held. **2 of 4 said they would abandon here** | **P1-Q6**, **OBS-P1-T5**, **OBS-P2-T5**, **OBS-P5-T5** · task ease **3.4/7**, lowest of ten | **Critical** |
| **PP-1.2** | 8 Waiver | Disabled button with no stated cause reads as a bug. Median 22 s pause, 2 of 4 asked if it was frozen | **OBS-P2-T6**, **OBS-P5-T6** · ease **4.8/7** | **Major** |
| **PP-1.3** | 4 Filter | Three constraints, three separate controls; 2 of 4 needed a hint to find delivery | **OBS-P1-T3**, **OBS-P4-T3** · ease **4.5/7** | **Major** |
| **PP-1.4** | 9 Pay | One line item she cannot name puts the whole bill under suspicion | **OBS-P1-T7**, **OBS-P5-T7**, **OBS-P2-T7** — 3 of 4 | **Major** |
| **PP-1.5** | 11 Capture | Seven photographs in front of the owner feels like an accusation | **P1-Q8** · renter ease **4.1/7** vs owner **6.5/7** | **Minor** |
| **PP-1.6** | 1 Trigger | The alternative she is leaving behind — a group chat with no state — sets her expectation that nothing is certain until she holds it | **P1-Q2**, **P2-Q2** | **Minor** |

### Opportunities

| From | Opportunity | HMW | Requirement |
|---|---|---|---|
| PP-1.1 | Give the **verb** the same weight as the amount, and state the worst case as a figure for each option | [HMW-1](05-hmw-statements.md#hmw-1--hold-versus-charge) | FR-04, FR-05 |
| PP-1.2 | Make the gate state its own condition and its own progress, so the constraint reads as designed | [HMW-8](05-hmw-statements.md#hmw-8--friction-that-explains-itself) | FR-14 |
| PP-1.3 | Show the live result count on the apply control so the constraints can be tuned without leaving the sheet | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-06 |
| PP-1.4 | Every line self-describing on tap; no line without a plain-language "what is this" | [HMW-6](05-hmw-statements.md#hmw-6--the-bill-that-explains-itself) | FR-20 |
| PP-1.5 | Show the *return comparison* at the moment of the ask, not days later | [HMW-3](05-hmw-statements.md#hmw-3--proof-without-a-photoshoot) | FR-10 |
| PP-1.6 | Availability that is a commitment, not a promise — bookable means held | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-16, FR-17 |

**The moment of truth is stage 7.** Everything before it is going well and
everything after it is going well. The single lowest point on the curve is also
the single highest-severity finding in the study, and it is a wording problem
with a four-figure consequence.

---

## JM-2 — Roshan lends a board to a stranger for the first time

**Persona:** [Roshan de Silva](02-personas-empathy-maps.md#persona-2--roshan-de-silva--the-reluctant-lender) · SC-2 Owner
**Scenario:** The SUP has been behind the wardrobe since September. He has moved
it four times. He decides to try listing one of his boards and see what happens.
**Success:** the board goes out, comes back in the condition it left in, and the
whole thing cost him under thirty minutes of attention.

### Emotion curve

```
 +2 │                                                         ╭────────
    │                                              ╭──────────╯
 +1 │   ╭──╮                        ╭──────────────╯
    │───╯  ╰──╮      ╭──────────────╯
  0 ├─────────╰──╮   │                                              → time
    │            ╰───╯
 −1 │             ▼
    │        "A STRANGER
 −2 │         HAS MY BOARD"
    └──┬─────┬─────┬─────┬──────┬──────┬──────┬──────┬──────┬──────┬───
       1     2     3     4      5      6      7      8      9      10
     Guilt  List  Price Request Vet  Handover Away  Return Settle  Paid
```

### The journey

| # | Stage | What he does | What he thinks | Emotion | Screens |
|---|---|---|---|---|---|
| 1 | **Guilt** | Moves the SUP out of the way for the fourth time | *This cost me a lot and it does nothing* (**P3-Q1**) | **+1** | — |
| 2 | **Consider** | Weighs lending against selling. Doesn't sell — he might want it in July | *Selling is final* (**P3-Q1**) | **0** | — |
| 3 | **List** | Photographs the board, enters the tag: size, grade, code | *How honest do I have to be about the dings?* | **0** | S-25, S-36 |
| 4 | **Price** | Sets a day rate and a deposit. Sees what comparable items earn | *Is anyone going to pay that?* | **−0.5** | S-26 |
| 5 | **Request** | A request arrives, with the renter's fit verdict and trust tier attached | *At least I can see who this is* (**P3-Q4**) | **0** | S-28, S-38 |
| 6 | **Vet** | Checks verification, tier, review count | ***"Stars don't mean anything to me… I want to know the app has their ID"*** (**P3-Q4**) | **−1** ⚠️ | S-28, S-32, S-37 |
| 7 | **Accept** | Accepts. Address is released to the renter *only now* | *I'm not giving my address before they've paid* (**P3-Q4**) | **+1** | S-13, S-27 |
| 8 | **Handover** | Meets at GearPoint, seven angles captured with both present, both sign | *This is the reason I'd use it* (**OBS-P4-T8**) | **+1.5** | S-13, S-14, S-15, S-16 |
| 9 | **Away** | The board is gone for three days | *Someone I don't know has my board* | **+1** | S-17, S-33 |
| 10 | **Return & settle** | Same seven angles, side by side, differences flagged. No dispute | *Right — that's what all the photos were for* | **+2** | S-18, S-18b |
| 11 | **Paid** | Earnings and occupancy on the dashboard | *Did that earn more than the hassle?* | **+2** | S-24, S-35 |

### Pain points

| # | Stage | Pain | Evidence | Severity |
|---|---|---|---|---|
| **PP-2.1** | 6 Vet | A review score is trivially gamed and he discounts it entirely; without held identity the whole trust argument collapses | **P3-Q4**, **P7-Q4** | **Critical** |
| **PP-2.2** | 6–7 | Will not expose his home address to an unpaid request | **P3-Q4** | **Critical** |
| **PP-2.3** | 10 Settle | Without a paired record, "it was like that when I got it" is unanswerable | **P4-Q4**, **P6-Q10** | **Critical** |
| **PP-2.4** | 5, 7 | Coordination around a working week; he declines what he can't schedule, silently | **P4-Q1**, **P2-Q2** | **Major** |
| **PP-2.5** | 4 Price | No basis for setting a rate or a deposit; guesses | **P3-Q1**, **SUR-Q6** | **Major** |
| **PP-2.6** | 3 List | Turnaround between bookings is invisible, so he risks double-booking himself | **P6-Q2** | **Major** |
| **PP-2.7** | 1–2 | Terminal-idle equipment (P5's outgrown board) is *bad rental supply* — renting out something you're finished with is admin for money you didn't need | **P5-Q1**, **F-01** | **Major** |

### Opportunities

| From | Opportunity | HMW | Requirement |
|---|---|---|---|
| PP-2.1 | Trust as a **stack** — verified identity + money at stake + condition evidence, shown together. Never a score alone | [HMW-3](05-hmw-statements.md#hmw-3--proof-without-a-photoshoot) | FR-07, FR-09, NFR-09 |
| PP-2.2 | Address release gated on completed payment, stated to both parties | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-15, NFR-10 |
| PP-2.3 | Guided angles at handover, the same angles paired at return, differences flagged rather than hunted | [HMW-3](05-hmw-statements.md#hmw-3--proof-without-a-photoshoot) | FR-08, FR-09 |
| PP-2.4 | Requests arrive pre-vetted; a neutral collection point removes the rendezvous entirely | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-16, FR-17, FR-21 |
| PP-2.5 | Pricing guidance from comparable items; deposit defaults derived from item value | [HMW-1](05-hmw-statements.md#hmw-1--hold-versus-charge) | FR-05, FR-22 |
| PP-2.6 | Buffer days as first-class calendar objects, visible to both sides | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-06, FR-17 |
| PP-2.7 | A **swap** path for terminal-idle equipment, so the owner isn't forced into a rental relationship he doesn't want | [HMW-5](05-hmw-statements.md#hmw-5--trading-with-someone-who-doesnt-want-your-thing) | FR-11, FR-12 |

**The moment of truth is stage 6.** The dip is not fear of damage — it is the
absence of anything he considers real evidence about who is asking. The recovery
at stage 8 and the peak at stage 10 are both produced by the same feature, which
is why **F-04** insists trust is a stack of three and not a rating.

---

## JM-3 — Mala resolves a damage claim

**Persona:** [Mala Thangarajah](02-personas-empathy-maps.md#persona-3--mala-thangarajah--the-custodian) · SC-3 Operator
**Scenario:** A renter returned a board on Sunday. On Wednesday the owner opens a
claim: a crack near the fin box that he says wasn't there. The renter says it
was. Mala has to decide.
**Success:** a decision inside ten minutes, with a stated reason both parties can
read, and a record that still exists in six months.

### Emotion curve

```
 +2 │                                                  ╭──────────
    │                                        ╭─────────╯
 +1 │                              ╭─────────╯
    │                    ╭─────────╯
  0 ├───╮                │                                    → time
    │   ╰────╮      ╭────╯
 −1 │        ╰──────╯
    │         ▼
 −2 │    "IT'S IN WHATSAPP,
    │      SO IT'S GONE"
    └──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───
       1      2      3      4      5      6      7      8
     Notified Gather Compare Policy Decide Record Communicate Close
```

### The journey

| # | Stage | What she does | What she thinks | Emotion | Screens |
|---|---|---|---|---|---|
| 1 | **Notified** | A claim appears. It is three days after the return | *The argument always happens three days later* (**P6-Q10**) | **0** | S-40, S-38 |
| 2 | **Gather** | *Today:* hunts through a chat thread and a paper register for what the item looked like on Friday | ***"If it's in WhatsApp it's gone"*** (**P7-Q10**) | **−1** ⚠️ | — |
| 3 | **Compare** | Opens the claim; handover and return evidence are already paired, difference flagged at the fin box | *I can see it. I don't have to take anyone's word* | **+0.5** | S-40, S-18b, S-34 |
| 4 | **Policy** | Reads the applicable protection tier and excess on the same screen | *What did they actually agree to?* | **+1** | S-40, S-34 |
| 5 | **Decide** | Apportions the cost. Types the reason — it is required, not optional | *I have to be able to show my working* (**P7-Q10**) | **+1.5** | S-40 |
| 6 | **Record** | State change is stamped with time and actor; immutable | *In six months this still exists* | **+2** | S-40 |
| 7 | **Communicate** | Both parties see the decision **and the reason** | *Nobody has to ring me about it* | **+2** | S-34, S-33, S-38 |
| 8 | **Close** | Claim leaves the queue with a state, not a silence | *Open a thing, close a thing* (**P7-Q10**) | **+2** | S-40 |

### Pain points

| # | Stage | Pain | Evidence | Severity |
|---|---|---|---|---|
| **PP-3.1** | 2 Gather | Conversation-shaped tools have no memory; the evidence window has already closed by the time the dispute arrives | **P7-Q10**, **P6-Q10** | **Critical** |
| **PP-3.2** | 1 Notified | Disputes surface days late, when the item is already back in circulation and cannot be re-examined | **P6-Q10** | **Critical** |
| **PP-3.3** | 5–6 Decide | A decision with no recorded reason is indistinguishable from an arbitrary one — the same mechanism as the unexplained line item in **F-12** | **P7-Q10**, **OBS-P1-T7** | **Major** |
| **PP-3.4** | all | A manual register does not scale past ~60 items and cannot be searched or audited | **P7-Q4** | **Major** |
| **PP-3.5** | — | Idle weekday capacity she cannot fill; Saturday she turns four people away | **P6-Q2** | **Major** |
| **PP-3.6** | 3–5 | This is desk work being done on a phone-shaped tool | **P7-Q10** | **Minor** |

### Opportunities

| From | Opportunity | HMW | Requirement |
|---|---|---|---|
| PP-3.1 | A **queue** with state, age and assignee — not a thread | [HMW-7](05-hmw-statements.md#hmw-7--the-claim-that-closes) | FR-18, NFR-11 |
| PP-3.2 | Evidence captured at the transaction, not requested at the dispute; return capture within a fixed window | [HMW-3](05-hmw-statements.md#hmw-3--proof-without-a-photoshoot) | FR-08, FR-09 |
| PP-3.3 | A reason is mandatory on every decision, and visible to both parties | [HMW-6](05-hmw-statements.md#hmw-6--the-bill-that-explains-itself) | FR-19, FR-22 |
| PP-3.4 | An immutable, timestamped, attributable audit trail | [HMW-7](05-hmw-statements.md#hmw-7--the-claim-that-closes) | FR-19, NFR-12 |
| PP-3.5 | Operators as **collection points**, filling weekday capacity from demand they don't have to find | [HMW-4](05-hmw-statements.md#hmw-4--the-handover-problem) | FR-21 |
| PP-3.6 | A desktop layout for the console, because that is where the work happens | [HMW-7](05-hmw-statements.md#hmw-7--the-claim-that-closes) | NFR-11 |

**The moment of truth is stage 2**, and it is the only stage on this map that the
product removes rather than improves. Everything from stage 3 onward is
comfortable *because* stage 2 no longer exists — the evidence was captured at the
transaction rather than requested at the dispute. That is the argument for
**S-15** stated from the third stakeholder's side, and it is why **F-06**'s
renter/owner tension is worth paying: the cost lands on Kavitha and the benefit
lands on Roshan *and* Mala.

---

## Cross-journey observations

**1. The three journeys share exactly one screen pair, and it is the product's
centre of gravity.** S-15 and S-18 appear in all three maps. It is the cost in
JM-1, the payoff in JM-2, and the precondition in JM-3. Nothing else in the app
does work for all three stakeholders.

**2. Each journey has a distinct failure mode, and only one is a design defect.**
Kavitha's is *language* (a deposit misread as a charge). Roshan's is *evidence*
(no verified identity, so no first lend). Mala's is *memory* (no record, so no
decision). Only Kavitha's is currently unresolved in the prototype, and it is the
highest-severity finding in the study.

**3. The dips do not line up.** Kavitha's low point (stage 7, deposit) is
Roshan's high point (money at stake is exactly what makes him say yes). This is
not a contradiction to be designed away — it is the market clearing, and the
design's job is to make the same fact legible from both sides rather than to
soften it for one.
