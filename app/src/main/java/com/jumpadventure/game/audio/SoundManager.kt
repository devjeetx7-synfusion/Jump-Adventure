package com.jumpadventure.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
    private val sfxExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "JumpAdventure-SFX").apply { isDaemon = true }
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

        val track = AudioTrack.Builder()
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
            .setBufferSizeInBytes(generated.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(generated, 0, generated.size)
            track.play()
            Thread.sleep(safeDuration.toLong() + 15L)
        } finally {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            }
            track.release()
        }
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

            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(generated.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                track.write(generated, 0, generated.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 15L)
            } finally {
                runCatching {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                }
                track.release()
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
                isMusicRunning = false
            }
        }, "JumpAdventure-Music").apply {
            isDaemon = true
            start()
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
        sfxExecutor.shutdownNow()
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}
