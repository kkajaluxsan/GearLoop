package com.gearloop.app

import android.app.Application
import android.content.Context
import com.gearloop.app.data.db.GearDatabase
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
import kotlinx.coroutines.runBlocking

/**
 * Builds the app's object graph once, by hand.
 *
 * Manual dependency injection, the pattern the Android architecture guide
 * describes for an app this size: one database, one store, four repositories —
 * one per member's workload — and nothing that needs a framework to assemble.
 */
class AppContainer(context: Context, val db: GearDatabase = GearDatabase.build(context)) {

    /** Lives as long as the process; owns the hot flows the screens read. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val store = GearStore(db, scope)
    val session = SessionStore(db.session(), scope)

    val discovery = DiscoveryRepository(db, store, session, scope)
    val money = MoneyRepository(db, store, session, scope)
    val evidence = EvidenceRepository(db, store, session, money, scope)
    val exchange = ExchangeRepository(db, store, session, money, scope)

    val demo = DemoData(db, store, session, money, evidence)
    val toaster = Toaster(scope)
}

class GearLoopApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Seeds the database on first launch and reads it into memory, so the
        // first frame of whatever screen opens already has its data. A few
        // dozen rows: tens of milliseconds, once per process.
        runBlocking { container.demo.start() }
    }
}

/** The container, from anywhere that has a context. */
val Context.container: AppContainer
    get() = (applicationContext as GearLoopApplication).container
