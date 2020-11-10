package ai.fuzzylabs.wearablemyfoot.util

/**
 * 3D vector
 *
 * @param[x] X, or the first component
 * @param[y] Y, or the second component
 * @param[z] Z, or the third component
 */
data class DoubleVector3D(val x: Double, val y: Double, val z: Double) {
    fun toCSVRow(): String = "$x,$y,$z"

    fun toTypedArray(): DoubleArray = doubleArrayOf(x, y, z)

    companion object {
        fun zero(): DoubleVector3D = DoubleVector3D(0.0, 0.0, 0.0)
    }
}