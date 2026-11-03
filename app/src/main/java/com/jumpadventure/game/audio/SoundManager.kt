package com.jumpadventure.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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

    /**
     * Short sound effects play on their own pool so a long cue (the victory
     * fanfare, which sleeps between notes) can no longer swallow the button
     * clicks and pickup sounds queued behind it. Two threads is enough: effects
     * are tens of milliseconds and the executor queues the rest.
     */
    private val sfxExecutor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "JumpAdventure-SFX").apply { isDaemon = true }
    }

    /** Serialises music so notes never overlap themselves. */
    private val musicExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "JumpAdventure-Music").apply { isDaemon = true }
    }

    @Volatile
    private var isMusicRunning = false

    @Volatile
    private var currentMusicWorldId = -1

    @Volatile
    private var lastRequestedWorldId = 1

    @Volatile
    private var musicThread: Thread? = null

    private fun submitSfx(block: () -> Unit) {
        if (!soundEnabled || released.get()) return
        runCatching {
            sfxExecutor.execute {
                if (soundEnabled && !released.get()) {
                    runCatching(block).onFailure { Log.w(TAG, "SFX playback failed", it) }
                }
            }
        }
    }

    private fun playSfxTone(freq: Double, durationMs: Int, freqEnd: Double = freq) {
        submitSfx { playToneBlocking(freq, durationMs, freqEnd, 0.40f) }
    }

    private fun playToneBlocking(
        freq: Double,
        durationMs: Int,
        freqEnd: Double = freq,
        gain: Float = 0.35f
    ) {
        if (released.get()) return

        val sampleRate = 22050
        val safeDuration = durationMs.coerceAtLeast(20)
        val numSamples = (safeDuration * sampleRate / 1000).coerceAtLeast(1)
        val generated = ByteArray(numSamples * 2)

        var phase = 0.0
        for (i in 0 until numSamples) {
            if (Thread.currentThread().isInterrupted) return
            val t = i.toDouble() / numSamples.toDouble()
            val currentFreq = freq + (freqEnd - freq) * t
            phase += 2.0 * PI * currentFreq / sampleRate.toDouble()
            val attack = (t / 0.08).coerceIn(0.0, 1.0)
            val release = ((1.0 - t) / 0.10).coerceIn(0.0, 1.0)
            val envelope = minOf(attack, release)
            val value = sin(phase) * envelope * gain
            val shortValue = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            val idx = i * 2
            generated[idx] = (shortValue.toInt() and 0xFF).toByte()
            generated[idx + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
        }

        val track = buildTrack(sampleRate, generated.size) ?: return

        try {
            track.write(generated, 0, generated.size)
            track.play()
            Thread.sleep(safeDuration.toLong() + 15L)
        } finally {
            releaseTrack(track)
        }
    }

    /**
     * Builds a static [AudioTrack] for one generated PCM buffer.
     *
     * Returns null instead of throwing when the device refuses a track (audio
     * HAL exhaustion, muted hardware, or a sample count the buffer cannot hold).
     * Previously such a failure propagated out of the note generator and could
     * take the game thread down with it.
     */
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

    fun playJump() = playSfxTone(300.0, 120, 600.0)
    fun playCoin() = playSfxTone(987.77, 100, 1318.51)
    fun playStar() = playSfxTone(523.25, 180, 1046.50)
    fun playButtonClick() = playSfxTone(400.0, 50, 200.0)
    fun playHit() = playSfxTone(180.0, 200, 80.0)
    fun playPowerUp() = playSfxTone(440.0, 160, 880.0)

    fun playPowerUpActivation(type: String) {
        when (type) {
            "MAGNET" -> playSfxTone(500.0, 180, 750.0)
            "SHIELD" -> playSfxTone(350.0, 200, 600.0)
            "SPEED" -> playSfxTone(600.0, 150, 1200.0)
            "HIGH_JUMP" -> playSfxTone(400.0, 180, 900.0)
            "POWER" -> playSfxTone(300.0, 220, 950.0)
            else -> playSfxTone(440.0, 160, 880.0)
        }
    }

    fun playShieldBreak() = playSfxTone(250.0, 140, 120.0)

    /** MainActivity owns the full winner fanfare; keep the engine completion cue short. */
    fun playLevelComplete() = playSfxTone(784.0, 140, 1046.5)

    fun playStarImpact(isCenter: Boolean = false) {
        submitSfx {
            val sampleRate = 22050
            val durationMs = if (isCenter) 220 else 160
            val numSamples = durationMs * sampleRate / 1000
            val generated = ByteArray(numSamples * 2)
            val baseFreq = if (isCenter) 200.0 else 300.0
            val chimeFreq = if (isCenter) 1318.51 else 1046.50

            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples.toDouble()
                val bodyEnv = exp(-6.0 * t)
                val chimeEnv = exp(-3.5 * t)
                val bodyWave = sin(2.0 * PI * (baseFreq + 140.0 * (1.0 - t)) * (i.toDouble() / sampleRate)) * bodyEnv
                val chimeWave = sin(2.0 * PI * (chimeFreq + 180.0 * t) * (i.toDouble() / sampleRate)) * chimeEnv * 0.65
                val sampleVal = ((bodyWave + chimeWave) * 0.7).coerceIn(-1.0, 1.0)
                val shortValue = (sampleVal * Short.MAX_VALUE * 0.55).toInt().toShort()
                val idx = i * 2
                generated[idx] = (shortValue.toInt() and 0xFF).toByte()
                generated[idx + 1] = ((shortValue.toInt() shr 8) and 0xFF).toByte()
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
        submitSfx {
            val notes = arrayOf(
                Triple(523.25, 100, 587.33),
                Triple(659.25, 100, 698.46),
                Triple(783.99, 120, 880.00),
                Triple(1046.50, 140, 1174.66),
                Triple(1318.51, 150, 1396.91),
                Triple(1567.98, 360, 1567.98)
            )
            for ((start, duration, end) in notes) {
                if (!soundEnabled || released.get()) break
                playToneBlocking(start, duration, end, 0.38f)
                if (Thread.currentThread().isInterrupted) break
                Thread.sleep(25L)
            }
        }
    }

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
                val basePitch = when ((normalizedWorldId - 1) % 4) {
                    0 -> 261.63
                    1 -> 293.66
                    2 -> 329.63
                    else -> 220.00
                }
                val melodyOffsets = floatArrayOf(0f, 4f, 7f, 12f, 7f, 4f, 2f, 5f)
                var step = 0

                while (isMusicRunning && musicEnabled && !released.get() && !Thread.currentThread().isInterrupted) {
                    val semitones = melodyOffsets[step % melodyOffsets.size]
                    val notePitch = basePitch * Math.pow(2.0, semitones / 12.0)
                    // Music bypasses soundEnabled by design: the two settings are independent.
                    playToneBlocking(notePitch, 145, notePitch, 0.20f)
                    step++
                    Thread.sleep(90L)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (t: Throwable) {
                Log.w(TAG, "Music playback failed", t)
            } finally {
                // Only clear the flag if this thread is still the current music
                // thread. A fast pause/resume restart used to have the dying
                // thread clobber the flag of its replacement, leaving the game
                // permanently silent.
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
        runCatching { musicExecutor.shutdownNow() }
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
