package com.jumpadventure.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Fully procedural audio engine for Jump Adventure — no bundled audio files.
 *
 * SFX are synthesized as small multi-partial envelopes (soft, non-harsh), each
 * with a per-cue cooldown so rapid pickups can never stack into a harsh burst.
 * Music is a layered, seamless loop (bass + arpeggiated melody + pad) with a
 * per-world key, kept at a calm level under the SFX.
 *
 * Music and SFX have independent enable flags ([musicEnabled] / [soundEnabled])
 * and the whole engine releases its threads and audio focus in [release].
 */
class SoundManager(val context: Context) {
    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var musicEnabled: Boolean = true
        set(value) {
            val changed = field != value
            field = value
            if (!value) {
                stopMusic()
            } else if (changed && !released.get()) {
                startMusic(lastRequestedWorldId)
            }
        }

    private val released = AtomicBoolean(false)

    private val sfxExecutor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "JumpAdventure-SFX").apply { isDaemon = true }
    }

    /** Per-cue cooldown timestamps (ms) so identical SFX never overlap themselves. */
    private val lastPlayedMs = ConcurrentHashMap<String, Long>()

    @Volatile
    private var isMusicRunning = false

    @Volatile
    private var currentMusicWorldId = -1

    @Volatile
    private var lastRequestedWorldId = 1

    @Volatile
    private var musicThread: Thread? = null

    // ------------------------------------------------------------------
    // Synthesis core
    // ------------------------------------------------------------------

    private fun submitSfx(cue: String, minIntervalMs: Long = 0L, block: () -> Unit) {
        if (!soundEnabled || released.get()) return
        if (minIntervalMs > 0L) {
            val now = System.currentTimeMillis()
            val last = lastPlayedMs[cue] ?: 0L
            if (now - last < minIntervalMs) return
            lastPlayedMs[cue] = now
        }
        runCatching {
            sfxExecutor.execute {
                if (soundEnabled && !released.get()) {
                    runCatching(block).onFailure { Log.w(TAG, "SFX playback failed", it) }
                }
            }
        }
    }

    /**
     * Renders a short synthesized clip: a list of [Segment]s played back to back.
     * Each segment sweeps linearly from [f0] to [f1] with an ADSR-ish envelope
     * and a second harmonic for warmth. Everything is normalized to [gain].
     */
    private class Segment(val f0: Double, val f1: Double, val durationMs: Int, val gain: Float)

    private fun playSegmentsBlocking(segments: List<Segment>, sampleRate: Int = 22050) {
        if (released.get()) return
        var phase = 0.0
        var phase2 = 0.0
        val chunks = mutableListOf<ByteArray>()
        var totalSamples = 0
        for (seg in segments) {
            val duration = seg.durationMs.coerceAtLeast(15)
            val n = duration * sampleRate / 1000
            val buf = ByteArray(n * 2)
            for (i in 0 until n) {
                if (Thread.currentThread().isInterrupted) return
                val t = i.toDouble() / n
                val f = seg.f0 + (seg.f1 - seg.f0) * t
                phase += 2.0 * PI * f / sampleRate
                phase2 += 2.0 * PI * (f * 2.0) / sampleRate
                val attack = (t / 0.12).coerceIn(0.0, 1.0)
                val release = ((1.0 - t) / 0.18).coerceIn(0.0, 1.0)
                val env = min(attack, release)
                val value = (sin(phase) * 0.8 + sin(phase2) * 0.2) * env * seg.gain
                val shortValue = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                buf[i * 2] = (shortValue.toInt() and 0xFF).toByte()
                buf[i * 2 + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
            }
            chunks.add(buf)
            totalSamples += n
        }
        val all = ByteArray(totalSamples * 2)
        var offset = 0
        for (c in chunks) {
            System.arraycopy(c, 0, all, offset, c.size)
            offset += c.size
        }
        val totalMs = segments.sumOf { it.durationMs }
        val track = buildTrack(sampleRate, all.size) ?: return
        try {
            track.write(all, 0, all.size)
            track.play()
            Thread.sleep(totalMs.toLong() + 15L)
        } finally {
            releaseTrack(track)
        }
    }

    private fun buildTrack(sampleRate: Int, bufferSizeBytes: Int): AudioTrack? {
        if (released.get() || bufferSizeBytes <= 0) return null
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.onFailure { Log.w(TAG, "Could not create AudioTrack", it) }.getOrNull()
    }

    private fun releaseTrack(track: AudioTrack) {
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
        }
        runCatching { track.flush() }
        runCatching { track.release() }
    }

    // ------------------------------------------------------------------
    // SFX library (all original synthesized cues, soft and normalized)
    // ------------------------------------------------------------------

    /** Springy rising chirp. */
    fun playJump() = submitSfx("jump", 70) {
        playSegmentsBlocking(listOf(Segment(300.0, 660.0, 130, 0.30f)))
    }

    /** Soft low thud for landing; cooldown keeps rapid bounces pleasant. */
    fun playLand() = submitSfx("land", 60) {
        playSegmentsBlocking(listOf(Segment(170.0, 70.0, 110, 0.32f)))
    }

    /** Bright two-note coin chime. */
    fun playCoin() = submitSfx("coin", 45) {
        playSegmentsBlocking(listOf(
            Segment(987.77, 987.77, 70, 0.28f),
            Segment(1318.51, 1318.51, 110, 0.26f)
        ))
    }

    fun playStar() = submitSfx("star", 60) {
        playSegmentsBlocking(listOf(Segment(523.25, 1046.50, 170, 0.28f)))
    }

    /** Gentle UI click. */
    fun playButtonClick() = submitSfx("click", 40) {
        playSegmentsBlocking(listOf(Segment(620.0, 480.0, 45, 0.22f)))
    }

    /** Dissonant low impact for obstacle hits. */
    fun playHit() = submitSfx("hit", 120) {
        playSegmentsBlocking(listOf(Segment(210.0, 75.0, 200, 0.34f)))
    }

    /** Descending three-note cue for game over. */
    fun playGameOver() = submitSfx("gameover", 0) {
        playSegmentsBlocking(listOf(
            Segment(392.0, 370.0, 180, 0.30f),
            Segment(311.13, 293.66, 180, 0.30f),
            Segment(261.63, 130.81, 320, 0.32f)
        ))
    }

    fun playPowerUp() = submitSfx("powerup", 60) {
        playSegmentsBlocking(listOf(Segment(440.0, 880.0, 150, 0.28f)))
    }

    /** Distinct activation stingers per power-up type. */
    fun playPowerUpActivation(type: String) {
        when (type) {
            "MAGNET" -> submitSfx("magnet", 80) {
                playSegmentsBlocking(listOf(
                    Segment(280.0, 280.0, 90, 0.26f),
                    Segment(420.0, 420.0, 90, 0.26f),
                    Segment(560.0, 560.0, 120, 0.26f)
                ))
            }
            "SHIELD" -> submitSfx("shield", 80) {
                playSegmentsBlocking(listOf(
                    Segment(523.25, 784.0, 90, 0.26f),
                    Segment(784.0, 1046.5, 130, 0.26f)
                ))
            }
            "SPEED" -> submitSfx("speed", 80) {
                playSegmentsBlocking(listOf(
                    Segment(500.0, 700.0, 60, 0.26f),
                    Segment(700.0, 950.0, 60, 0.26f),
                    Segment(950.0, 1250.0, 90, 0.26f)
                ))
            }
            "HIGH_JUMP" -> submitSfx("highjump", 80) {
                playSegmentsBlocking(listOf(
                    Segment(320.0, 480.0, 70, 0.26f),
                    Segment(480.0, 960.0, 150, 0.28f)
                ))
            }
            "POWER" -> submitSfx("power", 80) {
                playSegmentsBlocking(listOf(
                    Segment(300.0, 600.0, 100, 0.28f),
                    Segment(600.0, 950.0, 140, 0.28f)
                ))
            }
            else -> submitSfx("generic", 60) {
                playSegmentsBlocking(listOf(Segment(440.0, 880.0, 150, 0.28f)))
            }
        }
    }

    fun playShieldBreak() = submitSfx("shieldbreak", 100) {
        playSegmentsBlocking(listOf(Segment(300.0, 110.0, 160, 0.30f)))
    }

    fun playLevelComplete() = submitSfx("levelcomplete", 0) {
        playSegmentsBlocking(listOf(
            Segment(784.0, 1046.5, 120, 0.28f),
            Segment(1046.5, 1318.5, 160, 0.26f)
        ))
    }

    fun playStarImpact(isCenter: Boolean = false) {
        submitSfx("starimpact", 50) {
            val sampleRate = 22050
            val durationMs = if (isCenter) 220 else 160
            val numSamples = durationMs * sampleRate / 1000
            val generated = ByteArray(numSamples * 2)
            val baseFreq = if (isCenter) 200.0 else 300.0
            val chimeFreq = if (isCenter) 1318.51 else 1046.50
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val bodyEnv = exp(-6.0 * t)
                val chimeEnv = exp(-3.5 * t)
                val bodyWave = sin(2.0 * PI * (baseFreq + 140.0 * (1.0 - t)) * (i.toDouble() / sampleRate)) * bodyEnv
                val chimeWave = sin(2.0 * PI * (chimeFreq + 180.0 * t) * (i.toDouble() / sampleRate)) * chimeEnv * 0.65
                val sampleVal = ((bodyWave + chimeWave) * 0.7).coerceIn(-1.0, 1.0)
                val shortValue = (sampleVal * Short.MAX_VALUE * 0.55).toInt().toShort()
                generated[i * 2] = (shortValue.toInt() and 0xFF).toByte()
                generated[i * 2 + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
            }
            val track = buildTrack(sampleRate, generated.size) ?: return@submitSfx
            try {
                track.write(generated, 0, generated.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 15L)
            } finally {
                releaseTrack(track)
            }
        }
    }

    fun playVictory() {
        submitSfx("victory", 0) {
            val notes = arrayOf(
                Triple(523.25, 100, 587.33),
                Triple(659.25, 100, 698.46),
                Triple(783.99, 120, 880.00),
                Triple(1046.50, 140, 1174.66),
                Triple(1318.51, 150, 1396.91),
                Triple(1567.98, 360, 1567.98)
            )
            val segments = notes.map { (start, duration, end) -> Segment(start, end, duration, 0.30f) }
            playSegmentsBlocking(segments)
            if (Thread.currentThread().isInterrupted) return@submitSfx
        }
    }

    // ------------------------------------------------------------------
    // Background music: layered seamless loop (bass + melody + pad)
    // ------------------------------------------------------------------

    private fun resolveRequestedWorld(worldId: Int): Int {
        if (worldId > 1) return worldId
        val savedLevel = context
            .getSharedPreferences("jump_adventure_save", Context.MODE_PRIVATE)
            .getInt("current_level", 1)
            .coerceAtLeast(1)
        return ((savedLevel - 1) / 25) + 1
    }

    @Synchronized
    fun startMusic(worldId: Int = 1) {
        if (!musicEnabled || released.get()) return
        val normalizedWorldId = resolveRequestedWorld(worldId).coerceAtLeast(1)
        lastRequestedWorldId = normalizedWorldId

        if (isMusicRunning && currentMusicWorldId == normalizedWorldId) return
        stopMusicInternal(keepLastRequestedWorld = true)

        requestAudioFocus()

        isMusicRunning = true
        currentMusicWorldId = normalizedWorldId
        musicThread = Thread({
            try {
                // Per-world key center; calm adventure mood.
                val basePitch = when ((normalizedWorldId - 1) % 4) {
                    0 -> 261.63
                    1 -> 293.66
                    2 -> 329.63
                    else -> 220.00
                }
                // 8-step melody + matching bass roots, looped seamlessly.
                val melodyOffsets = floatArrayOf(0f, 4f, 7f, 12f, 7f, 4f, 2f, 5f)
                val bassRoots = floatArrayOf(0f, 0f, -5f, -3f, 0f, -5f, 2f, -3f)
                var step = 0

                while (isMusicRunning && musicEnabled && !released.get() && !Thread.currentThread().isInterrupted) {
                    val semitones = melodyOffsets[step % melodyOffsets.size]
                    val notePitch = basePitch * Math.pow(2.0, semitones / 12.0)
                    val bassPitch = basePitch * 0.5 * Math.pow(2.0, bassRoots[step % bassRoots.size] / 12.0)
                    // Music bypasses soundEnabled by design: the two settings are independent.
                    // Soft pad under each melody note keeps the loop immersive, not piercing.
                    playSegmentsBlocking(
                        listOf(
                            Segment(bassPitch, bassPitch, 150, 0.10f),
                            Segment(notePitch, notePitch, 150, 0.16f),
                            Segment(notePitch * 2.0, notePitch * 2.0, 150, 0.05f)
                        )
                    )
                    step++
                    Thread.sleep(60L)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (t: Throwable) {
                Log.w(TAG, "Music playback failed", t)
            } finally {
                // Only clear the flag if this thread is still the current music
                // thread, so a fast pause/resume cannot be clobbered by the
                // dying thread and leave the game permanently silent.
                if (musicThread === Thread.currentThread()) {
                    isMusicRunning = false
                }
            }
        }, "JumpAdventure-Music").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun pauseMusic() {
        abandonAudioFocus()
        if (isMusicRunning) {
            stopMusicInternal(keepLastRequestedWorld = true)
        }
    }

    @Synchronized
    fun resumeMusic() {
        if (!isMusicRunning && !released.get() && musicEnabled) {
            startMusic(lastRequestedWorldId)
        }
    }

    // ---------------------------------------------------------------------
    // Audio focus. Without this the game kept playing over the user's own
    // music and never ducked for calls or other apps.
    // ---------------------------------------------------------------------

    private var audioFocusRequest: Any? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseMusic()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { /* music volume is fixed per-note */ }
            AudioManager.AUDIOFOCUS_GAIN -> if (musicEnabled && !released.get()) {
                resumeMusic()
            }
        }
    }

    private fun audioManager(): AudioManager? =
        runCatching {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }.getOrNull()

    private fun requestAudioFocus() {
        val manager = audioManager() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                audioFocusRequest = request
                manager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }.onFailure { Log.w(TAG, "Audio focus request failed", it) }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? AudioFocusRequest)?.let { manager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(focusChangeListener)
            }
            audioFocusRequest = null
        }
    }

    @Synchronized
    fun stopMusic() {
        stopMusicInternal(keepLastRequestedWorld = true)
    }

    private fun stopMusicInternal(keepLastRequestedWorld: Boolean) {
        isMusicRunning = false
        currentMusicWorldId = -1
        musicThread?.interrupt()
        musicThread = null
        if (!keepLastRequestedWorld) lastRequestedWorldId = 1
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        stopMusicInternal(keepLastRequestedWorld = false)
        runCatching { sfxExecutor.shutdownNow() }
        lastPlayedMs.clear()
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
