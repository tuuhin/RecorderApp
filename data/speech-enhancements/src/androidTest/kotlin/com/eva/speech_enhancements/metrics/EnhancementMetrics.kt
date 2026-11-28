package com.eva.speech_enhancements.metrics

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Reference-based quality scores for the real-speech enhancement tests.
 *
 * Every metric compares an estimate (the noisy input or the enhanced output)
 * against the dataset's clean ground truth from `assets/clear_audio`. The
 * "before" variant scores the noisy input against clean; the "after" variant
 * scores the enhancer's output against clean, so a healthy pipeline must move
 * every score toward the clean reference.
 *
 * Pure Kotlin, no Android dependencies, so it stays unit-testable.
 */
internal object EnhancementMetrics {

    private const val FFT_SIZE = 1024
    private const val HOP_SIZE = 256
    const val SPEECH_BAND_LOW_HZ = 300.0
    const val SPEECH_BAND_HIGH_HZ = 3400.0
    const val WIDE_BAND_HIGH_HZ = 8000.0
    private const val ACTIVE_FRAME_FRACTION = 0.1
    private const val NOISE_FLOOR_FRACTION = 0.1

    /**
     * Measures [noisy] and [enhanced] against the [clean] ground truth. All three
     * arrays must be the same length and sample rate; the quality test asserts that
     * before calling this.
     */
    suspend fun measure(
        clip: String,
        clean: FloatArray,
        noisy: FloatArray,
        enhanced: FloatArray,
        sampleRate: Int,
    ): EnhancementScore = coroutineScope {

        if (clean.size != noisy.size) throw IllegalStateException("Clean noisy length mismatch")
        if (clean.size != enhanced.size) throw IllegalStateException("Clean enhanced length mismatch")
        if (clean.size < FFT_SIZE) throw IllegalStateException("clip too short to check the spectral lines")

        val siSdrBefore = async { siSdrDb(reference = clean, estimate = noisy) }
        val siSdrAfter = async { siSdrDb(reference = clean, estimate = enhanced) }

        val normalCorrelationBefore = async { normalizedCorrelation(clean, noisy) }
        val normalCorrelationAfter = async { normalizedCorrelation(clean, enhanced) }

        val lsdbBefore = async { lsdDb(clean, noisy) }
        val lsdbAfter = async { lsdDb(clean, enhanced) }

        val segmentalBefore = async { segmentalSnrDb(clean, noisy) }
        val segmentalAfter = async { segmentalSnrDb(clean, enhanced) }

        val noiseGain = async { noiseFloorGainDb(clean, noisy, enhanced, sampleRate) }

        val bandRetentionBefore = async {
            bandRetentionDb(
                clean,
                enhanced,
                sampleRate,
                SPEECH_BAND_LOW_HZ,
                SPEECH_BAND_HIGH_HZ,
            )
        }
        val bandRetentionAfter = async {
            bandRetentionDb(
                clean,
                enhanced,
                sampleRate,
                SPEECH_BAND_LOW_HZ,
                WIDE_BAND_HIGH_HZ,
            )
        }

        val bestLag = async { bestLag(clean, enhanced, maxLagSamples = 1440) }


        EnhancementScore(
            clip = clip,
            siSdrBeforeDb = siSdrBefore.await(),
            siSdrAfterDb = siSdrAfter.await(),
            deltaSiSdrDb = siSdrAfter.await() - siSdrBefore.await(),
            correlationBefore = normalCorrelationBefore.await(),
            correlationAfter = normalCorrelationAfter.await(),
            lsdBeforeDb = lsdbBefore.await(),
            lsdAfterDb = lsdbAfter.await(),
            segSnrBeforeDb = segmentalBefore.await(),
            segSnrAfterDb = segmentalAfter.await(),
            noiseFloorGainDb = noiseGain.await(),
            bandRetention300To3400Db = bandRetentionBefore.await(),
            bandRetention300To8000Db = bandRetentionAfter.await(),
            alignmentLagSamples = bestLag.await(),
        )
    }

