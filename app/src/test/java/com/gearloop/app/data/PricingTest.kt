package com.gearloop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pricing engine that replaced the prototype's typed-in figures.
 *
 * Milestone 02 priced one basket as constants in [Demo]. Milestone 03 computes
 * every basket, so the first job of these tests is to hold the engine to the
 * designed one: three days of the Torq on Plus, with the rash guard and the
 * FIRSTLOOP coupon, is LKR 9,240 line for line, and the listing footer that
 * quotes the same board before extras is LKR 9,576.
 */
class PricingTest {

    private val torq = Demo.torq.copy(extras = listOf(Extra("Rash guard", Demo.EXTRA_RASHGUARD / Demo.BOOKING_DAYS)))

    private fun quote(
        days: Int = 3,
        protection: Protection = Protection.PLUS,
        coupon: Boolean = true,
        extras: Boolean = true,
        item: GearItem = torq,
    ) = Pricing.quote(item, days, protection, coupon, extras)

    @Test
    fun `the designed basket still comes to the designed total`() {
        val q = quote()
        assertEquals(Demo.RENTAL_BASE, q.base)
        assertEquals(Demo.DURATION_DISCOUNT, q.durationDiscount)
        assertEquals(Demo.EXTRA_RASHGUARD, q.extras.sumOf { it.second })
        assertEquals(Demo.PROTECTION_PLUS, q.protectionCost)
        assertEquals(Demo.SERVICE_FEE, q.serviceFee)
        assertEquals(Demo.COUPON, q.coupon)
        assertEquals(Demo.BOOKING_TOTAL, q.total)
        assertEquals(9_240, q.total)
    }

    @Test
    fun `the bill prints the same lines the design lists`() {
        assertEquals(Demo.priceBreakdown, quote().lines())
    }

    @Test
    fun `the listing footer is the same board before extras and coupon`() {
        assertEquals(Demo.LISTING_FOOTER_TOTAL, Pricing.listingFooter(torq, 3))
        assertEquals(9_576, Pricing.listingFooter(torq, 3))
        assertEquals(Pricing.listingFooter(torq, 3), quote(coupon = false, extras = false).total)
    }

    @Test
    fun `taking the extra off re-quotes the fee as well as the line`() {
        // 7,560 rental + 1,260 Plus + 756 fee − 831 coupon.
        assertEquals(8_745, quote(extras = false).total)
    }

    @Test
    fun `the service fee is never charged on protection`() {
        val basic = quote(protection = Protection.BASIC)
        val pro = quote(protection = Protection.PRO)
        assertEquals(basic.serviceFee, pro.serviceFee)
        assertEquals(Protection.PRO.perDay * 3 - Protection.BASIC.perDay * 3, pro.total - basic.total)
    }

    @Test
    fun `only Basic holds a deposit`() {
        assertEquals(torq.deposit, quote(protection = Protection.BASIC).depositHold)
        assertEquals(0, quote(protection = Protection.PLUS).depositHold)
        assertEquals(0, quote(protection = Protection.PRO).depositHold)
    }

    @Test
    fun `the most you could lose is the deposit, the excess, or nothing`() {
        assertEquals(torq.deposit, Protection.BASIC.mostYouCouldLose(torq.deposit))
        assertEquals(3_000, Protection.PLUS.mostYouCouldLose(torq.deposit))
        assertEquals(0, Protection.PRO.mostYouCouldLose(torq.deposit))
    }

    @Test
    fun `duration discounts step at three, seven and fourteen days when switched on`() {
        val all = listOf(true, true, true)
        assertEquals(0, Pricing.durationDiscountPercent(2, all))
        assertEquals(10, Pricing.durationDiscountPercent(3, all))
        assertEquals(20, Pricing.durationDiscountPercent(7, all))
        assertEquals(30, Pricing.durationDiscountPercent(14, all))
        // The owner's switches on S-26 are real: off means off.
        assertEquals(0, Pricing.durationDiscountPercent(14, listOf(false, false, false)))
        assertEquals(10, Pricing.durationDiscountPercent(14, listOf(true, false, false)))
    }

    @Test
    fun `the coupon applies only to an eligible first booking`() {
        assertEquals(Pricing.COUPON_AMOUNT, quote(coupon = true).coupon)
        assertEquals(0, quote(coupon = false).coupon)
        assertEquals(quote(coupon = false).total - Pricing.COUPON_AMOUNT, quote(coupon = true).total)
    }

    @Test
    fun `the owner is paid the rental and the extras, never the fee or the protection`() {
        val q = quote()
        assertEquals(q.rentalSubtotal + q.extras.sumOf { it.second }, q.ownerPayout)
    }

    @Test
    fun `the fee explanation states the rule the arithmetic applies (D-01)`() {
        val sentence = quote().explanations().first { it.first == "Service fee" }.second
        assertTrue(sentence.startsWith("${Pricing.SERVICE_FEE_PERCENT}% of the rental and extras"))
        assertEquals(Pricing.serviceFeeOn(quote().rentalSubtotal + Demo.EXTRA_RASHGUARD), quote().serviceFee)
    }

    @Test
    fun `every line on the bill has an explanation (FR-20)`() {
        val q = quote()
        val explained = q.explanations().map { it.first }
        assertTrue("Rental" in explained)
        assertTrue("Discount" in explained)
        assertTrue("Rash guard" in explained)
        assertTrue("Protection" in explained)
        assertTrue("Service fee" in explained)
        assertTrue("Coupon" in explained)
    }
}
