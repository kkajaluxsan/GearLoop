# GearLoop — User Interview Guide, Version B

**A different point of view from [`interview-questions.md`](interview-questions.md).**

| | Version A | **Version B (this file)** |
|---|---|---|
| Method | Discovery interview | **Usability test / prototype walkthrough** |
| App on the table? | No, until Q9 | **Yes, from question 1** |
| Asks about | Their life, habits, past behaviour | **What they can actually do with the screens** |
| Answers the question | *Should this exist?* | ***Does this work?*** |
| Produces | Themes and quotes | **Task success rates, failure points, timings, quotes** |
| Best for | The research chapter | **The evaluation / testing chapter** |

Run Version A **first** with a few people if you can — it tells you whether the
idea is worth anything. Version B tells you whether the design communicates. Most
UI/UX modules want both, and a report that only has one of them is visibly
missing half the argument. If you only have time for one and the assignment is a
*design* assignment, run this one — it produces numbers.

---

## Before you run these

**Who.** 5–8 people. In usability testing you do not need many: five participants
find most of the problems, and the sixth mostly repeats the fifth. Prefer people
who have **not** seen the app and did **not** help you build it.

**Setup.** App running on a phone, in their hands, not yours. Sit beside them, not
opposite. Open the cover screen and start **Flow A — Rent** so everyone begins on
the same screen. Tap **Reset the demo** between participants.

**Say this first:**

> I'm testing the design, not you. Nothing you do here can be wrong — if something
> is confusing, that's the design's fault and it's exactly what I need to find.
> Please **think out loud**: say what you're looking at, what you expect, and what
> you're about to tap. I'm going to stay quiet, and if you ask me a question I'll
> probably ask what you think first — that's not me being rude, it's the method.
> May I record the screen and audio? Your name won't appear in the report.

**The two rules that make this work:**

1. **Say nothing.** The urge to help is enormous. Sit on your hands. Every second
   of their confusion is a finding, and the moment you rescue them you've deleted it.
2. **Never ask "was that easy?"** — people say yes to be kind. Ask *"what were you
   expecting there?"* instead.

**Score each task** the moment it ends, before you discuss it:

- **Outcome:** completed unaided / completed with a hint / gave up
- **Ease (ask them, 1–7):** *"How easy or difficult was that task?"* — 1 very
  difficult, 7 very easy. This one number per task is what lets you write "Task 3
  averaged 3.2 out of 7, the lowest in the study" instead of "some people found
  filtering hard".
- **Where they hesitated:** the screen number and what they said.

---

## The ten questions

### 1. Before you touch anything — look at this screen for five seconds, then tell me what this app is for and who it's for.

*Screen: S-04 Home. Tests: whether the concept reads without explanation.*

Cover the screen after five seconds. Then:

- "What do you think you can do here?"
- "Is this for renting, buying, or something else? What made you say that?"
- "What does the yellow mean?"

**Watch for:** whether "rent **and** swap" comes across, or whether it reads as a
shop. The yellow is meant to signal *action and safety only* — if someone says
"it's just the brand colour", that's a real finding about the design's central
colour rule.

---

### 2. Find something on this screen that would fit you, and tell me how you know.

*Screens: S-04 → S-02 Fit Profile. Tests: the Fit Stamp, the app's most distinctive idea.*

Then hand them S-02 and say: *"Change your height and weight to your real ones."*

- "Something changed when you did that — what?"
- "The app says this board is **WORKABLE** for you. In your own words, what is it
  telling you?"
- "Would you book it? What if it said **POOR FIT**? What about **NOT YOUR SIZE**?"

**Watch for:** whether the five-step ramp (PERFECT / GOOD / WORKABLE / POOR / NOT
YOUR SIZE) is understood as a *safety* scale or as a star rating. Also whether
they notice the derived sizes recalculating live — that's the app's proof it isn't
a static mock-up, and if nobody spots it, it needs to announce itself better.

---

### 3. You've got LKR 3,000 a day to spend, you can't collect it yourself, and it has to fit you. Find it.

*Screens: S-04 chips → S-06 Filter Sheet → S-05 Results. Tests: whether the filters are legible.*

