package ai.fuzzylabs.insoleandroid.imu

import android.util.Log
import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.github.psambit9791.jdsp.transform.PCA
import java.lang.Integer.max

@ExperimentalUnsignedTypes
private val TAG = IMUSession::class.java.simpleName

@ExperimentalUnsignedTypes
class IMUSession(samplingFrequency: Int = 100, private val windowSizeMillis: Int = 10000){
    private val windowSize = windowSizeMillis / (1000 / samplingFrequency)
    private var window: MutableList<IMUReading> = MutableList(windowSize) {IMUReading.zero()}
    var readings: MutableList<IMUReading> = mutableListOf()

    fun shiftWindow(reading: IMUReading) {
        window = window.drop(1).toMutableList();
        window.add(reading)
    }

    fun addReading(reading: IMUReading) {
        readings.add(reading)
    }

    fun getWindowStepCount(): Int {
        val steps = countSteps(window)
        Log.d(TAG, "Cadence: $steps")
        return steps
    }

    fun getWindowCadence(): Double {
        return getWindowStepCount().toDouble() / windowSizeMillis.toDouble() * 60000.0
    }

    companion object {
        private fun getFirstPrincipleComponent(readings: Iterable<IMUReading>): DoubleArray {
            val acceleration = readings.map { it.getAcceleration() }
            val pca = PCA(acceleration.toTypedArray(), 3)
            pca.fit()
            return pca.transform().map { it.component1() }.toDoubleArray()
        }

        fun countSteps(readings: Iterable<IMUReading>): Int {
            val pca0 = getFirstPrincipleComponent(readings)

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