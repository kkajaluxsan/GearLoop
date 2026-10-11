package com.gearloop.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for the accounts on this phone.
 *
 * PBKDF2 with HMAC-SHA256 and a random 16-byte salt per account, as NIST
 * SP 800-132 describes. Only the salt and the derived key are stored; the
 * password itself never reaches the database. Comparison is constant-time, so
 * a wrong password takes as long to reject as a nearly right one.
 */
object Passwords {

    const val ITERATIONS = 40_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private val random = SecureRandom()

    data class Hashed(val hash: String, val salt: String)

    fun hash(password: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return Hashed(derive(password, salt), Base64.getEncoder().encodeToString(salt))
    }

    fun verify(password: String, hash: String, salt: String): Boolean {
        val saltBytes = runCatching { Base64.getDecoder().decode(salt) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(hash) }.getOrNull() ?: return false
        val actual = Base64.getDecoder().decode(derive(password, saltBytes))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }
}

/**
 * The rules a form field is checked against, in one place, so the sign-up form,
 * the settings screen and the card form say the same thing about the same input.
 * Each returns null when the value is fine, or the sentence to show under it.
 */
object Validate {

    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun name(value: String): String? = when {
        value.isBlank() -> "Enter your name"
        value.trim().length < 2 -> "That name is too short"
        !value.trim().contains(' ') -> "Enter your first and last name"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email address"
        !emailPattern.matches(value.trim()) -> "That doesn't look like an email address"
        else -> null
    }

    fun password(value: String): String? = when {
        value.length < 8 -> "Use at least 8 characters"
        value.none(Char::isLetter) || value.none(Char::isDigit) -> "Use letters and at least one number"
        else -> null
    }

    /** A Sri Lankan mobile number: 07X XXX XXXX, or +94 7X XXX XXXX. */
    fun phone(value: String): String? {
        val digits = value.filter(Char::isDigit)
        val local = when {
            digits.startsWith("94") && digits.length == 11 -> "0" + digits.drop(2)
            else -> digits
        }
        return if (local.length == 10 && local.startsWith("07")) null else "Enter a mobile number like 077 123 4567"
    }

    fun normalisePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        val local = if (digits.startsWith("94") && digits.length == 11) "0" + digits.drop(2) else digits
        return if (local.length == 10) "${local.take(3)} ${local.substring(3, 6)} ${local.drop(6)}" else value.trim()
    }

    /** The Luhn check every card number carries. */
    fun cardNumber(value: String): String? {
        val digits = value.filter(Char::isDigit)
        if (digits.length !in 13..19) return "Card numbers are 13 to 19 digits"
        var sum = 0
        digits.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return if (sum % 10 == 0) null else "That card number isn't valid"
    }

    /** MM/YY, not in the past. */
    fun cardExpiry(value: String, today: java.time.LocalDate = AppClock.today): String? {
        val digits = value.filter(Char::isDigit)
        if (digits.length != 4) return "Use MM/YY"
        val month = digits.take(2).toInt()
        val year = 2000 + digits.drop(2).toInt()
        if (month !in 1..12) return "Use MM/YY"
        val endOfMonth = java.time.YearMonth.of(year, month).atEndOfMonth()
        return if (endOfMonth.isBefore(today)) "This card has expired" else null
    }

    fun cardBrand(number: String): String {
        val digits = number.filter(Char::isDigit)
        return when {
            digits.startsWith("4") -> "Visa"
            digits.take(2).toIntOrNull() in 51..55 || digits.take(4).toIntOrNull() in 2221..2720 -> "Mastercard"
            digits.startsWith("34") || digits.startsWith("37") -> "Amex"
            else -> "Card"
        }
    }
}
