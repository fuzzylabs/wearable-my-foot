package ai.fuzzylabs.insoleandroid

import java.nio.ByteBuffer
import java.time.Instant

class IMUReading(
    private val aX: Float,
    private val aY: Float,
    private val aZ: Float,
    private val gX: Float,
    private val gY: Float,
    private val gZ: Float
) {
    private val instant = Instant.now()

    override fun toString(): String {
        return "${instant}\n${aX}\n${aY}\n${aZ}\n${gX}\n${gY}\n${gZ}"
    }

    fun toCSVRow(): String {
        return "${instant},${aX},${aY},${aZ},${gX},${gY},${gZ}\n"
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray?): IMUReading? {
            if (byteArray != null) {
                // Bytes are sent in Little-Endian, but JVM expects Big-Endian
                val buffer = ByteBuffer.wrap(byteArray.reversedArray())
                val aX = buffer.getFloat(20)
                val aY = buffer.getFloat(16)
                val aZ = buffer.getFloat(12)
                val gX = buffer.getFloat(8)
                val gY = buffer.getFloat(4)
                val gZ = buffer.getFloat(0)
                return IMUReading(aX, aY, aZ, gX, gY, gZ)
            }
            return null
        }
    }
}