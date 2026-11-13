package codeescape.android.adapter

import android.content.Context
import codeescape.core.content.GameConfig
import codeescape.core.model.PlayerProfile
import codeescape.core.port.ProfileRepository
import org.json.JSONObject
import java.io.File
import java.util.Optional

/**
 * Best-effort Android storage adapter using app-private JSON (fresh Android storage).
 * Schema is kept core-compatible (username, profile fields, lifetime XP) without reusing desktop files.
 */
class AndroidProfileRepository(context: Context) : ProfileRepository {
    private val file = File(context.filesDir, "profiles.json")
    private val lock = Any()
    private val accounts = mutableMapOf<String, String>() // username -> password (plain, local-only best effort)
    private val profiles = mutableMapOf<String, PlayerProfile>()
    private var runSnapshotJson: JSONObject? = null

    init { load(); ensureDemoAccount() }

    private fun load() = synchronized(lock) {
        accounts.clear(); profiles.clear(); runSnapshotJson = null
        if (!file.isFile) return
        try {
            val root = JSONObject(file.readText())
            val users = root.optJSONObject("users") ?: JSONObject()
            val keys = users.keys()
            while (keys.hasNext()) {
                val u = keys.next()
                val o = users.optJSONObject(u) ?: continue
                accounts[u] = o.optString("password", "")
                val p = PlayerProfile(u, o.optString("email", ""))
                p.character = o.optString("character", "Boy")
                p.outfitVariant = o.optInt("outfitVariant", 0).coerceIn(0, 3)
                p.selectedTop = o.optInt("selectedTop", 1)
                p.selectedBottom = o.optInt("selectedBottom", 1)
                p.selectedShoes = o.optInt("selectedShoes", 1)
                p.selectedAccessory = o.optInt("selectedAccessory", 1)
                p.lifetimeXp = o.optInt("lifetimeXp", 0).coerceAtLeast(0)
                p.savedXp = o.optInt("savedXp", p.lifetimeXp).coerceAtLeast(0)
                profiles[u] = p
            }
            runSnapshotJson = root.optJSONObject("runSnapshot")
        } catch (_: Exception) { }
    }

    private fun persist() = synchronized(lock) {
        try {
            val users = JSONObject()
            for ((u, p) in profiles) {
                val o = JSONObject()
                o.put("password", accounts[u] ?: "")
                o.put("email", p.email ?: "")
                o.put("character", p.character ?: "Boy")
                o.put("outfitVariant", p.outfitVariant)
                o.put("selectedTop", p.selectedTop)
                o.put("selectedBottom", p.selectedBottom)
                o.put("selectedShoes", p.selectedShoes)
                o.put("selectedAccessory", p.selectedAccessory)
                o.put("lifetimeXp", p.lifetimeXp.coerceAtLeast(0))
                o.put("savedXp", p.savedXp.coerceAtLeast(0))
                users.put(u, o)
            }
            val root = JSONObject()
            root.put("users", users)
            runSnapshotJson?.let { root.put("runSnapshot", it) }
            file.writeText(root.toString())
        } catch (_: Exception) { }
    }

    private fun ensureDemoAccount() = synchronized(lock) {
        if (!accounts.containsKey("demo") || profiles["demo"] == null) {
            val demo = PlayerProfile("demo", "demo@codeescape.local")
            demo.character = "Boy"
            demo.outfitVariant = 0
            demo.selectedTop = 1; demo.selectedBottom = 1; demo.selectedShoes = 1; demo.selectedAccessory = 1
            demo.lifetimeXp = 0; demo.savedXp = 0
            accounts["demo"] = "demo"
            profiles["demo"] = demo
            persist()
        } else {
            // Ensure demo credentials remain available (non-destructive) if account exists but password missing.
            if (accounts["demo"].isNullOrEmpty()) { accounts["demo"] = "demo"; persist() }
        }
    }

    override fun findByUsername(username: String?): Optional<PlayerProfile> = synchronized(lock) {
        if (username == null) return Optional.empty()
        val p = profiles[username] ?: return Optional.empty()
        val copy = PlayerProfile(p.username, p.email)
        copy.character = p.character; copy.outfitVariant = p.outfitVariant
        copy.selectedTop = p.selectedTop; copy.selectedBottom = p.selectedBottom
        copy.selectedShoes = p.selectedShoes; copy.selectedAccessory = p.selectedAccessory
        copy.lifetimeXp = p.lifetimeXp; copy.savedXp = p.savedXp
        Optional.of(copy)
    }

    override fun authenticate(username: String?, password: String?): Boolean = synchronized(lock) {
        if (username == null) return false
        accounts[username] != null && accounts[username] == (password ?: "")
    }

    override fun saveAccount(username: String?, email: String?, password: String?, profile: PlayerProfile?) = synchronized(lock) {
        if (username == null) return
        accounts[username] = password ?: ""
        val p = profile ?: PlayerProfile(username, email)
        p.username = username
        p.email = email ?: p.email
        profiles[username] = p
        persist()
    }

    override fun saveProfile(profile: PlayerProfile?) = synchronized(lock) {
        if (profile?.username == null) return
        val p = profiles[profile.username] ?: PlayerProfile(profile.username, profile.email)
        p.username = profile.username; p.email = profile.email
        p.character = profile.character; p.outfitVariant = profile.outfitVariant
        p.selectedTop = profile.selectedTop; p.selectedBottom = profile.selectedBottom
        p.selectedShoes = profile.selectedShoes; p.selectedAccessory = profile.selectedAccessory
        p.lifetimeXp = profile.lifetimeXp; p.savedXp = profile.savedXp
        profiles[profile.username] = p
        persist()
    }

