package com.gearloop.app.data

/**
 * What a booking costs, worked out rather than written down.
 *
 * The prototype priced one basket — the Torq, three days, Plus, a rash guard and
 * the FIRSTLOOP coupon — as constants in [Demo]. This is that arithmetic made
 * general, so any listing, any dates and any plan come out right, and a unit
 * test holds it to the designed basket: three days of the Torq on Plus must
 * still come to LKR 9,240, line for line.
 *
 * Every rule a renter is told on S-11 is a rule here, and nowhere else:
 *  - the duration discount is 10 % at three days, 20 % at seven, 30 % at
 *    fourteen, each one only if the owner switched it on (S-26);
 *  - the service fee is 10 % of the rental and its extras after discounts, and
 *    is never charged on protection;
 *  - FIRSTLOOP takes LKR 831 off a renter's first booking only;
 *  - only Basic holds a deposit, and an owner can waive it for Plus renters.
 */
object Pricing {

    const val SERVICE_FEE_PERCENT = 10
    const val COUPON_CODE = "FIRSTLOOP"
    const val COUPON_AMOUNT = 831

    fun durationDiscountPercent(days: Int, discounts: List<Boolean>): Int = when {
        days >= 14 && discounts.getOrElse(2) { false } -> 30
        days >= 7 && discounts.getOrElse(1) { false } -> 20
        days >= 3 && discounts.getOrElse(0) { false } -> 10
        else -> 0
    }

    fun serviceFeeOn(amount: Int): Int = amount * SERVICE_FEE_PERCENT / 100

    fun quote(
        item: GearItem,
        days: Int,
        protection: Protection,
        couponEligible: Boolean,
        includeExtras: Boolean = true,
    ): Quote {
        val base = item.pricePerDay * days
        val discount = base * durationDiscountPercent(days, item.discounts) / 100
        val subtotal = base - discount
        val extras = if (includeExtras) item.extras.map { it.name to it.perDay * days } else emptyList()
        val extrasTotal = extras.sumOf { it.second }
        val protectionCost = protection.perDay * days
        val fee = serviceFeeOn(subtotal + extrasTotal)
        val coupon = if (couponEligible) COUPON_AMOUNT else 0
        val deposit = when {
            protection.holdsDeposit -> item.deposit
            else -> 0
        }
        return Quote(
            pricePerDay = item.pricePerDay,
            days = days,
            base = base,
            discountPercent = durationDiscountPercent(days, item.discounts),
            durationDiscount = discount,
            extras = extras,
            protection = protection,
            protectionCost = protectionCost,
            serviceFee = fee,
            coupon = coupon,
            depositHold = deposit,
        )
    }

    /**
     * The figure S-07's footer quotes: the rental, Plus protection and the fee on
     * the rental — no extras chosen yet and no coupon applied, which is why it is
     * captioned "before extras" and sits above the checkout total.
     */
    fun listingFooter(item: GearItem, days: Int): Int {
        val base = item.pricePerDay * days
        val subtotal = base - base * durationDiscountPercent(days, item.discounts) / 100
        return subtotal + Protection.PLUS.perDay * days + serviceFeeOn(subtotal)
    }
}

data class Quote(
    val pricePerDay: Int,
    val days: Int,
    val base: Int,
    val discountPercent: Int,
    val durationDiscount: Int,
    val extras: List<Pair<String, Int>>,
    val protection: Protection,
    val protectionCost: Int,
    val serviceFee: Int,
    val coupon: Int,
    val depositHold: Int,
) {
    val rentalSubtotal: Int get() = base - durationDiscount

    val total: Int get() = rentalSubtotal + extras.sumOf { it.second } + protectionCost + serviceFee - coupon

    /** What the owner is paid: the rental and the extras, nothing else. */
    val ownerPayout: Int get() = rentalSubtotal + extras.sumOf { it.second }

    private val dayWord: String get() = if (days == 1) "day" else "days"

    /** The panel under the S-08 calendar. */
    fun dateLines(): List<PriceLine> = buildList {
        add(PriceLine("${lkr(pricePerDay)} × $days $dayWord", grouped(base)))
        if (durationDiscount > 0) {
            add(PriceLine("$days-day discount", signed(-durationDiscount), PriceLine.Kind.DISCOUNT))
        }
        add(PriceLine("Subtotal", grouped(rentalSubtotal), PriceLine.Kind.TOTAL))
    }

    /** The bill on S-11, in the order the design lists it. */
    fun lines(): List<PriceLine> = buildList {
        add(PriceLine("${lkr(pricePerDay)} × $days $dayWord", grouped(base)))
        if (durationDiscount > 0) {
            add(PriceLine("$days-day discount", signed(-durationDiscount), PriceLine.Kind.DISCOUNT))
        }
        extras.forEach { (name, amount) -> add(PriceLine(name, grouped(amount))) }
        add(PriceLine("Delivery", "—", PriceLine.Kind.MUTED))
        if (protectionCost > 0) {
            add(PriceLine("${protection.label} protection", grouped(protectionCost)))
        } else {
            add(PriceLine("${protection.label} protection", "Included", PriceLine.Kind.MUTED))
        }
        add(PriceLine("Service fee", grouped(serviceFee)))
        if (coupon > 0) {
            add(PriceLine("Coupon ${Pricing.COUPON_CODE}", signed(-coupon), PriceLine.Kind.DISCOUNT))
        }
        add(PriceLine("Total", lkr(total), PriceLine.Kind.TOTAL))
    }

    /**
     * One plain sentence per line, for the "what is this?" disclosure (FR-20):
     * every line on a bill should explain itself without leaving the screen.
     */
    fun explanations(): List<Pair<String, String>> = buildList {
        add("Rental" to "${lkr(pricePerDay)} a day for $days $dayWord.")
        if (durationDiscount > 0) {
            add("Discount" to "$discountPercent% off the rental for booking $days days or more, set by the owner.")
        }
        extras.forEach { (name, amount) -> add(name to "Rented with the item: ${lkr(amount)} for $days $dayWord.") }
        add(
            "Protection" to if (protectionCost > 0) {
                "${protection.label} is ${lkr(protection.perDay)} a day and replaces the deposit hold."
            } else {
                "Basic is included. A deposit is held on your card instead."
            },
        )
        add(
            "Service fee" to "${Pricing.SERVICE_FEE_PERCENT}% of the rental and extras after discounts. " +
                "It pays for the condition record, the claims desk and payment handling. " +
                "It is never charged on protection.",
        )
        if (coupon > 0) add("Coupon" to "${Pricing.COUPON_CODE} takes ${lkr(coupon)} off your first booking.")
    }
}
