package ai.fuzzylabs.wearablemyfoot.imu

import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.github.psambit9791.jdsp.transform.PCA
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import ai.fuzzylabs.incrementalpca.IncrementalPCA
import android.util.Log

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
    internal var ipca: IncrementalPCA = IncrementalPCA(3,3)
    private var ipcaInitialized: Boolean = false
    private val windowSize = windowSizeMillis / (1000 / samplingFrequency)
    private var window: MutableList<IMUReading> = mutableListOf()
    var currentElement: IMUSessionElement = IMUSessionElement(Instant.now(), getWindowCadence())
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
            reading.setPC(pc[0], pc[1], pc[2])
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
            .map { (reading, pc) -> reading.setPC(pc[0], pc[1], pc[2]) }
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

    fun updateWindowMetrics(isRecording: Boolean) {
        val cadence = getWindowCadence()
        currentElement = IMUSessionElement(Instant.now(), cadence)
        Log.d(TAG, "Metrics updated: $currentElement")
        if(isRecording) elements.add(currentElement)
    }



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
        return getWindowCadencePreprocessed(_window.map { it.getPC0() }.toDoubleArray())
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