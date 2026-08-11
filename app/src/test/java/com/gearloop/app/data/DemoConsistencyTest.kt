package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dataset checked against itself.
 *
 * [Demo] is one source of truth for forty-two screens, and the failure mode that
 * matters here is not a crash — it is two screens quoting the same fact
 * differently and nobody noticing until it is on a projector. Every case below
 * is a disagreement that was actually in the build:
 *
 *  - the price breakdown's lines did not add to the total it printed, and the
 *    listing footer quoted a third figure that no combination of them produced;
 *  - the review histogram's bars summed to 36 under a heading saying 37, and
 *    the average printed above them was not the average of either;
 *  - the same tail ding was cosmetic on one screen and functional on the next,
 *    which had Kavitha claiming for damage her own pickup record showed;
 *  - the requests inbox priced every booking at three days whatever its dates
 *    said, and the wallet held a deposit against a booking three other screens
 *    described as having no deposit.
 *
 * None of these are things a compiler or a screenshot can catch.
 */
class DemoConsistencyTest {

    // ------------------------------------------------------------------ money

    @Test
    fun `the price breakdown adds up to the total it prints`() {
        val lines = Demo.priceBreakdown
        val total = lines.last()
        assertEquals("last line should be the total", PriceLine.Kind.TOTAL, total.kind)

        // Read the figures back off the rendered strings, so this checks what
        // S-11 actually shows rather than the constants behind it.
        val sum = lines.dropLast(1).sumOf { it.value.asAmount() }
        assertEquals(total.value.asAmount(), sum)
        assertEquals(Demo.BOOKING_TOTAL, sum)
    }

    @Test
    fun `the date panel subtotal is the base less the duration discount`() {
        val subtotal = Demo.datePanelLines.last()
        assertEquals(PriceLine.Kind.TOTAL, subtotal.kind)
        assertEquals(Demo.RENTAL_BASE - Demo.DURATION_DISCOUNT, subtotal.value.asAmount())
        assertEquals(Demo.RENTAL_SUBTOTAL, subtotal.value.asAmount())
    }

    @Test
    fun `the listing footer quotes the same basket minus the extras and the coupon`() {
        // S-07 has no extras chosen on it and no coupon applied, so it must come
        // out above the checkout total — never below it, which would mean the
        // price went up at the till.
        assertTrue(
            "listing footer ${Demo.LISTING_FOOTER_TOTAL} should exceed checkout ${Demo.BOOKING_TOTAL}",
            Demo.LISTING_FOOTER_TOTAL > Demo.BOOKING_TOTAL,
        )
        // The whole gap is accounted for: the footer's basket has no extra on it
        // (and so no service fee on that extra), and no coupon taken off yet.
        val difference = Demo.LISTING_FOOTER_TOTAL - Demo.BOOKING_TOTAL
        val explained = Demo.COUPON - Demo.EXTRA_RASHGUARD - Demo.EXTRA_RASHGUARD / 10
        assertEquals("the gap should be the coupon less the extra and its fee", explained, difference)
    }

    @Test
    fun `the rental base is the daily rate times the booked days`() {
        assertEquals(Demo.torq.pricePerDay * Demo.BOOKING_DAYS, Demo.RENTAL_BASE)
    }

    @Test
    fun `nothing the renter is charged for is also listed as included`() {
        val charged = Demo.priceBreakdown
            .filter { it.kind == PriceLine.Kind.NORMAL }
            .map { it.label.lowercase() }
        Demo.torqIncluded.forEach { included ->
            assertTrue(
                "\"$included\" is listed as included on S-07 but charged for on S-11",
                charged.none { it == included.lowercase() },
            )
        }
    }

    // ---------------------------------------------------------------- reviews

    @Test
    fun `the review histogram sums to the review count on the same screen`() {
        assertEquals(Demo.starDistribution.total, Demo.kavitha.reviews)
        assertEquals(
            Demo.kavitha.reviews,
            (1..5).sumOf { Demo.starDistribution.at(it) },
        )
    }

    @Test
    fun `the rating printed above the histogram is the average of it`() {
        assertEquals(Demo.starDistribution.average, Demo.kavitha.rating, 0.0001)
        // And it survives being formatted for display, which is the form four
        // different screens show it in.
        assertEquals("4.94", Demo.kavitha.ratingText)
    }

    // ----------------------------------------------------------------- damage

    @Test
    fun `the pickup mark and the return mark are different marks`() {
        assertTrue(
            "a claim for damage already on the pickup record is not a claim",
            Demo.PICKUP_MARK.note != Demo.RETURN_MARK.note,
        )
        assertEquals(DamageMark.Severity.COSMETIC, Demo.PICKUP_MARK.severity)
        assertEquals(DamageMark.Severity.FUNCTIONAL, Demo.RETURN_MARK.severity)
    }

    @Test
    fun `the claim sits under the cap its own severity sets`() {
        val ceiling = Demo.RETURN_MARK.capOn(Demo.BOARD_VALUE)
        assertTrue(
            "claim ${Demo.CLAIM_AMOUNT} exceeds the $ceiling ceiling the rubric allows",
            Demo.CLAIM_AMOUNT <= ceiling,
        )
    }

    // ---------------------------------------------------------------- bookings

    @Test
    fun `every request in the inbox is priced for the nights it asks for`() {
        Demo.requestInbox.forEach { request ->
            assertEquals(
                "${request.renter.name}'s request",
                request.item.pricePerDay * request.days,
                request.total,
            )
        }
    }

    @Test
    fun `requests do not all share one countdown`() {
        val clocks = Demo.requestInbox.map { it.respondWithin }
        assertEquals("each request needs its own clock", clocks.size, clocks.distinct().size)
    }

    @Test
    fun `the booking covered by Plus protection has no hold against it`() {
        // S-09, S-11 and S-17 all say Plus means nothing is held. The wallet has
        // to agree with them.
        assertTrue(
            "a hold is listed against $${Demo.BOOKING_REF}, which took Plus protection",
            Demo.walletHolds.none { it.item.contains(Demo.BOOKING_REF) },
        )
        assertTrue(
            "the wallet should say why that booking has no hold",
            Demo.coveredNotHeld.any { it.first.contains(Demo.BOOKING_REF) },
        )
    }

    @Test
    fun `the wallet charge for the booking matches what checkout took`() {
        val booking = Demo.walletTxns.first { it.label.contains(Demo.BOOKING_REF) }
        assertEquals(-Demo.BOOKING_TOTAL, booking.amount)
    }

    // ------------------------------------------------------------------ angles

    @Test
    fun `the listing flow asks for the same angles the handover checks`() {
        // S-25's own copy claims they are the same set, and for a while they
        // were not — it wanted "Rails" and "Fins" where capture wants "Rail L",
        // "Rail R" and "Fin box".
        assertEquals(7, Demo.captureAngles.size)
        assertEquals(
            Demo.captureAngles.map { it.label },
            Demo.returnComparison.map { it.first },
        )
    }

    private fun String.asAmount(): Int {
        if (this == "—") return 0
        val digits = filter { it.isDigit() }
        if (digits.isEmpty()) return 0
        val magnitude = digits.toInt()
        return if (startsWith(MINUS)) -magnitude else magnitude
    }
}
