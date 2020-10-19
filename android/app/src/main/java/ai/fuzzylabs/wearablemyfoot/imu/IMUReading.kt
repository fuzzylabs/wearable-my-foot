package ai.fuzzylabs.wearablemyfoot.imu

import ai.fuzzylabs.wearablemyfoot.util.DoubleVector3D
import java.nio.ByteBuffer

const val G = 9.8;


/**
 * A raw reading received from an Arduino IMU
 *
 * @constructor Creates an IMU reading instance with defined time, acceleration and angular velocity
 * @param[time] time from the start of Arduino, as an unsigned 32-bit integert
 * @param[aX] acceleration in +X direction (measured in Gs)
 * @param[aY] acceleration in +Y direction (measured in Gs)
 * @param[aZ] acceleration in +Z direction (measured in Gs)
 * @param[gX] angular velocity around +X axis (measured in deg/s)
 * @param[gY] angular velocity around +Y axis (measured in deg/s)
 * @param[gZ] angular velocity around +Z axis (measured in deg/s)
 */
@ExperimentalUnsignedTypes
class IMUReading constructor(
    private val time: UInt, // Arduino sends unsigned long, which is 32 bit
    private val acceleration: DoubleVector3D,
    private val angularVelocity: DoubleVector3D
) {
    var pc = DoubleVector3D.zero()

    override fun toString(): String {
        return "${time}\n$acceleration\n$angularVelocity\n"
    }

    /**
     * Get CSV row representation
     *
     * @return String to be used on CSV export
     */
    fun toCSVRow(): String {
        return "${time},${acceleration.toCSVRow()},${angularVelocity.toCSVRow()},${pc.toCSVRow()}\n"
    }

    /**
     * @return Time of the reading in ms from the start of the Arduino device
     */
    fun getTime(): UInt {
        return time;
    }

    /**
     * Get acceleration vector (in m/s^2)
     *
     * @return DoubleArray of acceleration in m/s^2 (+X, +Y, +Z)
     */
    fun getAcceleration(): DoubleArray = acceleration.toTypedArray().map { it * G }.toDoubleArray()

    /**
     * Get 0th PC of acceleration (in m/s^2)
     */
    fun getPC0(): Double {
        return pc.x
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray?): IMUReading? {
            if (byteArray != null) {
                // Bytes are sent in Little-Endian, but JVM expects Big-Endian
                val buffer = ByteBuffer.wrap(byteArray.reversedArray())
                val time = buffer.getInt(24)
                val aX = buffer.getFloat(20)
                val aY = buffer.getFloat(16)
                val aZ = buffer.getFloat(12)
                val gX = buffer.getFloat(8)
                val gY = buffer.getFloat(4)
                val gZ = buffer.getFloat(0)
                return IMUReading(
                    time.toUInt(),
                    DoubleVector3D(aX.toDouble(), aY.toDouble(), aZ.toDouble()),
                    DoubleVector3D(gX.toDouble(), gY.toDouble(), gZ.toDouble())
                )
            }
            return null
        }

        fun zero(): IMUReading {
            return IMUReading(0u, DoubleVector3D.zero(), DoubleVector3D.zero())
        }
    }
}