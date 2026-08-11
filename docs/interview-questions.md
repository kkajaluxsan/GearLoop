# GearLoop — User Interview Guide

Ten questions for the primary-research interviews, written against what GearLoop
actually claims. Each question exists to test one design decision that is already
built into the prototype, so the transcripts can be turned into evidence rather
than into quotes.

**What GearLoop is:** a peer-to-peer app for renting and swapping sports gear —
surfboards, bikes, SUPs, camping kit, cricket gear, climbing gear — between
people in the same area (the demo dataset is set in Jaffna, prices in LKR).
Three things make it different from a normal marketplace, and those three are
what the interviews have to justify:

1. **Fit is treated as a safety property**, not a preference — the app derives
   your board volume, ski length and frame size from your height and weight and
   stamps every listing PERFECT / GOOD / WORKABLE / POOR / NOT YOUR SIZE.
2. **Condition is captured as evidence** — seven guided photo angles at pickup
   with both people present, and the same seven angles side by side at return.
3. **Gear can be swapped, not only rented** — including three-way trades between
   people who could not have traded directly, with a "fairness meter" that shows
   the value gap and the cash top-up that closes it.

---

## Before you run these

**Who to talk to.** Aim for **6–10 people**, and deliberately split them:

| Group | How many | Why they matter |
|---|---|---|
| People who **own** gear that sits unused | 3–4 | The supply side. No lenders, no app. |
| People who **want to try** a sport but haven't bought kit | 3–4 | The demand side. |
| People who have **rented from a shop** or borrowed from friends | 1–2 | They know what the current alternative costs and feels like. |

Interviewing ten people who all think it's a nice idea proves nothing. One owner
who says "I would never lend my board to a stranger" is worth more to the report
than five polite yeses.

**How long.** 25–35 minutes. Q1–Q8 are conversation, no app on the table. Q9–Q10
are done with the prototype in their hands.

**Say this first (consent — put it in the appendix of your report):**

> I'm a student working on a design project for an app that lets people rent and
> swap sports gear with each other. This is research, not a sales pitch — I
> didn't build it to be right, I built it to be tested, so please be blunt. It'll
> take about half an hour. May I record the audio? I'll only use it for the
> assignment, I won't use your full name, and you can stop at any point.

**How to ask.** Ask about **what they did**, not what they would do. "Tell me
about the last time…" produces facts; "Would you use an app that…" produces
politeness. When they give you an opinion, follow it with *"can you tell me about
a time that happened?"*

**Don't defend the design.** If they misread a screen, that is the finding. Write
down the misreading word for word.

---

## The ten questions

### 1. Tell me about a piece of sports or outdoor equipment you own that you haven't used in the last three months. How did it end up that way?

*Tests: whether the supply side exists at all.*

The whole app assumes there is idle gear sitting in people's houses. Find out if
that is true, what it is, roughly what it cost, and why it stopped being used —
grew out of it, injury, moved, bought something better, seasonal.

**Probes:** What did it cost you? Where is it right now? Have you thought about
selling it? What stopped you?

**Listen for:** whether the reason it's idle is temporary (seasonal) or permanent
(finished with the sport). Permanent-idle owners are candidates for *swap*;
seasonal-idle owners are candidates for *rental*. That distinction is the whole
argument for the app having both.

---

### 2. Walk me through the last time you needed equipment you didn't own. What did you actually do?

*Tests: the real alternative GearLoop has to beat.*

Let them tell the whole story — borrowed from a friend, rented from a shop, bought
it and regretted it, borrowed through a Facebook or WhatsApp group, or just didn't
go.

**Probes:** How did you find it? What did it cost? How did you arrange to collect
it? What was annoying about it? What happened afterwards — did you have to clean
it, return it, pay for anything?

**Listen for:** the friction they mention *unprompted*. If nobody complains about
finding gear but three people complain about arranging the handover, the app's
weight is in the wrong place and S-13/S-14 matter more than S-04/S-05.

