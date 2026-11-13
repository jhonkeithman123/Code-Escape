package codeescape.android.adapter

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import codeescape.core.port.NarrationCallback
import codeescape.core.port.NarrationPort
import java.util.Locale

/** Best-effort Android TTS adapter with lifecycle-aware stop and optional utterance progress hooks. */
class AndroidNarrationAdapter(context: Context) : NarrationPort {
    private companion object {
        const val STORY_UTTERANCE_ID = "codeescape_story"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var speaking = false
    @Volatile private var pendingComplete: (() -> Unit)? = null
    var onStart: (() -> Unit)? = null
    var onDone: (() -> Unit)? = null
    /** Best-effort word/range progress (API 26+ provides range callbacks on some engines). */
    var onRange: ((start: Int, end: Int) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    try { tts?.language = Locale.US } catch (_: Exception) {}
                    attachProgressListener()
                }
            }
        } catch (_: Exception) {
            ready = false
        }
    }

    private fun attachProgressListener() {
        try {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (!isStoryUtterance(utteranceId)) return
                    speaking = true
                    mainHandler.post { try { onStart?.invoke() } catch (_: Exception) {} }
                }

                override fun onDone(utteranceId: String?) {
                    if (!isStoryUtterance(utteranceId)) return
                    speaking = false
                    fireDoneOnMain()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (!isStoryUtterance(utteranceId)) return
                    speaking = false
                    fireDoneOnMain()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (!isStoryUtterance(utteranceId)) return
                    speaking = false
                    fireDoneOnMain()
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (!isStoryUtterance(utteranceId)) return
                    val s = start
                    val e = end.coerceAtLeast(start)
                    mainHandler.post { try { onRange?.invoke(s, e) } catch (_: Exception) {} }
                }
            })
        } catch (_: Exception) {}
    }

    private fun isStoryUtterance(utteranceId: String?): Boolean =
        utteranceId == null || utteranceId == STORY_UTTERANCE_ID

    private fun fireDoneOnMain() {
        mainHandler.post { fireDone() }
    }

    private fun fireDone() {
        val cb = pendingComplete
        pendingComplete = null
        try { onDone?.invoke() } catch (_: Exception) {}
        try { cb?.invoke() } catch (_: Exception) {}
    }

    override fun isSupported(): Boolean = ready

    override fun speakStory(text: String?) {
        speakStory(text, null as NarrationCallback?)
    }

    override fun speakStory(text: String?, callback: NarrationCallback?) {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) {
            try { callback?.onComplete() } catch (_: Exception) {}
            return
        }
        if (!ready) {
            // Best-effort: no TTS engine available; do not block flow.
            try { callback?.onComplete() } catch (_: Exception) {}
            return
        }
        try {
            stopInternal(clearPending = true)
            pendingComplete = { try { callback?.onComplete() } catch (_: Exception) {} }
            speaking = true
            val params = Bundle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(t, TextToSpeech.QUEUE_FLUSH, params, STORY_UTTERANCE_ID)
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(t, TextToSpeech.QUEUE_FLUSH, hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to STORY_UTTERANCE_ID))
            }
        } catch (_: Exception) {
            speaking = false
            val cb = pendingComplete
            pendingComplete = null
            try { cb?.invoke() } catch (_: Exception) {}
        }
    }

    override fun stop() { stopInternal(clearPending = true) }

    private fun stopInternal(clearPending: Boolean) {
        speaking = false
        if (clearPending) pendingComplete = null
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun isSpeaking(): Boolean = speaking

    /** Call on pause/background; keeps engine for quick resume of future narration. */
    fun pauseForLifecycle() { stop() }

    fun shutdown() {
        pendingComplete = null
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        ready = false
        speaking = false
    }
}