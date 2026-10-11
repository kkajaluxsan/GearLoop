package com.gearloop.app.data.repo

import androidx.room.withTransaction
import com.gearloop.app.data.AccountRole
import com.gearloop.app.data.AppClock
import com.gearloop.app.data.Passwords
import com.gearloop.app.data.Person
import com.gearloop.app.data.TrustTier
import com.gearloop.app.data.Validate
import com.gearloop.app.data.db.AccountEntity
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.db.PersonEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A member's account, as the screens see it: never the password hash. */
data class Account(
    val handle: String,
    val email: String,
    val phone: String?,
    val role: AccountRole,
    val person: Person,
    val lastLoginAt: Long?,
) {
    val isAdmin: Boolean get() = role == AccountRole.ADMIN
}

/**
 * Sign-up, sign-in and the account itself.
 *
 * Accounts live on this phone, in the same database as everything else; there
 * is no server to send a password to. A password is checked against its salted
 * PBKDF2 hash ([Passwords]) and is never stored or logged. Creating an account
 * writes the member's public record, their profile, their verification steps
 * and their badges in one transaction, so an account is never half-made.
 */
class AccountRepository(
    private val db: GearDatabase,
    private val store: GearStore,
    private val session: SessionStore,
    scope: CoroutineScope,
) {
    private val dao = db.accounts()

    enum class Field { NAME, EMAIL, PASSWORD, PHONE, AREA }

    sealed interface Result {
        data object Ok : Result
        data class Failed(val field: Field?, val message: String) : Result
    }

    /** Every account on this phone, most recently used first. */
    val accounts: StateFlow<List<Account>> = combine(dao.accounts(), store.people) { rows, people ->
        rows.map { it.toAccount(people) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The signed-in member's account, or null when signed out. */
    val current: StateFlow<Account?> = combine(session.me, accounts) { me, list ->
        list.firstOrNull { it.handle == me }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    // ============================================================ sign up / in / out

    suspend fun signUp(name: String, email: String, password: String, area: String, phone: String?): Result {
        Validate.name(name)?.let { return Result.Failed(Field.NAME, it) }
        Validate.email(email)?.let { return Result.Failed(Field.EMAIL, it) }
        Validate.password(password)?.let { return Result.Failed(Field.PASSWORD, it) }
        if (area.isBlank()) return Result.Failed(Field.AREA, "Choose where you are")
        val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanPhone != null) Validate.phone(cleanPhone)?.let { return Result.Failed(Field.PHONE, it) }

        val address = email.trim().lowercase(Locale.ENGLISH)
        if (dao.byEmail(address) != null) return Result.Failed(Field.EMAIL, "There's already an account with this email. Log in instead.")

        val hashed = withContext(Dispatchers.Default) { Passwords.hash(password) }
        val now = AppClock.nowMillis()
        val handle = freeHandle(name)
        val since = AppClock.today.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
        db.withTransaction {
            dao.upsert(
                AccountEntity(
                    handle = handle, email = address, passwordHash = hashed.hash, salt = hashed.salt,
                    phone = cleanPhone?.let(Validate::normalisePhone), role = AccountRole.MEMBER,
                    createdAt = now, lastLoginAt = now,
                ),
            )
            db.discovery().upsertPerson(
                PersonEntity(
                    handle = handle, name = cleanName(name), area = area.trim(), tier = TrustTier.BRONZE,
                    rating = 0.0, reviews = 0, respondsIn = "~1 h", listings = 0, memberSince = since,
                    completedRentals = 0, onTimeReturns = 100, verifiedSteps = if (cleanPhone != null) 2 else 1,
                ),
            )
            val seed = db.seed()
            seed.profiles(listOf(Seed.newProfile(handle)))
            seed.verification(Seed.newVerification(handle, phoneDone = cleanPhone != null, phone = cleanPhone, at = now))
            seed.badges(Seed.newBadges(handle))
            seed.trips(listOf(Seed.newTrip(handle)))
            seed.tripItems(Seed.tripExtras(handle))
        }
        session.signIn(handle)
        return Result.Ok
    }

    suspend fun signIn(email: String, password: String): Result {
        Validate.email(email)?.let { return Result.Failed(Field.EMAIL, it) }
        if (password.isEmpty()) return Result.Failed(Field.PASSWORD, "Enter your password")
        val row = dao.byEmail(email.trim().lowercase(Locale.ENGLISH))
            ?: return Result.Failed(Field.EMAIL, "No account uses this email. Create one instead.")
        val ok = withContext(Dispatchers.Default) { Passwords.verify(password, row.passwordHash, row.salt) }
        if (!ok) return Result.Failed(Field.PASSWORD, "That password isn't right")
        dao.touch(row.handle, AppClock.nowMillis())
        session.signIn(row.handle)
        return Result.Ok
    }

    suspend fun signOut() = session.signOut()

    // ============================================================ the account itself

    /** Update the public details: name, area and phone. */
    suspend fun updateDetails(name: String, area: String, phone: String?): Result {
        val handle = session.handle.ifEmpty { return Result.Failed(null, "You're signed out") }
        Validate.name(name)?.let { return Result.Failed(Field.NAME, it) }
        if (area.isBlank()) return Result.Failed(Field.AREA, "Choose where you are")
        val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanPhone != null) Validate.phone(cleanPhone)?.let { return Result.Failed(Field.PHONE, it) }
        val account = dao.byHandle(handle) ?: return Result.Failed(null, "Account not found")
        val person = db.discovery().person(handle) ?: return Result.Failed(null, "Account not found")
        db.withTransaction {
            dao.upsert(account.copy(phone = cleanPhone?.let(Validate::normalisePhone)))
            db.discovery().upsertPerson(person.copy(name = cleanName(name), area = area.trim()))
        }
        return Result.Ok
    }

    suspend fun changePassword(current: String, next: String): Result {
        val handle = session.handle.ifEmpty { return Result.Failed(null, "You're signed out") }
        val row = dao.byHandle(handle) ?: return Result.Failed(null, "Account not found")
        val ok = withContext(Dispatchers.Default) { Passwords.verify(current, row.passwordHash, row.salt) }
        if (!ok) return Result.Failed(Field.PASSWORD, "Your current password isn't right")
        Validate.password(next)?.let { return Result.Failed(null, it) }
        val hashed = withContext(Dispatchers.Default) { Passwords.hash(next) }
        dao.upsert(row.copy(passwordHash = hashed.hash, salt = hashed.salt))
        return Result.Ok
    }

    /**
     * Forgotten password: proven with the mobile number on the account, since
     * there is no email server to send a reset link from. Signs in on success.
     */
    suspend fun resetPassword(email: String, phone: String, next: String): Result {
        Validate.email(email)?.let { return Result.Failed(Field.EMAIL, it) }
        val row = dao.byEmail(email.trim().lowercase(Locale.ENGLISH))
            ?: return Result.Failed(Field.EMAIL, "No account uses this email")
        val onFile = row.phone?.filter(Char::isDigit)
            ?: return Result.Failed(Field.PHONE, "This account has no mobile number to check against")
        val given = Validate.normalisePhone(phone).filter(Char::isDigit)
        if (given != onFile) return Result.Failed(Field.PHONE, "That isn't the number on this account")
        Validate.password(next)?.let { return Result.Failed(Field.PASSWORD, it) }
        val hashed = withContext(Dispatchers.Default) { Passwords.hash(next) }
        dao.upsert(row.copy(passwordHash = hashed.hash, salt = hashed.salt, lastLoginAt = AppClock.nowMillis()))
        session.signIn(row.handle)
        return Result.Ok
    }

    /**
     * Delete the signed-in account. Everything that was only theirs goes with
     * it; their listings come out of search; bookings and conversations they
     * share with other members stay, under "Former member", because those are
     * the other member's records too.
     */
    suspend fun deleteAccount(password: String): Result {
        val handle = session.handle.ifEmpty { return Result.Failed(null, "You're signed out") }
        val row = dao.byHandle(handle) ?: return Result.Failed(null, "Account not found")
        val ok = withContext(Dispatchers.Default) { Passwords.verify(password, row.passwordHash, row.salt) }
        if (!ok) return Result.Failed(Field.PASSWORD, "That password isn't right")
        db.withTransaction {
            dao.deleteSports(handle)
            dao.deletePrefs(handle)
            dao.deleteSaved(handle)
            dao.deleteTrip(handle)
            dao.deleteTripItems(handle)
            dao.deleteAlerts(handle)
            dao.deleteTxns(handle)
            dao.deleteNotifications(handle)
            dao.deleteVerification(handle)
            dao.deleteBadges(handle)
            dao.deleteMatches(handle)
            dao.deleteCloset(handle)
            dao.deleteTasks(handle)
            dao.pauseListings(handle)
            db.discovery().deleteProfile(handle)
            db.discovery().person(handle)?.let {
                db.discovery().upsertPerson(it.copy(name = "Former member", area = ""))
            }
            dao.delete(handle)
        }
        session.signOut()
        return Result.Ok
    }

    // ============================================================ helpers

    /** `@firstname_l`, with a number on the end if that one is taken. */
    private suspend fun freeHandle(name: String): String {
        val parts = cleanName(name).lowercase(Locale.ENGLISH).split(' ').filter { it.isNotBlank() }
        val first = parts.firstOrNull()?.filter { it in 'a'..'z' || it in '0'..'9' }?.ifEmpty { null } ?: "member"
        val initial = parts.drop(1).lastOrNull()?.firstOrNull { it in 'a'..'z' }?.toString()
        val base = "@" + first.take(16) + (initial?.let { "_$it" } ?: "")
        var candidate = base
        var n = 2
        while (dao.byHandle(candidate) != null || db.discovery().personExists(candidate) > 0) {
            candidate = base + n
            n++
        }
        return candidate
    }

    private fun cleanName(name: String): String =
        name.trim().split(Regex("\\s+")).joinToString(" ") { part -> part.replaceFirstChar { it.titlecase(Locale.ENGLISH) } }

    private fun AccountEntity.toAccount(people: Map<String, Person>) = Account(
        handle = handle,
        email = email,
        phone = phone,
        role = role,
        person = people[handle] ?: Person(handle.removePrefix("@"), handle, "", TrustTier.BRONZE, 0.0, 0, ""),
        lastLoginAt = lastLoginAt,
    )
}
