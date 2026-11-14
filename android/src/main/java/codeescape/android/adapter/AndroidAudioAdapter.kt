package codeescape.android.adapter

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import codeescape.core.port.AudioPort

/** Best-effort Android audio adapter. Maps core track IDs to legacy music assets and supports lifecycle pause/resume. */
class AndroidAudioAdapter(private val context: Context) : AudioPort {
    private var music: MediaPlayer? = null
    private var enabled = true
    private var muted = false
    private var volume = 0.45f
    private var currentTrack: String? = null
    private var currentLoop: Boolean = true
    private var pausedByLifecycle = false
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val sfxCache = mutableMapOf<String, Int>()

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) stopMusic()
    }

    override fun isEnabled(): Boolean = enabled

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        if (muted) stopMusic()
    }

    override fun isMuted(): Boolean = muted

    override fun setVolume(volume01: Float) {
        volume = volume01.coerceIn(0f, 1f)
        try { music?.setVolume(volume, volume) } catch (_: Exception) {}
    }

    override fun getVolume(): Float = volume

    override fun playMusic(trackId: String?, loop: Boolean) {
        if (trackId.isNullOrBlank()) { stopMusic(); return }
        // Keep an already-playing identical track running (avoid restarts on lifecycle/screen re-entry).
        if (trackId == currentTrack && loop == currentLoop && music != null && isMusicActive()) return
        currentTrack = trackId
        currentLoop = loop
        pausedByLifecycle = false
        if (!enabled || muted) return
        stopMusicInternal(releaseOnly = true)
        try {
            val resId = context.resources.getIdentifier(trackId.substringBeforeLast('.'), "raw", context.packageName)
            music = when {
                resId != 0 -> MediaPlayer.create(context, resId)
                else -> openFromAssets(trackId)
            }
            music?.isLooping = loop
            music?.setVolume(volume, volume)
            music?.setOnPreparedListener { mp -> try { if (enabled) mp.start() } catch (_: Exception) {} }
            if (music?.isPlaying != true) {
                // MediaPlayer.create returns prepared; asset path may need prepare/start fallback
                try { music?.start() } catch (_: Exception) {
                    try { music?.prepare(); music?.start() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {
            stopMusicInternal(releaseOnly = true)
        }
    }

    private fun openFromAssets(trackId: String): MediaPlayer? {
        val base = trackId.substringBeforeLast('.')
        val candidates = listOf(trackId, "music/$trackId", "audio/$trackId", "$base.wav", "music/$base.wav", "audio/$base.wav")
        for (path in candidates) {
            try {
                val afd = context.assets.openFd(path)
                return MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    override fun stopMusic() {
        currentTrack = null
        pausedByLifecycle = false
        stopMusicInternal(releaseOnly = false)
    }

    private fun stopMusicInternal(releaseOnly: Boolean) {
        try { if (!releaseOnly) music?.stop() } catch (_: Exception) {}
        try { music?.release() } catch (_: Exception) {}
        music = null
    }

    override fun playFeedback(correct: Boolean) {
        if (!enabled || muted) return
        // Best-effort: try short SFX asset names if bundled; otherwise system tone.
        val names = if (correct) {
            listOf("correct.wav", "success.wav", "correct.mp3", "sfx/correct.wav", "music/correct.wav")
        } else {
            listOf("wrong.wav", "error.wav", "wrong.mp3", "sfx/wrong.wav", "music/wrong.wav")
        }
        for (n in names) {
            if (playSfxAsset(n)) return
        }
        var toneGen: android.media.ToneGenerator? = null
        try {
            val tone = if (correct) android.media.ToneGenerator.TONE_PROP_ACK else android.media.ToneGenerator.TONE_PROP_NACK
            toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, (volume * 80).toInt().coerceIn(20, 100))
            toneGen.startTone(tone, 120)
        } catch (_: Exception) {
        } finally {
            try { toneGen?.release() } catch (_: Exception) {}
        }
    }

    private fun playSfxAsset(name: String): Boolean {
        return try {
            val base = name.substringBeforeLast('.', name)
            val ext = if (name.contains('.')) name.substringAfterLast('.') else "wav"
            val variants = linkedSetOf(
                name, "sfx/$name", "music/$name", "audio/$name", "sound/$name",
                "$base.wav", "sfx/$base.wav", "music/$base.wav", "audio/$base.wav",
                "$base.mp3", "sfx/$base.mp3", "music/$base.mp3", "audio/$base.mp3",
                "$base.ogg", "sfx/$base.ogg", "music/$base.ogg", "audio/$base.ogg"
            )
            if (ext !in listOf("wav", "mp3", "ogg")) variants.add("$base.$ext")
            val cacheKey = variants.joinToString("|")
            val id = sfxCache[cacheKey] ?: run {
                var sid = 0
                for (path in variants) {
                    try {
                        val afd = context.assets.openFd(path)
                        sid = soundPool.load(afd, 1)
                        afd.close()
                        if (sid != 0) break
                    } catch (_: Exception) { }
                }
                if (sid != 0) sfxCache[cacheKey] = sid
                sid
            }
            if (id != 0) { soundPool.play(id, volume, volume, 1, 0, 1f); true } else false
        } catch (_: Exception) { false }
    }

    /** Pause due to lifecycle/app background (distinct from user-disabled audio). */
    fun pauseMusic() {
        try {
            if (music?.isPlaying == true) {
                music?.pause()
                pausedByLifecycle = true
            }
        } catch (_: Exception) {}
    }

    fun resumeMusic() {
        try {
            if (enabled && !muted && pausedByLifecycle && music != null && music?.isPlaying == false) {
                music?.start()
            }
            pausedByLifecycle = false
        } catch (_: Exception) {
            // best-effort recovery: restart intended track
            val t = currentTrack
            if (enabled && !muted && t != null) playMusic(t, currentLoop)
        }
    }

    /** True while the music player exists and is producing sound (used by lifecycle re-entry logic). */
    fun isMusicActive(): Boolean {
        return try { music?.isPlaying == true } catch (_: Exception) { false }
    }

    fun release() {
        stopMusic()
        try { soundPool.release() } catch (_: Exception) {}
        sfxCache.clear()
    }
}