    /**
     * Scale-invariant signal-to-distortion ratio in dB of [estimate] against
     * [reference]: the estimate is first projected onto the reference so an
     * output gain difference does not count as distortion.
     */
    suspend fun siSdrDb(reference: FloatArray, estimate: FloatArray): Double = withContext(Dispatchers.Default) {
        var referenceEnergy = 0.0
        var projection = 0.0
        var estimateMean = 0.0
        for (sample in estimate) {
            currentCoroutineContext().ensureActive()
            estimateMean += sample
        }
        estimateMean /= estimate.size

        var referenceMean = 0.0
        for (sample in reference) {
            currentCoroutineContext().ensureActive()
            referenceMean += sample
        }
        referenceMean /= reference.size

        for (index in reference.indices) {
            currentCoroutineContext().ensureActive()
            val r = reference[index] - referenceMean
            val e = estimate[index] - estimateMean
            referenceEnergy += r * r
            projection += r * e
        }
        val alpha = projection / (referenceEnergy + 1e-12)

        var targetEnergy = 0.0
        var distortionEnergy = 0.0
        for (index in reference.indices) {
            currentCoroutineContext().ensureActive()
            val r = reference[index] - referenceMean
            val e = estimate[index] - estimateMean
            val target = alpha * r
            val distortion = e - target
            targetEnergy += target * target
            distortionEnergy += distortion * distortion
        }
        10.0 * log10((targetEnergy + 1e-12) / (distortionEnergy + 1e-12))
    }


    /** Zero-lag normalized cross-correlation in [-1, 1]. */
    suspend fun normalizedCorrelation(a: FloatArray, b: FloatArray) = withContext(Dispatchers.Default) {
        var dot = 0.0
        var aEnergy = 0.0
        var bEnergy = 0.0

        for (index in a.indices) {
            currentCoroutineContext().ensureActive()
            val x = a[index].toDouble()
            val y = b[index].toDouble()
            dot += x * y
            aEnergy += x * x
            bEnergy += y * y
        }
        dot / (sqrt(aEnergy * bEnergy) + 1e-12)
    }


    /**
     * Mean log-spectral distance in dB between [reference] and [estimate], with a
     * global gain match first so an output level difference does not dominate.
     *
     * Per-frame spectra are floored at 60 dB below the frame's peak bin: a denoiser
     * that pushes noise-only bins far below the clean reference would otherwise
     * dominate the mean with hundreds of dB of deviation and hide the actual
     * spectral difference.
     */
    suspend fun lsdDb(reference: FloatArray, estimate: FloatArray) = withContext(Dispatchers.Default) {
        val gain = projectionGain(reference, estimate)
        val window = hann(FFT_SIZE)
        val spectralFloorDb = 60.0
        var squaredSum = 0.0
        var frames = 0
        var offset = 0
        while (offset + FFT_SIZE <= reference.size) {
            currentCoroutineContext().ensureActive()
            val refSpectrum = magnitudeSquared(DoubleArray(FFT_SIZE) { reference[offset + it] * window[it] })
            val estSpectrum = magnitudeSquared(DoubleArray(FFT_SIZE) { estimate[offset + it] * gain * window[it] })
            val peakDb = refSpectrum.max().let { 10.0 * log10(it + 1e-12) }
            val floorDb = peakDb - spectralFloorDb
            for (bin in refSpectrum.indices) {
                currentCoroutineContext().ensureActive()
                val refDb = (10.0 * log10(refSpectrum[bin] + 1e-12)).coerceAtLeast(floorDb)
                val estDb = (10.0 * log10(estSpectrum[bin] + 1e-12)).coerceAtLeast(floorDb)
                squaredSum += (refDb - estDb) * (refDb - estDb)
            }
            frames++
            offset += HOP_SIZE
        }
        if (frames <= 0) throw IllegalStateException("Clips too short")
        sqrt(squaredSum / (frames * refBinCount()))
    }

    /** Mean frame-wise SNR in dB over frames whose reference energy is non-trivial. */
    suspend fun segmentalSnrDb(reference: FloatArray, estimate: FloatArray) = withContext(Dispatchers.Default) {
        val gain = projectionGain(reference, estimate)
        val window = hann(FFT_SIZE)
        var sum = 0.0
        var frames = 0
        var offset = 0
        while (offset + FFT_SIZE <= reference.size) {
            currentCoroutineContext().ensureActive()
            var referenceEnergy = 0.0
            var errorEnergy = 0.0
            for (i in 0 until FFT_SIZE) {
                currentCoroutineContext().ensureActive()
                val r = reference[offset + i] * window[i]
                val e = estimate[offset + i] * gain * window[i]
                referenceEnergy += r * r
                errorEnergy += (e - r) * (e - r)
            }
            // skip near-silent frames so the noise floor does not dominate the mean
            if (referenceEnergy > 1e-6) {
                sum += 10.0 * log10((referenceEnergy + 1e-12) / (errorEnergy + 1e-12))
                frames++
            }
            offset += HOP_SIZE
        }
        if (frames <= 0) throw IllegalStateException("Clips too short")
        sum / frames
    }