This is the hardest task in the set and the one most likely to expose a problem.
Three constraints — price, delivery, fit — and the participant has to find all
three controls.

- "The button at the bottom of the filter sheet has a number on it. What is that
  number?"
- "How many filters are switched on right now? How can you tell?"
- "Take one off. What did you expect to happen?"

**Then push them into failure on purpose:** *"Now narrow it until nothing is
left."* They should reach S-39 No Results.

- "What are your options now?"
- "Whose fault does this feel like — yours or the app's?"

**Watch for:** whether they trust the count on the button (it is the true number
of results, which is the point), and whether the empty state's way out reads as
help or as an apology.

---

### 4. Read this line out loud and tell me what each part means: `6'6" · GRADE B · TQ-4471`

*Screen: S-07 Listing Detail. Tests: the Gear Tag, one of the three signature elements.*

Point at the hi-vis tag block and nothing else.

- "What's **6'6"**? What's **Grade B** — good or bad? Out of what?"
- "What's **TQ-4471** for? Who uses that?"
- "Does this look like a page about *one specific board*, or about a product you'd
  find several of?"

**Watch for:** the last one especially. The whole tag exists to say *this is a
real, individual, tracked object, not a catalogue SKU.* If participants read it as
a model number, the idea isn't landing and you should say so in the report rather
than hide it.

---

### 5. Book this for the weekend, and stop when you get to the money.

*Screens: S-08 Dates → S-09 Protection & Deposit. Tests: buffer days and the deposit decision.*

