package com.jumpadventure.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

class SoundManager(context: Context) {
    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true

    /**
     * Synthesizes short audio tones dynamically using AudioTrack to ensure standalone audio without missing raw resources.
     */
    private fun playTone(freq: Double, durationMs: Int, freqEnd: Double = freq) {
        if (!soundEnabled) return

        thread {
            try {
                val sampleRate = 22050
                val numSamples = durationMs * sampleRate / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    val currentFreq = freq + (freqEnd - freq) * t
                    sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / currentFreq))
                }

                var idx = 0
                for (dVal in sample) {
                    // Ramp amplitude at start and end to avoid clicking
                    val shortVal = (dVal * 32767 * 0.4).toInt().toShort()
                    generatedSnd[idx++] = (shortVal.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = (shortVal.toInt() and 0xff00 shr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playJump() {
        playTone(300.0, 120, 600.0)
    }

    fun playCoin() {
        playTone(987.77, 100, 1318.51) // B5 to E6 chime
    }

    fun playStar() {
        playTone(523.25, 180, 1046.50) // C5 to C6 shine
    }

    fun playButtonClick() {
        playTone(400.0, 50, 200.0)
    }

    fun playHit() {
        playTone(180.0, 200, 80.0)
    }

    fun playLevelComplete() {
        playVictory()
    }

    fun playStarImpact(isCenter: Boolean = false) {
        if (!soundEnabled) return
        thread {
            try {
                val sampleRate = 22050
                val durationMs = if (isCenter) 220 else 160
                val numSamples = durationMs * sampleRate / 1000
                val generatedSnd = ByteArray(2 * numSamples)

                val baseFreq = if (isCenter) 200.0 else 300.0
                val chimeFreq = if (isCenter) 1318.51 else 1046.50

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / numSamples
                    val bodyEnv = Math.exp(-6.0 * t)
                    val chimeEnv = Math.exp(-3.5 * t)

                    val bodyWave = sin(2.0 * Math.PI * (baseFreq + 140.0 * (1.0 - t)) * (i.toDouble() / sampleRate)) * bodyEnv
                    val chimeWave = sin(2.0 * Math.PI * (chimeFreq + 180.0 * t) * (i.toDouble() / sampleRate)) * chimeEnv * 0.65

                    val sampleVal = ((bodyWave + chimeWave) * 0.7).coerceIn(-1.0, 1.0)
                    val shortVal = (sampleVal * 32767 * 0.55).toInt().toShort()

                    val idx = i * 2
                    generatedSnd[idx] = (shortVal.toInt() and 0x00ff).toByte()
                    generatedSnd[idx + 1] = (shortVal.toInt() and 0xff00 shr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playVictory() {
        if (!soundEnabled) return
        thread {
            try {
                // Victory intro -> rising celebratory tones -> strong congratulations finish
                val notes = arrayOf(
                    Triple(523.25, 110, 587.33),   // C5 -> D5
                    Triple(659.25, 110, 698.46),   // E5 -> F5
                    Triple(783.99, 130, 880.00),   // G5 -> A5
                    Triple(1046.50, 150, 1174.66), // C6 -> D6
                    Triple(1318.51, 160, 1396.91), // E6 -> F6
                    Triple(1567.98, 420, 1567.98)  // G6 triumphant holding tone
                )
                val delays = arrayOf(105L, 105L, 125L, 145L, 155L)

                for (i in notes.indices) {
                    if (!soundEnabled) break
                    val (fStart, dur, fEnd) = notes[i]
                    playTone(fStart, dur, fEnd)
                    if (i < delays.size) Thread.sleep(delays[i])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