    /**
     * How much quieter the noise floor became, in dB: the ratio between the
     * noise-to-speech frame-energy gap before and after enhancement. Positive
     * values mean the background got quieter relative to the speech peaks.
     */
    suspend fun noiseFloorGainDb(
        clean: FloatArray,
        noisy: FloatArray,
        enhanced: FloatArray,
        sampleRate: Int,
    ) = withContext(Dispatchers.Default) {
        val frameRmsClean = frameRms(clean)
        val activeThreshold = ACTIVE_FRAME_FRACTION * frameRmsClean.max()

        var noisyNoiseEnergy = 0.0
        var noisyNoiseCount = 0
        var noisySpeechEnergy = 0.0
        var noisySpeechCount = 0
        var enhancedNoiseEnergy = 0.0
        var enhancedNoiseCount = 0
        var enhancedSpeechEnergy = 0.0
        var enhancedSpeechCount = 0

        val frameSize = FFT_SIZE
        var frameIndex = 0
        var offset = 0
        while (offset + frameSize <= clean.size) {
            currentCoroutineContext().ensureActive()
            var noisyEnergy = 0.0
            var enhancedEnergy = 0.0
            for (i in 0 until frameSize) {
                currentCoroutineContext().ensureActive()
                noisyEnergy += noisy[offset + i].toDouble() * noisy[offset + i]
                enhancedEnergy += enhanced[offset + i].toDouble() * enhanced[offset + i]
            }
            val isSpeech = frameRmsClean[frameIndex] > activeThreshold
            if (isSpeech) {
                noisySpeechEnergy += noisyEnergy
                noisySpeechCount++
                enhancedSpeechEnergy += enhancedEnergy
                enhancedSpeechCount++
            } else {
                noisyNoiseEnergy += noisyEnergy
                noisyNoiseCount++
                enhancedNoiseEnergy += enhancedEnergy
                enhancedNoiseCount++
            }
            frameIndex++
            offset += frameSize
        }
        require(noisyNoiseCount > 0 && noisySpeechCount > 0) { "clip has no speech/noise frame split" }
        require(enhancedNoiseCount > 0 && enhancedSpeechCount > 0) { "clip has no speech/noise frame split" }

        val gapBefore = speechToNoiseGapDb(noisySpeechEnergy, noisySpeechCount, noisyNoiseEnergy, noisyNoiseCount)
        val gapAfter =
            speechToNoiseGapDb(enhancedSpeechEnergy, enhancedSpeechCount, enhancedNoiseEnergy, enhancedNoiseCount)
        gapAfter - gapBefore
    }

    /**
     * Band-energy retention in dB of [estimate] against [reference] between
     * [lowHz] and [highHz], with a global gain match. 0 dB is perfect; large
     * negative values mean the enhancer removed speech content in that band.
     */
    suspend fun bandRetentionDb(
        reference: FloatArray,
        estimate: FloatArray,
        sampleRate: Int,
        lowHz: Double,
        highHz: Double,
    ) = withContext(Dispatchers.Default) {
        val gain = projectionGain(reference, estimate)
        val window = hann(FFT_SIZE)
        var referenceEnergy = 0.0
        var estimateEnergy = 0.0
        var offset = 0
        while (offset + FFT_SIZE <= reference.size) {
            currentCoroutineContext().ensureActive()
            val refSpectrum = magnitudeSquared(DoubleArray(FFT_SIZE) { reference[offset + it] * window[it] })
            val estSpectrum = magnitudeSquared(DoubleArray(FFT_SIZE) { estimate[offset + it] * gain * window[it] })
            for (bin in refSpectrum.indices) {
                currentCoroutineContext().ensureActive()
                val hz = bin * sampleRate.toDouble() / FFT_SIZE
                if (hz in lowHz..highHz) {
                    referenceEnergy += refSpectrum[bin]
                    estimateEnergy += estSpectrum[bin]
                }
            }
            offset += HOP_SIZE
        }
        10.0 * log10((estimateEnergy + 1e-12) / (referenceEnergy + 1e-12))
    }

    /**
     * Lag in samples that best aligns [estimate] to [reference]: positive means
     * the estimate is delayed. Searched with a coarse-plus-fine strategy up to
     * [maxLagSamples]; 0 is the expected value for a delay-compensated pass.
     */
    suspend fun bestLag(reference: FloatArray, estimate: FloatArray, maxLagSamples: Int) =
        withContext(Dispatchers.Default) {
            val coarseStep = 16
            var bestCoarse = 0
            var bestCoarseValue = -2.0
            var lag = -maxLagSamples
            while (lag <= maxLagSamples) {
                currentCoroutineContext().ensureActive()
                val value = lagCorrelation(reference, estimate, lag)
                if (value > bestCoarseValue) {
                    bestCoarseValue = value
                    bestCoarse = lag
                }
                lag += coarseStep
            }
            var bestFine = bestCoarse
            var bestFineValue = bestCoarseValue
            for (fine in (bestCoarse - coarseStep + 1)..<(bestCoarse + coarseStep)) {
                currentCoroutineContext().ensureActive()
                if (abs(fine) > maxLagSamples) continue
                val value = lagCorrelation(reference, estimate, fine)
                if (value > bestFineValue) {
                    bestFineValue = value
                    bestFine = fine
                }
            }
            bestFine
        }