On the calendar: *"Some days are shaded differently — what's going on there?"*
(They're buffer days between bookings.)

On S-09, do **not** explain the options. Ask:

- "What are you being asked to choose between?"
- "What does a **deposit** mean here — is that money you're paying or money you're
  getting back?"
- "Which would you pick? Say why out loud."
- "How much would you actually lose if you damaged it, in each option?"

**Watch for:** whether "a hold, not a charge" is understood without being told. It
is the single most expensive misunderstanding on the whole path — someone who
thinks LKR 15,000 is being *taken* abandons the booking.

---

### 6. Keep going and get to the payment screen.

*Screen: S-10 Safety & Waiver. Tests: the deliberate scroll gate.*

The Accept button is disabled until the waiver has genuinely been scrolled to the
end. Say nothing at all here. Time it.

- "You paused there — what did you think had happened?"
- "Did you think the app was broken?"
- "Now that you know it needs you to scroll to the bottom: fair, or annoying?"
- "Did you read it? Be honest — I'm not marking you."

**Watch for:** the gap between "app is broken" and "app is making me read this".
This is a designed piece of friction and you need evidence it's read as
deliberate, not as a bug — that's the difference between defending it in the viva
and rewriting it.

---

### 7. Here's the final bill. Is there anything on it you'd argue with?

*Screen: S-11 Review & Pay. Tests: whether the price breakdown earns trust.*

Let them read the whole thing before you speak.

- "Go through it and tell me what each line is."
- "Is there anything here you weren't expecting?"
- "Do the numbers add up? Would you check?"
- "If you were about to pay LKR 9,240, is there anything you'd still want to know?"

**Watch for:** any line they cannot explain. A line item a user can't name is a
line item they suspect. Note it verbatim — "what's this one?" is a quotable finding.

---

### 8. You're meeting the owner to collect the board. Do the handover.

*Screens: S-13 Handover Hub → S-15 Condition Capture. Tests: the evidence chain.*

Let them work through the seven guided angles. Then tap the validation chip to
flip it into the **fail** state and ask what happened.

- "What is the app asking you to do, and why do you think it wants that?"
- "It just rejected one of your photos — what would you do now?"
- "Realistically: would you stand there and do all seven with someone you've never
  met? How would that feel?"
- "Who is this protecting — you or the owner?"

Then show **S-18 Return & Settlement**, the same seven angles side by side:

- "What is this screen for? Can you tell what changed?"

**Watch for:** whether the pairing at S-18 makes the effort at S-15 feel worth it
in hindsight. The seven-angle capture is the app's biggest ask of a user's
patience, and S-18 is the payoff — if the payoff doesn't land, the ask is too big.

---

### 9. Different job now: you own a kayak and you want to trade it. Is this a fair deal?

*Screens: S-19 → S-20 → S-21 Fairness & Boot → S-23 Cycle Preview. Tests: the Fairness Meter.*

Let them build a proposal, then leave them on S-21 with the slider.

- "The beam in the middle is tilted. What's it telling you?"
- "Make it level." *(They drag the cash slider — the one animated moment in the app.)*
- "What did you just agree to pay, and to whom?"
- "Do **you** think that's fair, or just that the app thinks so? Where do those
  values come from — would you trust them?"

Then S-23: *"Three people are trading in a circle here, none of whom could have
traded with each other directly. Read it and tell me who ends up with what."*

**Watch for:** whether the three-way trade is understood **from the screen alone**.
It is the most conceptually difficult thing in the app. If people can't follow it
without you narrating, that is a legitimate, reportable design failure — and a
much better finding than a passing grade on an easy screen.

---

### 10. Last one. Sum up: what would you change, what did you trust least, and would you actually use this?

*Screens: S-24 Owner Dashboard and S-28 Requests Inbox first, then close.*

Show the owner side briefly — *"you own the gear now; someone wants to rent it —
what do you do?"* — then close the session with three questions, in this order:

1. **"If you could change one thing, what would it be?"** (Ask before praise has a
   chance to set in.)
2. **"Was there any moment you didn't trust it, or felt unsure?"**
3. **"Honestly — would you use this? For renting, for lending, or for swapping?"**

And the one worth ending on:

> **"Was there anything you expected to be here that wasn't?"**

**Watch for:** absences. Everything else in this script tests what you built;
this tests what you didn't. Answers here go straight into the "future work"
section, which is otherwise the hardest paragraph in the report to write honestly.

---

## Recording sheet

One row per participant per task. Fill it in during the session, not after.

| Task | Outcome (unaided / hint / failed) | Ease 1–7 | Time | Where they hesitated (screen) | Quote |
|---|---|---|---|---|---|
| 1 First impression | | | | | |
| 2 Fit stamp | | | | | |
| 3 Filtering | | | | | |
| 4 Gear tag | | | | | |
| 5 Deposit | | | | | |
| 6 Waiver | | | | | |
| 7 Price breakdown | | | | | |
| 8 Condition capture | | | | | |
| 9 Fairness meter | | | | | |
| 10 Owner + close | | | | | |

---

## Writing it up

Version B produces numbers, so **use them** — this is what makes an evaluation
chapter look like evaluation rather than opinion.

1. **A task-success table.** Ten tasks down the side, participants across the top,
   ✓ / hint / ✗ in the cells. One glance shows the examiner where the design
   breaks. Put it early.
2. **Rank the tasks by average ease score.** Your worst two tasks are your
   findings. Say what you'd change about each.
3. **Severity-rate each problem**, don't just list them:
   - **Critical** — stopped them completing the task (e.g. abandoned at the deposit)
   - **Major** — completed, but slowly or with the wrong mental model
   - **Minor** — noticed, mildly irritated, carried on
4. **Count, don't guess.** "4 of 6 participants thought the deposit was a charge
   rather than a hold" is evidence. "Users found deposits confusing" is a feeling.
5. **Report the failures loudest.** A test where everything went well is a test
   that was run badly, and markers know it. The three-way trade screen and the
   waiver gate are your most likely genuine failures — if they fail, that's the
   most valuable page in your report, not the most embarrassing one.
6. **Close each finding with what you'd do.** Problem → severity → evidence
   (how many people, one quote) → the specific change → whether you made it.

**Optional extra, cheap and impressive:** ask one participant to repeat task 3 with
the phone's font size set to maximum, and one with the app in dark mode. Both are
built and both are testable; noticing that you checked accessibility and theming
under real use costs you ten minutes and reads as thoroughness.

**Demo notes:** the prototype has no backend or saved data, so nothing a
participant does can break the next session — but do tap **Reset the demo** on the
cover between people, because that's what puts the fairness slider and the fit
profile back where they were designed. If you want a participant to jump straight
to one screen, the cover screen has an index of all 42.
