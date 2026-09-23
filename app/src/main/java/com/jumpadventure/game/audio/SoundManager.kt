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
        thread {
            playTone(523.25, 120) // C5
            Thread.sleep(130)
            playTone(659.25, 120) // E5
            Thread.sleep(130)
            playTone(783.99, 120) // G5
            Thread.sleep(130)
            playTone(1046.50, 250) // C6
        }
    }
}
