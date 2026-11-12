package codeescape.android.adapter

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Lightweight Android scheduler/lifecycle scaffolding aligned with UI timing needs.
 * Ties delayed/runnable work to lifecycle to avoid leaks during pause/destroy.
 */
class AndroidLifecycleScheduler : DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())

    fun post(delayMs: Long = 0L, block: () -> Unit): Runnable {
        val r = Runnable { block() }
        if (delayMs <= 0L) handler.post(r) else handler.postDelayed(r, delayMs)
        return r
    }

    fun cancel(runnable: Runnable?) {
        if (runnable != null) handler.removeCallbacks(runnable)
    }

    fun cancelAll() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause(owner: LifecycleOwner) {
        // Hook point for adapters that should pause; consumers may opt-in.
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancelAll()
    }
}