    /** Optimal projection gain of [estimate] onto [reference]: sum(r*e)/sum(r*r). */
    private fun projectionGain(reference: FloatArray, estimate: FloatArray): Double {
        var dot = 0.0
        var referenceEnergy = 0.0
        for (index in reference.indices) {
            val r = reference[index].toDouble()
            dot += r * estimate[index]
            referenceEnergy += r * r
        }
        return dot / (referenceEnergy + 1e-12)
    }

    private fun speechToNoiseGapDb(
        speechEnergy: Double,
        speechFrames: Int,
        noiseEnergy: Double,
        noiseFrames: Int,
    ): Double {
        val speechPower = speechEnergy / (speechFrames * FFT_SIZE)
        val noisePower = noiseEnergy / (noiseFrames * FFT_SIZE)
        return 10.0 * log10((speechPower + 1e-12) / (noisePower + 1e-12))
    }

    private fun frameRms(samples: FloatArray): DoubleArray {
        val frameSize = FFT_SIZE
        val frameCount = samples.size / frameSize
        val rms = DoubleArray(frameCount)
        for (frame in 0 until frameCount) {
            var energy = 0.0
            val offset = frame * frameSize
            for (i in 0 until frameSize) energy += samples[offset + i].toDouble() * samples[offset + i]
            rms[frame] = sqrt(energy / frameSize)
        }
        return rms
    }

    private fun lagCorrelation(reference: FloatArray, estimate: FloatArray, lag: Int): Double {
        val size = reference.size
        return when {
            lag >= 0 -> {
                val length = size - lag
                if (length <= 0) -2.0
                else normalizedCorrelation(reference, lag, estimate, 0, length)
            }

            else -> {
                val length = size + lag
                if (length <= 0) -2.0
                else normalizedCorrelation(reference, 0, estimate, -lag, length)
            }
        }
    }

    private fun normalizedCorrelation(
        a: FloatArray,
        aOffset: Int,
        b: FloatArray,
        bOffset: Int,
        length: Int,
    ): Double {
        var dot = 0.0
        var aEnergy = 0.0
        var bEnergy = 0.0
        for (i in 0 until length) {
            val x = a[aOffset + i].toDouble()
            val y = b[bOffset + i].toDouble()
            dot += x * y
            aEnergy += x * x
            bEnergy += y * y
        }
        return dot / (sqrt(aEnergy * bEnergy) + 1e-12)
    }

    private fun refBinCount(): Int = FFT_SIZE / 2 + 1

    private fun magnitudeSquared(frame: DoubleArray): DoubleArray {
        val spectrum = fft(frame)
        val complexBins = spectrum.size / 2 // one complex bin per FFT point
        val out = DoubleArray(complexBins / 2 + 1)
        for (bin in 0..complexBins / 2) {
            val real = spectrum[2 * bin]
            val imag = spectrum[2 * bin + 1]
            out[bin] = real * real + imag * imag
        }
        return out
    }

    /** In-place-ish iterative radix-2 FFT over a power-of-two [input]; returns interleaved real/imag. */
    private fun fft(input: DoubleArray): DoubleArray {
        val n = input.size
        require(n and (n - 1) == 0) { "FFT size must be a power of two, was $n" }

        val real = DoubleArray(n) { input[it] }
        val imag = DoubleArray(n)

        // bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tempReal = real[i]; real[i] = real[j]; real[j] = tempReal
                val tempImag = imag[i]; imag[i] = imag[j]; imag[j] = tempImag
            }
        }

        var length = 2
        while (length <= n) {
            val step = 2 * PI / length
            for (start in 0 until n step length) {
                var angle = 0.0
                var k = start
                while (k < start + length / 2) {
                    val cosA = cos(angle)
                    val sinA = sin(angle)
                    val tReal = real[k + length / 2] * cosA + imag[k + length / 2] * sinA
                    val tImag = -real[k + length / 2] * sinA + imag[k + length / 2] * cosA
                    real[k + length / 2] = real[k] - tReal
                    imag[k + length / 2] = imag[k] - tImag
                    real[k] += tReal
                    imag[k] += tImag
                    k++
                    angle += step
                }
            }
            length = length shl 1
        }
        return DoubleArray(2 * n) { if (it < n) real[it] else imag[it - n] }
    }

    private fun hann(size: Int): DoubleArray = DoubleArray(size) { index ->
        0.5 * (1.0 - cos(2.0 * PI * index / (size - 1)))
    }
}
