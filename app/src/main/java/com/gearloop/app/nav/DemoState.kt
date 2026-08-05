package com.gearloop.app.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The state behind a live demonstration, which is not the same thing as app state.
 *
 * Two problems this solves, both of which only show up in a room:
 *
 * **Nobody should have to narrate from memory.** A flow is a fixed sequence with
 * a point to make at each stop. [Catalogue] already knows the sequence and the
 * points; this tracks where in it you are, so the app can put the sentence on
 * screen instead of leaving it to whoever is holding the phone.
 *
 * **The second walkthrough should start where the first one did.** Every screen
 * now saves its state properly, which is right for a real app and wrong for a
 * demo — the fairness slider stays wherever the last person dragged it. Bumping
 * [generation] remounts the whole graph and clears all of it.
 */
class DemoState(
    activeFlowId: String? = null,
    step: Int = 0,
    generation: Int = 0,
) {
    var activeFlowId by mutableStateOf(activeFlowId)
        private set

    var step by mutableIntStateOf(step)
        private set

    /** Changing this remounts the nav graph, resetting every screen. */
    var generation by mutableIntStateOf(generation)
        private set

    val flow: Flow? get() = activeFlowId?.let { Catalogue.flowById(it) }

    val currentStep: FlowStep? get() = flow?.steps?.getOrNull(step)

    val hasPrevious: Boolean get() = flow != null && step > 0
    val hasNext: Boolean get() = flow?.let { step < it.size - 1 } == true

    fun start(flow: Flow) {
        activeFlowId = flow.id
        step = 0
    }

    fun exit() {
        activeFlowId = null
        step = 0
    }

    /** Keeps the bar honest when someone navigates by hand mid-flow. */
    fun syncTo(route: String) {
        val i = flow?.indexOf(route) ?: return
        if (i >= 0) step = i
    }

    fun moveTo(index: Int) {
        val f = flow ?: return
        step = index.coerceIn(0, f.size - 1)
    }

    fun reset() {
        generation++
    }

    companion object {
        val Saver: Saver<DemoState, List<Any>> = Saver(
            save = { listOf(it.activeFlowId ?: "", it.step, it.generation) },
            restore = {
                DemoState(
                    activeFlowId = (it[0] as String).ifEmpty { null },
                    step = it[1] as Int,
                    generation = it[2] as Int,
                )
            },
        )
    }
}

val LocalDemoState = staticCompositionLocalOf { DemoState() }
