package ai.fuzzylabs.wearablemyfoot.imu

import org.junit.jupiter.api.*
import kotlin.math.absoluteValue

@ExperimentalUnsignedTypes
internal class IMUSessionTest {

    @Test
    fun updateWindowMetrics() {
        session.updateWindowMetrics(isRecording = false)
        assert((session.currentElement.cadence - 85.0).absoluteValue < 10.0) {
            "Cadence expected 85.0 (+-10) but was ${session.currentElement.cadence}"
        }
        assert((session.currentElement.speed - 4.0).absoluteValue < 0.5) {
            "Speed expected 4.0 (+-0.5) m/s but was ${session.currentElement.speed}"
        }
    }

    companion object {
        private val session: IMUSession = IMUSession()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val reader = this::class.java.classLoader?.getResourceAsStream("test-run.csv")?.reader()?.readLines()
            // stop at record 4000, that aligns to the desired window for calculations
            val imuReadings = reader!!.drop(1).take(3999).map { readingFromCSVString(it) }
            imuReadings.forEach { session.shiftWindow(it, false) }
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            session.clear()
        }

        private fun readingFromCSVString(str: String): IMUReading {
            val stringFields = str.split(",")
            val time = stringFields.first().toUInt()
            val readings = stringFields.drop(1).map { it.toFloat() }.toTypedArray()
            return IMUReading(
                time,
                readings[0],
                readings[1],
                readings[2],
                readings[3],
                readings[4],
                readings[5]
            )
        }
    }
}