---

### 3. Have you ever ended up with the wrong size or wrong specification of equipment? What happened?

*Tests: the Fit Stamp — the single most distinctive decision in the app.*

GearLoop claims size is a safety issue, not a preference, and spends a whole
onboarding screen (S-02) deriving sizes from height and weight so every listing
can be stamped with a verdict. That claim needs evidence from real people.

**Probes:** How did you know it was wrong — before or after you used it? Did it
affect how safe you felt? How do you normally work out what size you need? Who
told you — a shop, a friend, the internet?

**Listen for:** whether they trust themselves to judge size. Someone who says
"I'd just ask the owner" is telling you the fit stamp is a convenience. Someone
who describes a scary session on the wrong board is telling you it's a safety
feature. Both are useful; you need to know which one you're building.

---

### 4. Imagine lending your own gear to someone in your area you've never met. What would have to be true before you said yes?

*Tests: trust tiers, verification, deposits, and whether the owner side is even possible.*

Let them generate the list themselves before you offer any of the app's answers.
Only after they run out, ask what they think of: verified ID, a bronze/silver/gold
trust tier from past rentals, public reviews, a refundable deposit held on the
renter's card.

**Probes:** What would you *never* lend, no matter what? Does the answer change
if it's worth LKR 10,000 versus LKR 150,000? Would seeing their past rentals
change your mind? What if they were a friend of a friend?

**Listen for:** whether trust comes from **the platform** (verification, holds,
insurance) or from **the person** (mutual friends, same area, met them once).
GearLoop currently bets on the platform. If your participants keep saying "I'd
lend it if I knew someone who knew them," that's a finding worth reporting.

---

### 5. A deposit is money frozen on your card and given back after — the app instead offers a protection plan: pay a small daily fee and the deposit drops to a small excess. Which would you rather have, and why?

*Tests: S-09 Protection & Deposit, the screen where money gets uncomfortable.*

Give them a concrete number: a rental costing LKR 2,800 a day, with a
LKR 15,000 deposit held, versus LKR 400 a day extra and only LKR 3,000 at risk.

**Probes:** Which feels safer as the *renter*? Which would you want as the
*owner*? How long would you accept the money being held? Has money ever been
held on your card before — how did that feel? What would make you abandon the
booking at that point?

**Listen for:** the deposit being a hard stop rather than a preference. If people
say the hold itself is the problem — not the amount — then protection isn't an
upsell, it's the default path, and the screen should be designed that way round.

---

### 6. If gear was returned to you damaged, how would you sort out who pays — and how would you prove it wasn't already like that?

*Tests: the condition-evidence chain (S-15 capture, S-18 settlement, S-34 claim, S-40 admin).*

Ask the owner-side version first, then flip it: *"Now imagine you're the renter
and you're being blamed for a mark that was already there. What would you want to
have?"*

**Probes:** How much of a mark is worth arguing about? Would you take photos
yourself? Would you want to do it together at pickup, or is that awkward? Who
should decide when you two can't agree?

**Listen for:** whether people would actually stand there and take seven photos
with a stranger. The app treats that as obviously worth it. If participants find
it slow, embarrassing, or distrustful, that is a genuine usability problem in the
flow the app is proudest of — and it belongs in your report.

---

### 7. Where and when would you hand the gear over? Talk me through how that meeting would go.

*Tests: S-13 Handover Hub, S-14 Scan Handshake, delivery as a filter, and the map screens.*

**Probes:** Would you give a stranger your home address? Would you rather meet
somewhere public? Would you pay extra for them to drop it off? What time of day
does this realistically happen — before work, weekend morning? What if they're
late? What if you're going straight to the beach at 6am?

**Listen for:** address privacy, and timing. The app only unlocks the exact
address after booking, which is a decision you can defend in the report *if* an
interview shows people care. Also listen for whether handover happens at the
gear's location or at the activity's location — that changes the map screen.

---

