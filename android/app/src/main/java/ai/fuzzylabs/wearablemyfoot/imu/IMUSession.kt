package ai.fuzzylabs.wearablemyfoot.imu

import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.github.psambit9791.jdsp.transform.PCA
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max

@ExperimentalUnsignedTypes
private val TAG = IMUSession::class.java.simpleName

/**
 * Session of an interaction with IMU
 *
 * @constructor Creates and prepares a session for recording
 * @param[samplingFrequency] Sampling frequency of an IMU (Hz)
 * @param[windowSizeMillis] Window size to be used for calculation (ms)
 * @property[currentElement] The last session element ([IMUSessionElement]) calculated
 * @property[readings] List of [IMUReading] recorded during a session
 * @property[elements] List of [IMUSessionElement] calculated during a session
 */
@ExperimentalUnsignedTypes
class IMUSession(samplingFrequency: Int = 100, val windowSizeMillis: Int = 10000){
    private val windowSize = windowSizeMillis / (1000 / samplingFrequency)
    private var window: MutableList<IMUReading> = MutableList(windowSize) {IMUReading.zero()}
    var currentElement: IMUSessionElement = IMUSessionElement(Instant.now(), getWindowCadence())
    var readings: MutableList<IMUReading> = mutableListOf()
    var elements: MutableList<IMUSessionElement> = mutableListOf()

    /**
     * Shift current window by adding new reading
     *
     * @param[reading] Reading to be added
     */
    fun shiftWindow(reading: IMUReading) {
        window = window.drop(1).toMutableList();
        window.add(reading)
    }

    /**
     * Add reading to the recording
     *
     * @param[reading] Reading to be added
     */
    fun addReading(reading: IMUReading) {
        readings.add(reading)
    }

    /**
     * Clear the recording
     *
     * Makes [readings] and [elements] lists empty
     */
    fun clear() {
        readings = mutableListOf()
        elements = mutableListOf()
    }

    fun updateWindowMetrics(isRecording: Boolean) {
        val cadence = getWindowCadence()
        currentElement = IMUSessionElement(Instant.now(), cadence)
        if(isRecording) elements.add(currentElement)
    }

    /**
     * Recalculate the running session
     *
     * Recalculates all elements of a session (to mitigate the limitation of local PCA)
     */
    fun recalculateElements(startTime: Instant) {
        val startMillis = readings.first().getTime()
        val endMillis = readings.last().getTime()
        val stepMillis: Int = 1000
        val pca0 = getFirstPrincipleComponent(readings)
        val times = readings.map { it.getTime() }

        // Get the sequence of time windows to perform calculation on
        val timeSequence = ((startMillis + stepMillis.toUInt())..(endMillis + windowSizeMillis.toUInt())).step(stepMillis)

        // Get reading (pca0) windows to perform calculations on
        val windowSequence = sequence<DoubleArray> {
            for (time in timeSequence) {
                yield(pca0.zip(times)
                    .filter { it.second >= time - windowSizeMillis.toUInt() && it.second <= time }
                    .unzip().first.toDoubleArray())
            }
        }

        val elements = sequence<IMUSessionElement> {
            for ((time, window) in (timeSequence.asIterable()).zip(windowSequence.asIterable())) {
                val cadence = getWindowCadencePreprocessed(window)
                val realTime = startTime.plusMillis((time - startMillis).toLong())
                yield(IMUSessionElement(realTime, cadence))
            }

        }.toMutableList()

        this.elements = elements

    }

    /**
     * Get ByteArray representation of the window for visualisation
     */
    fun getVisualisationBytes(): ByteArray {
        val pc = getFirstPrincipleComponent(window)
        val minAcceleration = -16.0 * 9.8
        val maxAcceleration = +16.0 * 9.8
        return pc.map {
            floor((it - minAcceleration) / (maxAcceleration - minAcceleration) * 255).toInt().toByte()
        }.toByteArray()
    }

    private fun getWindowStepCount(_window: DoubleArray): Int {
        return countSteps(_window)
    }

    private fun getWindowCadence(): Double {
        return getWindowCadence(window)
    }

    /**
     * Get cadence for a window of raw readings
     *
     * @param[_window] An iterable of raw [IMUReading]s
     */
    private fun getWindowCadence(_window: Iterable<IMUReading>): Double {
        return getWindowCadencePreprocessed(getFirstPrincipleComponent(_window))
    }

    /**
     * Get cadence for a window of preprocessed readings
     *
     * @param[_window] Array of preprocessed acceleration values, e.g. with PCA
     * @see[getFirstPrincipleComponent]
     */
    private fun getWindowCadencePreprocessed(_window: DoubleArray): Double {
        return getWindowStepCount(_window).toDouble() / windowSizeMillis.toDouble() * 60000.0
    }

    companion object {
        /**
         * Perform PCA and get first Principle Component
         *
         * @param[readings] Collection of raw [IMUReading]s
         */
        private fun getFirstPrincipleComponent(readings: Iterable<IMUReading>): DoubleArray {
            val acceleration = readings.map { it.getAcceleration() }
            val pca = PCA(acceleration.toTypedArray(), 3)
            pca.fit()
            return pca.transform().map { it.component1() }.toDoubleArray()
        }

        /**
         * Count steps using peak detection
         *
         * Implements peak detection step counting. A step is counted if it exceeds
         * positive or negative 35.0 m/s^2
         *
         * @param[pca0] Input array, the first principle component is assumed
         */
        fun countSteps(pca0: DoubleArray): Int {
            with(Dispatchers.Default){

                val fp = FindPeak(pca0)
                var outPeaks: Peak? = null
                try {
                    outPeaks = fp.detectPeaks()
                } catch (e: NegativeArraySizeException) {
                    // Zero peaks found and the jDSP throws exceptions
                }
                val noPeaks: Int =  outPeaks?.filterByHeight(35.0, 200.0)?.size ?: 0

                var outTroughs: Peak? = null
                try {
                    outTroughs = fp.detectTroughs()
                } catch (e: NegativeArraySizeException) {
                    // Zero peaks found and the jDSP throws exceptions
                }
                val noTroughs =  outTroughs?.filterByHeight(35.0, 200.0)?.size ?: 0

                return max(noPeaks, noTroughs)
            }

        }
    }
}