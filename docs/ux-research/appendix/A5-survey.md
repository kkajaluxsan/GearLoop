# Appendix A5 — Survey Instrument & Responses

Strand C. Eight closed questions, about four minutes. Two jobs: establish that
the individuals in the interviews are not unusual, and recruit for Strands A
and B.

> **Fill this from real responses.** The figures cited in the report as `SUR-Q#`
> are the proxy set until this table is real.

**Target:** ≥ 15 responses. The proxy set assumes 18.

**Distribution:** a Google Form link posted to a university sports club group and
two surfing/cycling community groups, plus a QR code at a beach rental shack.
Record where each response came from — a survey answered entirely by your own
year group is a survey of your own year group.

---

## The instrument

Copy this into a form. Keep it to eight questions; a nine-question survey has a
measurably worse completion rate and the ninth question is never the important
one.

**Screening / context**

**SUR-Q1.** Which of these describes you? *(select all)*
☐ I do a sport or outdoor activity that needs equipment
☐ I own equipment for one
☐ I've rented or borrowed equipment
☐ I lend equipment to others
☐ I manage equipment for a club, shop or organisation
☐ None of these

*(Also the recruitment screen: box 5 identifies SC-3, the hardest category to
find and the one most student projects skip entirely.)*

**Supply — feeds F-01**

**SUR-Q2.** How many pieces of sports or outdoor equipment do you own that you
have **not used in the last 3 months**?
○ None ○ 1 ○ 2–3 ○ 4 or more

**SUR-Q3.** Roughly what is the most valuable single item among them?
○ Under LKR 10,000 ○ 10,000–25,000 ○ 25,000–75,000 ○ Over 75,000 ○ N/A

**Demand — feeds F-02**

**SUR-Q4.** In the last year, when you needed equipment you didn't own, what did
you do most often?
○ Borrowed from a friend ○ Rented from a shop ○ Asked in a group chat
○ Bought it ○ Didn't go

**Fit — feeds F-03**

**SUR-Q5.** Have you ever used sports equipment that turned out to be the wrong
size or specification for you?
○ Yes ○ No ○ Not sure

**SUR-Q5b.** *(if yes)* Which is closer to how it felt?
○ It affected how well I performed
○ **It affected how safe I felt**

*(The split between these two is the entire evidence base for treating fit as a
safety property rather than a preference. Do not merge them.)*

**Money — feeds F-05**

**SUR-Q6a.** What is the most you would be willing to leave as a **deposit** to
rent equipment worth about LKR 90,000?
○ Nothing ○ Up to 5,000 ○ 5,000–15,000 ○ 15,000–30,000 ○ Over 30,000

**SUR-Q6b.** Same question, different wording — the most you'd accept as **a hold
placed on your card and released when you return the item undamaged**?
○ Nothing ○ Up to 5,000 ○ 5,000–15,000 ○ 15,000–30,000 ○ Over 30,000

> **Ask both. Do not put them next to each other** — separate them with Q7, or
> the respondent will notice they are the same question and answer consistently
> out of politeness.
>
> **The gap between 6a and 6b is the single most quotable number in this study.**
> The proxy set has the median moving from LKR 5,000 to LKR 20,000 on wording
> alone, and that factor of four is what justifies **FR-04** and **FR-05**. If
> your real data shows no gap, **F-05 is wrong** and the report must say so —
> that is a legitimate outcome and a better one than a confirmed assumption.

**Swap — feeds F-01, F-08**

**SUR-Q7.** Would you consider **trading** equipment you've finished with for
something else, rather than renting it out or selling it?
○ Yes ○ No ○ Depends what for

**Recruitment**

**SUR-Q8.** Can I contact you for a 30-minute conversation about this?
○ Yes — *(contact field)* ○ No

*(Keep contact details out of this repository. Store them separately and delete
them once recruitment is closed.)*

---

## Response summary

Fill in as `n / 18` alongside the percentage. Report **fractions** in the report
body, not percentages, when n is small.

| Question | Response | n | % | Cited as |
|---|---|---|---|---|
| **Q1** | Own equipment | | | recruitment |
| | Manage equipment for an organisation (SC-3) | | | recruitment |
| **Q2** | ≥1 item unused for 3 months | | | **SUR-Q2** → F-01 |
| | 2 or more | | | |
| **Q3** | Most valuable unused item over LKR 25,000 | | | **SUR-Q2** → F-01 |
| **Q4** | Asked in a group chat | | | **SUR-Q4** → F-02 |
| | Rented from a shop | | | |
| | Didn't go | | | |
| **Q5** | Used wrong-sized equipment | | | **SUR-Q5** → F-03 |
| **Q5b** | …and it affected how **safe** they felt | | | **SUR-Q5** → F-03 |
| **Q6a** | Median acceptable **deposit** | LKR | | **SUR-Q6** → F-05 |
| **Q6b** | Median acceptable **hold** | LKR | | **SUR-Q6** → F-05 |
| **Q7** | Would consider trading | | | **SUR-Q7** → F-01 |

### Proxy figures currently cited in the report

Replace these. They are what the narrative in
[`01-research-data.md`](../01-research-data.md) is currently written against, so
if your real numbers differ, the findings text needs the same edit.

| Cited as | Proxy value | Used in |
|---|---|---|
| **SUR-Q2** | 14 of 18 own ≥1 item unused for 3 months; 9 of those named an item over LKR 25,000 | F-01 |
| **SUR-Q5** | 11 of 18 used wrong-sized equipment; **6 of those 11** framed it as safety, not performance | F-03 |
| **SUR-Q6** | Median acceptable deposit **LKR 5,000**; median acceptable *hold* **LKR 20,000** | F-05, HMW-1, FR-04 |

---

## Analysis notes

- **Report fractions, not percentages, at n = 18.** "14 of 18" is honest;
  "77.8 %" implies a precision eighteen people cannot carry.
- **The survey corroborates; it does not discover.** Its job is to stop a single
  interview being an anecdote. Nothing in this study rests on the survey alone,
  and nothing should.
- **Record the distribution channel per response.** If two thirds came from one
  club, say so in the limitations.
- **Keep the raw export.** `raw/survey-responses.csv`, referenced from
  [A2](A2-participant-log.md). Constraint **d** asks for *completed
  questionnaires*, and the CSV is what that means here.