### 8. Instead of renting, imagine trading gear permanently — your old board for someone's kayak. Would you? And how would you two agree it's a fair trade?

*Tests: the whole Exchange flow (S-19 to S-23) and the Fairness Meter.*

Then introduce the hard case: *"What if their item is worth a bit more than
yours — should one of you pay the difference in cash, and who decides how much?"*
And the harder one: *"What if you want their kayak, they don't want your board,
but a third person wants your board and has what they want — would you do a
three-way trade?"*

**Probes:** How do you value second-hand gear today? Would you trust a number the
app suggests? What if you regret it a week later? Would you want to test it
first?

**Listen for:** whether swapping is a real desire or an interesting idea. This is
the riskiest feature in the app — five screens and the one animated element — so
it needs the strongest evidence. Also listen for the emotional block: people are
often much more attached to permanently giving something away than to lending it,
even when they never use it. If that comes up, quote it.

---

### 9. *(Prototype in their hands.)* Here's the app. You want a surfboard for the weekend — find one that fits you and book it. Please think out loud, and don't ask me for help.

*Tests: the Rent flow end to end (S-04 → S-18), which is the main demonstration.*

Hand over the phone and **say nothing**. Silence is the method. Count how many
seconds they hesitate and where.

Specific things to watch for, without pointing at them:

- Do they notice the **fit stamp**, and do they know what "WORKABLE" means?
- Do they read the **gear tag** (`6'6" · GRADE B · TQ-4471`) as useful, or as noise?
- Do they understand **Grade B** without opening anything?
- Do they scroll the **waiver** (S-10), or do they get stuck on the disabled
  button and blame the app?
- Does the **price breakdown** (S-11) answer their questions, or do they ask
  "what's this line?"

**Probes after, not during:** What did you expect to happen when you tapped that?
What was this screen asking you to do? Was there anything you didn't trust?

**Listen for:** the moment they slow down. Write down the screen number.

---

### 10. If this existed tomorrow, would you rent your own gear out on it? What's the one thing that would stop you — and what's the one thing that would change your mind?

*Tests: whether the concept survives contact, and what the next design iteration should be.*

The closing question, and the one whose answers you can count in the report. Push
past the polite yes: *"On a normal week, what would actually stop you opening
it?"*

**Probes:** What would you list first? What would you charge — and how did you
land on that number? What would make you delete it? Is there anything you'd
expect it to do that it doesn't?

**Listen for:** the single most-repeated blocker across all participants. That
one sentence is your strongest slide, and it's the honest answer to "what would
you do next?" — which is the question every examiner asks at the end.

---

## Writing it up

For the assignment, don't paste the transcripts. Do this instead:

1. **Tag every notable answer** with the participant number and the question —
   `P3/Q4: "I'd lend it to someone in Nallur, not to someone I've never heard of."`
2. **Group tags that say the same thing** into 4–6 themes. A theme needs at least
   two people from different participants; one person's opinion is an anecdote.
3. **Turn each theme into a design implication** and name the screen it lands on:

   > *Theme:* Owners trust the person, not the platform. (P1, P3, P4, P7)
   > *Implication:* The trust profile (S-32) should lead with mutual connections
   > and area, not with the tier badge.
   > *Status:* not currently designed — logged as next iteration.

4. **Include the contradictions.** If two participants want opposite things, say
   so and say which one you designed for and why. That paragraph scores better
   than a page of agreement.
5. **Report what you'd change and didn't.** A finding you chose not to act on,
   with a stated reason, shows judgement. A findings section where every single
   thing was already built exactly right shows the interviews were decorative.

**Note for the demo:** the prototype has no backend, login or saved data — every
screen renders from a fixed dataset. For Q9, open the app and start **Flow A —
Rent** from the cover screen; the guide bar keeps the session on the same
eleven-screen path for every participant, which is what makes their timings and
hesitations comparable. Tap **Reset the demo** between participants so nobody
inherits the last person's taps.
