package ai.fuzzylabs.insoleandroid.imu

import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.github.psambit9791.jdsp.transform.PCA
import kotlinx.coroutines.Dispatchers
import java.lang.Integer.max
import java.time.Instant
import kotlin.math.floor

@ExperimentalUnsignedTypes
private val TAG = IMUSession::class.java.simpleName

@ExperimentalUnsignedTypes
class IMUSession(samplingFrequency: Int = 100, val windowSizeMillis: Int = 10000){
    private val windowSize = windowSizeMillis / (1000 / samplingFrequency)
    private var window: MutableList<IMUReading> = MutableList(windowSize) {IMUReading.zero()}
    var currentElement: IMUSessionElement = IMUSessionElement(Instant.now(), getWindowCadence())
    var readings: MutableList<IMUReading> = mutableListOf()
    var elements: MutableList<IMUSessionElement> = mutableListOf()

    fun shiftWindow(reading: IMUReading) {
        window = window.drop(1).toMutableList();
        window.add(reading)
    }

    fun addReading(reading: IMUReading) {
        readings.add(reading)
    }

    fun clear() {
        readings = mutableListOf()
        elements = mutableListOf()
    }

    fun updateWindowMetrics(isRecording: Boolean) {
        val cadence = getWindowCadence()
        currentElement = IMUSessionElement(Instant.now(), cadence)
        if(isRecording) elements.add(currentElement)
    }

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

    private fun getWindowCadence(_window: Iterable<IMUReading>): Double {
        return getWindowCadencePreprocessed(getFirstPrincipleComponent(_window))
    }

    private fun getWindowCadencePreprocessed(_window: DoubleArray): Double {
        return getWindowStepCount(_window).toDouble() / windowSizeMillis.toDouble() * 60000.0
    }

    companion object {
        private fun getFirstPrincipleComponent(readings: Iterable<IMUReading>): DoubleArray {
            val acceleration = readings.map { it.getAcceleration() }
            val pca = PCA(acceleration.toTypedArray(), 3)
            pca.fit()
            return pca.transform().map { it.component1() }.toDoubleArray()
        }

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