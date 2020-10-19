package ai.fuzzylabs.wearablemyfoot.imu

import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import java.time.Instant
import kotlin.math.floor
import ai.fuzzylabs.incrementalpca.IncrementalPCA
import ai.fuzzylabs.wearablemyfoot.math.cumtrapz
import ai.fuzzylabs.wearablemyfoot.util.DoubleVector3D
import kotlin.math.absoluteValue

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
class IMUSession(val pcaInitialSize: Int = 50, samplingFrequency: Int = 100, val windowSizeMillis: Int = 10000){
    private var ipca: IncrementalPCA = IncrementalPCA(3,3)
    private var ipcaInitialized: Boolean = false
    private val windowSize = windowSizeMillis / (1000 / samplingFrequency)
    private var window: MutableList<IMUReading> = mutableListOf()
    var currentElement: IMUSessionElement = IMUSessionElement(
        Instant.now(),
        0.0,
        0.0,
        0.0
    )
    var readings: MutableList<IMUReading> = mutableListOf()
    var elements: MutableList<IMUSessionElement> = mutableListOf()

    /**
     * Shift current window by adding new reading
     *
     * @param[reading] Reading to be added
     */
    fun shiftWindow(reading: IMUReading, isRecording: Boolean) {
        // Wait for enough readings for PCA initialisation
        if(!ipcaInitialized) {
            window.add(reading)
            if(window.size == pcaInitialSize) {
                initializePCA()
                ipcaInitialized = true
            }
        } else {
            val pc = ipca.update(reading.getAcceleration())
            reading.pc = DoubleVector3D(pc[0], pc[1], pc[2])
            if(window.size >= windowSize) window = window.drop(1).toMutableList()
            window.add(reading)
            if(isRecording) addReading(reading)
        }
    }

    /**
     * Add reading to the recording
     *
     * @param[reading] Reading to be added
     */
    private fun addReading(reading: IMUReading) {
        readings.add(reading)
    }

    private fun initializePCA() {
        val accelerationArray = window.map { it.getAcceleration() }.toTypedArray()
        val pcs = ipca.initialize(accelerationArray)
        window = window.zip(pcs)
            .map { (reading, pc) -> reading.also { it.pc = DoubleVector3D(pc[0], pc[1], pc[2]) } }
            .toMutableList()
    }

    /**
     * Clear the recording
     *
     * Makes [readings] and [elements] lists empty
     */
    fun clear() {
        window = mutableListOf()
        readings = mutableListOf()
        elements = mutableListOf()
        ipca = IncrementalPCA(3,3)
        ipcaInitialized = false
    }

    fun updateWindowMetrics(noNewReadings: Int, isRecording: Boolean) {
        currentElement = getWindowMetrics(noNewReadings, updateDistance = isRecording)
        if(isRecording) elements.add(currentElement)
    }

    private fun getWindowMetrics(noNewReadings: Int, updateDistance: Boolean): IMUSessionElement =
        Companion.getWindowMetrics(window, noNewReadings, currentElement.distance, updateDistance)

    /**
     * Get ByteArray representation of the window for visualisation
     */
    fun getVisualisationBytes(): ByteArray {
        val pc = window.map { it.getPC0() }
        val minAcceleration = -16.0 * 9.8
        val maxAcceleration = +16.0 * 9.8
        return pc.map {
            floor((it - minAcceleration) / (maxAcceleration - minAcceleration) * 255).toInt().toByte()
        }.toByteArray()
    }

    companion object {
        /**
         * Calculates running metrics for a given window of readings
         *
         * @param[window] Iterable of readings, window to perform calculations on
         * @param[noNewReadings] Number of new readings since the last update
         * @param[lastDistance] Distance estimate from the previous calculation
         * @return IMUSessionElement containing current time and calculated metrics
         */
        fun getWindowMetrics(
            window: Iterable<IMUReading>,
            noNewReadings: Int,
            lastDistance: Double,
            updateDistance: Boolean
        ): IMUSessionElement {
            val pca0 = window.map { it.getPC0() }.toDoubleArray()
            val startTime = window.first().getTime()
            val time = window.map { (it.getTime() - startTime).toDouble() / 1000 }
            val windowDeltaTime = window.last().getTime() - window.first().getTime()
            val newReadings = window.toList().takeLast(noNewReadings)
            val newDeltaTime = newReadings.last().getTime() - newReadings.first().getTime()

            val stepPeaks = findStepPeaks(pca0)
            val cadence = getWindowCadence(stepPeaks, windowDeltaTime.toInt())
            val speed = getSpeed(pca0, stepPeaks, time)
            val distance =
                if (updateDistance) lastDistance + speed * (newDeltaTime.toDouble() / 1000) else lastDistance

            return IMUSessionElement(Instant.now(), cadence, speed, distance)
        }

        /**
         * Get estimated speed for a windom
         *
         * @param[pca0] Input array, the first principle component is assumed
         * @param[stepPeaks] Iterable of peaks corresponding to steps
         * @param[time] Relative time for each reading
         * @return Speed in m/s^2
         */
        private fun getSpeed(pca0: DoubleArray, stepPeaks: IntArray, time: List<Double>): Double {
            val midpoints = listOf(0, *stepPeaks.toTypedArray(), pca0.size - 1)
                .zipWithNext()
                .map { (it.first + it.second) / 2 }
            val speedValues = midpoints.zipWithNext()
                .map { (a, b) -> getStepSpeed(pca0.slice(a..b), time.slice(a..b)) }

            return if (speedValues.isEmpty()) 0.0 else speedValues.sum() / speedValues.size
        }

        private fun getStepSpeed(stepWindow: Iterable<Double>, time: Iterable<Double>): Double {
            return cumtrapz(stepWindow, time).map { it.absoluteValue }.maxOrNull() ?: 0.0
        }

        /**
         * Get cadence for a window
         *
         * @param[stepPeaks] Iterable of peaks corresponding to steps
         * @param[deltaTime] Size of the window in milliseconds
         */
        private fun getWindowCadence(stepPeaks: IntArray, deltaTime: Int): Double {
            return stepPeaks.size / deltaTime.toDouble() * 60000.0
        }

        /**
         * Finds peaks in a signal that correspond to individual steps
         *
         * Implements peak detection step counting. A step is counted if it exceeds
         * 35.0 m/s^2. Enforces distance between peaks of 20 samples
         *
         * @param[pca0] Input array, the first principle component is assumed
         * @return Iterable<Int> with indices of the original array corresponding to peaks
         */
        private fun findStepPeaks(pca0: DoubleArray): IntArray {
            val fp = FindPeak(pca0)
            var outPeaks: Peak? = null
            try {
                outPeaks = fp.detectPeaks()
            } catch (e: NegativeArraySizeException) {
                // Zero peaks found and the jDSP throws exceptions
            }

            // Order is important! Until jDSP provides a method for filtering by multiple
            // properties filtering by distance needs to be performed first, so not to recreate
            // peak priorities
            val peaksByDistance = outPeaks?.filterByPeakDistance(20) ?: intArrayOf()
            val peakHeights = outPeaks?.findPeakHeights(peaksByDistance) ?: doubleArrayOf()

            return peaksByDistance.zip(peakHeights.asIterable())
                .filter { it.second >= 50.0 }
                .map { it.first }
                .toIntArray()
        }
    }
}