    override fun getLifetimeXp(username: String?): Int = synchronized(lock) {
        profiles[username]?.lifetimeXp?.coerceAtLeast(0) ?: 0
    }

    override fun commitAttemptXp(username: String?, earnedXp: Int) = synchronized(lock) {
        if (username == null) return
        val p = profiles[username] ?: PlayerProfile(username, "").also { profiles[username] = it }
        val total = (p.lifetimeXp.coerceAtLeast(0) + earnedXp.coerceAtLeast(0)).coerceAtLeast(0)
        p.lifetimeXp = total; p.savedXp = total
        persist()
    }

    // --- best-effort Android session/run restoration (app-private, core-compatible values only) ---
    fun saveRunSnapshot(username: String?, snapshot: RunSnapshot?) = synchronized(lock) {
        try {
            if (username.isNullOrBlank() || snapshot == null) {
                runSnapshotJson = null
            } else {
                runSnapshotJson = snapshot.toJson().put("username", username)
            }
            persist()
        } catch (_: Exception) { }
    }

    fun loadRunSnapshot(expectedUsername: String?): RunSnapshot? = synchronized(lock) {
        if (expectedUsername.isNullOrBlank()) return null
        val o = runSnapshotJson ?: return null
        return try {
            if (o.optString("username", "") != expectedUsername) null else RunSnapshot.fromJson(o)
        } catch (_: Exception) { null }
    }

    fun clearRunSnapshot() = synchronized(lock) { saveRunSnapshot(null, null) }

    data class RunSnapshot(
        val screen: String,
        val level: Int,
        val questionIndex: Int,
        val hearts: Int,
        val xp: Int,
        val correctAnswers: Int,
        val savedXpBeforeRun: Int,
        val showZeroXpAfterCancelledRun: Boolean,
        val sessionCommitted: Boolean,
        val pendingStoryLevel: Int,
        val questionTimeLeft: Int,
        val syntaxInput: String,
        val answered: Boolean,
        // best-effort continuity fields (backward-compatible defaults for older snapshots)
        val runningReason: String = "",
        val endingChestOpened: Boolean = false,
        val endingTreasureDialogShown: Boolean = false,
        val endingXpDialogShown: Boolean = false,
        val endingFinalXp: Int = 0,
        val resultMessage: String = "",
        val feedbackHeading: String = "",
        val feedbackBig: String = "",
        val feedbackDetail: String = "",
        val lastAnswerCorrect: Boolean? = null,
        val exitDialogShown: Boolean = false,
        val loadingTarget: String = "LOGIN"
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("screen", screen)
            .put("level", level)
            .put("questionIndex", questionIndex)
            .put("hearts", hearts)
            .put("xp", xp)
            .put("correctAnswers", correctAnswers)
            .put("savedXpBeforeRun", savedXpBeforeRun)
            .put("showZeroXpAfterCancelledRun", showZeroXpAfterCancelledRun)
            .put("sessionCommitted", sessionCommitted)
            .put("pendingStoryLevel", pendingStoryLevel)
            .put("questionTimeLeft", questionTimeLeft)
            .put("syntaxInput", syntaxInput)
            .put("answered", answered)
            .put("runningReason", runningReason)
            .put("endingChestOpened", endingChestOpened)
            .put("endingTreasureDialogShown", endingTreasureDialogShown)
            .put("endingXpDialogShown", endingXpDialogShown)
            .put("endingFinalXp", endingFinalXp)
            .put("resultMessage", resultMessage)
            .put("feedbackHeading", feedbackHeading)
            .put("feedbackBig", feedbackBig)
            .put("feedbackDetail", feedbackDetail)
            .put("hasLastAnswerCorrect", lastAnswerCorrect != null)
            .put("lastAnswerCorrect", lastAnswerCorrect ?: false)
            .put("exitDialogShown", exitDialogShown)
            .put("loadingTarget", loadingTarget)
        companion object {
            fun fromJson(o: JSONObject): RunSnapshot = RunSnapshot(
                screen = o.optString("screen", "DASHBOARD"),
                level = o.optInt("level", 1),
                questionIndex = o.optInt("questionIndex", 0),
                hearts = o.optInt("hearts", GameConfig.STARTING_HEARTS),
                xp = o.optInt("xp", 0),
                correctAnswers = o.optInt("correctAnswers", 0),
                savedXpBeforeRun = o.optInt("savedXpBeforeRun", 0),
                showZeroXpAfterCancelledRun = o.optBoolean("showZeroXpAfterCancelledRun", false),
                sessionCommitted = o.optBoolean("sessionCommitted", false),
                pendingStoryLevel = o.optInt("pendingStoryLevel", 1),
                questionTimeLeft = o.optInt("questionTimeLeft", GameConfig.MCQ_SECONDS),
                syntaxInput = o.optString("syntaxInput", ""),
                answered = o.optBoolean("answered", false),
                runningReason = o.optString("runningReason", ""),
                endingChestOpened = o.optBoolean("endingChestOpened", false),
                endingTreasureDialogShown = o.optBoolean("endingTreasureDialogShown", false),
                endingXpDialogShown = o.optBoolean("endingXpDialogShown", false),
                endingFinalXp = o.optInt("endingFinalXp", 0),
                resultMessage = o.optString("resultMessage", ""),
                feedbackHeading = o.optString("feedbackHeading", ""),
                feedbackBig = o.optString("feedbackBig", ""),
                feedbackDetail = o.optString("feedbackDetail", ""),
                lastAnswerCorrect = if (o.optBoolean("hasLastAnswerCorrect", false)) o.optBoolean("lastAnswerCorrect", false) else null,
                exitDialogShown = o.optBoolean("exitDialogShown", false),
                loadingTarget = o.optString("loadingTarget", "LOGIN")
            )
        }
    }
}