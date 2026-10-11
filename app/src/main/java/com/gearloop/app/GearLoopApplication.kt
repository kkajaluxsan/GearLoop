package com.gearloop.app

import android.app.Application
import android.content.Context
import com.gearloop.app.data.db.GearDatabase
import com.gearloop.app.data.repo.AccountRepository
import com.gearloop.app.data.repo.DemoData
import com.gearloop.app.data.repo.DiscoveryRepository
import com.gearloop.app.data.repo.EvidenceRepository
import com.gearloop.app.data.repo.ExchangeRepository
import com.gearloop.app.data.repo.GearStore
import com.gearloop.app.data.repo.MoneyRepository
import com.gearloop.app.data.repo.SessionStore
import com.gearloop.app.ui.feedback.Toaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Builds the app's object graph once, by hand.
 *
 * Manual dependency injection, the pattern the Android architecture guide
 * describes for an app this size: one database, one session, one store, the
 * account service and four repositories, and nothing that needs a framework to
 * assemble.
 */
class AppContainer(context: Context, val db: GearDatabase = GearDatabase.build(context)) {

    val appContext: Context = context.applicationContext

    /** Lives as long as the process; owns the hot flows the screens read. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val session = SessionStore(db.session(), scope)
    val store = GearStore(db, session, scope)

    val accounts = AccountRepository(db, store, session, scope)
    val discovery = DiscoveryRepository(db, store, session, scope)
    val money = MoneyRepository(db, store, session, scope)
    val evidence = EvidenceRepository(db, store, session, money, scope)
    val exchange = ExchangeRepository(db, store, session, money, scope)

    val demo = DemoData(db, store, session, money, evidence)
    val toaster = Toaster(scope)

    /**
     * The jobs a server would run on a schedule: a request the owner has not
     * answered within 24 hours expires and is refunded. Checked every minute
     * while the app is running, and once at launch.
     */
    fun startHousekeeping() {
        scope.launch {
            while (isActive) {
                runCatching { money.expireStaleRequests() }
                runCatching { evidence.closeLapsedReviews() }
                delay(60_000)
            }
        }
    }
}

class GearLoopApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Writes the sample marketplace on first launch and reads the database
        // into memory, so the first frame of whatever screen opens already has
        // its data. The splash screen covers the few tens of milliseconds.
        runBlocking { container.demo.start() }
        container.startHousekeeping()
    }
}

/** The container, from anywhere that has a context. */
val Context.container: AppContainer
    get() = (applicationContext as GearLoopApplication).container
