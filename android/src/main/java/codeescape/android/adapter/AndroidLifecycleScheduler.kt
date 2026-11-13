package codeescape.android.adapter

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Lightweight Android scheduler/lifecycle scaffolding aligned with UI timing needs.
 * Ties delayed/runnable work to lifecycle to avoid leaks during pause/destroy.
 * Source-level parity aid for cross-frontend timer/ownership alignment (no core scheduler port).
 */
class AndroidLifecycleScheduler : DefaultLifecycleObserver {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var paused = false
    private val pending = linkedSetOf<Runnable>()

    fun post(delayMs: Long = 0L, block: () -> Unit): Runnable {
        lateinit var r: Runnable
        r = Runnable {
            synchronized(pending) { pending.remove(r) }
            if (!paused) block()
        }
        synchronized(pending) { pending.add(r) }
        if (delayMs <= 0L) handler.post(r) else handler.postDelayed(r, delayMs)
        return r
    }

    fun postRepeating(intervalMs: Long, initialDelayMs: Long = intervalMs, block: () -> Unit): Runnable {
        lateinit var r: Runnable
        r = object : Runnable {
            override fun run() {
                synchronized(pending) { pending.remove(r) }
                if (paused) return
                block()
                if (!paused) {
                    synchronized(pending) { pending.add(this) }
                    handler.postDelayed(this, intervalMs.coerceAtLeast(1L))
                }
            }
        }
        synchronized(pending) { pending.add(r) }
        handler.postDelayed(r, initialDelayMs.coerceAtLeast(0L))
        return r
    }

    fun cancel(runnable: Runnable?) {
        if (runnable != null) {
            handler.removeCallbacks(runnable)
            synchronized(pending) { pending.remove(runnable) }
        }
    }

    fun cancelAll() {
        handler.removeCallbacksAndMessages(null)
        synchronized(pending) { pending.clear() }
    }

    fun setPaused(paused: Boolean) {
        this.paused = paused
    }

    override fun onPause(owner: LifecycleOwner) {
        setPaused(true)
    }

    override fun onResume(owner: LifecycleOwner) {
        setPaused(false)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        cancelAll()
    }
}