package ai.fuzzylabs.insoleandroid.imu

import java.nio.ByteBuffer

const val G = 9.8;
class IMUReading @ExperimentalUnsignedTypes constructor(
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

    fun toCSVRow(): String {
        return "${time},${aX},${aY},${aZ},${gX},${gY},${gZ}\n"
    }

    @ExperimentalUnsignedTypes
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

    fun getTime(): UInt {
        return time;
    }

    fun getAcceleration(): DoubleArray {
        return doubleArrayOf(aX.toDouble() * G, aY.toDouble() * G, aZ.toDouble() * G)
    }
}