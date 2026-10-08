package com.gearloop.app.ui.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gearloop.app.ui.components.Toast
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Confirmation after a write.
 *
 * Every create, update and delete in the app says what it did, in a toast — the
 * component the design system already had and nothing used. A delete always
 * offers Undo, because a destructive action that cannot be taken back has to be
 * confirmed first, and a confirm dialog on every row is worse than an undo.
 */
data class ToastEvent(
    val id: Long,
    val text: String,
    val undoLabel: String?,
    val onUndo: (suspend () -> Unit)?,
)

class Toaster(private val scope: CoroutineScope) {
    private val ids = AtomicLong()
    private val state = MutableStateFlow<ToastEvent?>(null)
    val current: StateFlow<ToastEvent?> = state.asStateFlow()

    /** How long a toast stays up. The UI tests shorten it so a toast never sits over the next tap. */
    @Volatile var displayMs: Long = 3_000
    @Volatile var undoDisplayMs: Long = 5_000

    fun show(text: String, undoLabel: String? = null, onUndo: (suspend () -> Unit)? = null) {
        val event = ToastEvent(ids.incrementAndGet(), text, if (onUndo != null) undoLabel ?: "Undo" else null, onUndo)
        state.value = event
        // The toast times out here, not in the screen that shows it. Timed by the
        // host, a toast whose screen closed first never expired, and came back,
        // Undo still live, the next time the app opened (D-18).
        scope.launch {
            delay(if (onUndo != null) undoDisplayMs else displayMs)
            dismiss(event.id)
        }
    }

    fun dismiss(id: Long) {
        if (state.value?.id == id) state.value = null
    }

    fun undo(event: ToastEvent) {
        dismiss(event.id)
        event.onUndo?.let { scope.launch { it() } }
    }
}

/**
 * Where toasts appear: low on the screen, clear of a tab bar or a sticky footer.
 * Announced politely to a screen reader, so the confirmation reaches someone who
 * cannot see it.
 */
@Composable
fun BoxScope.ToastHost(toaster: Toaster) {
    val event by toaster.current.collectAsStateWithLifecycle()
    val shown = event ?: return
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = Space.screen)
            .padding(bottom = Metrics.tabBar + Space.section + Space.l)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Toast(
            text = shown.text,
            undoLabel = shown.undoLabel,
            onUndo = if (shown.onUndo != null) ({ toaster.undo(shown) }) else null,
        )
    }
}
