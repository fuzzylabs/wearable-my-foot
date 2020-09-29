package ai.fuzzylabs.wearablemyfoot.imu

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
    private val aX: Float,
    private val aY: Float,
    private val aZ: Float,
    private val gX: Float,
    private val gY: Float,
    private val gZ: Float
) {
    override fun toString(): String {
        return "${time}\n${aX}\n${aY}\n${aZ}\n${gX}\n${gY}\n${gZ}"
    }

    /**
     * Get CSV row representation
     *
     * @return String to be used on CSV export
     */
    fun toCSVRow(): String {
        return "${time},${aX},${aY},${aZ},${gX},${gY},${gZ}\n"
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
                    aX,
                    aY,
                    aZ,
                    gX,
                    gY,
                    gZ
                )
            }
            return null
        }

        fun zero(): IMUReading {
            return IMUReading(0u,0F,0F,0F,0F,0F,0F)
        }
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
    fun getAcceleration(): DoubleArray {
        return doubleArrayOf(aX.toDouble() * G, aY.toDouble() * G, aZ.toDouble() * G)
    }
}