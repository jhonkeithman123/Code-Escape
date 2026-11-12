package codeescape.android.adapter

import android.content.Context
import android.speech.tts.TextToSpeech
import codeescape.core.port.NarrationPort
import java.util.Locale

/** Best-effort Android TTS adapter with lifecycle-aware stop and optional utterance progress hooks. */
class AndroidNarrationAdapter(context: Context) : NarrationPort {
    private companion object {
        const val STORY_UTTERANCE_ID = "codeescape_story"
    }

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var speaking = false
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
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { speaking = true; try { onStart?.invoke() } catch (_: Exception) {} }
                override fun onDone(utteranceId: String?) {
                    // only the latest story utterance drives state/callbacks
                    if (utteranceId == null || utteranceId == STORY_UTTERANCE_ID) {
                        speaking = false
                        try { onDone?.invoke() } catch (_: Exception) {}
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == null || utteranceId == STORY_UTTERANCE_ID) {
                        speaking = false
                        try { onDone?.invoke() } catch (_: Exception) {}
                    }
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId == null || utteranceId == STORY_UTTERANCE_ID) {
                        speaking = false
                        try { onDone?.invoke() } catch (_: Exception) {}
                    }
                }
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (utteranceId == null || utteranceId == STORY_UTTERANCE_ID) {
                        try { onRange?.invoke(start, end) } catch (_: Exception) {}
                    }
                }
            })
        } catch (_: Exception) {}
    }

    override fun isSupported(): Boolean = ready

    override fun speakStory(text: String?) {
        val t = text?.trim().orEmpty()
        if (t.isEmpty() || !ready) return
        try {
            stop()
            speaking = true
            // QUEUE_FLUSH for parity with screen narration transitions
            tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, STORY_UTTERANCE_ID)
        } catch (_: Exception) { speaking = false }
    }

    override fun stop() {
        speaking = false
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun isSpeaking(): Boolean = speaking

    /** Call on pause/background; keeps engine for quick resume of future narration. */
    fun pauseForLifecycle() { stop() }

    fun shutdown() {
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        ready = false
        speaking = false
    }
}