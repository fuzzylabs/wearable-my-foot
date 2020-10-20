package ai.fuzzylabs.wearablemyfoot.math

import org.junit.jupiter.api.Test

internal class MathKtTest {

    fun linearTestFunction(x: Double) = 2.0*x + 1.0

    @Test
    fun cumtrapzTest() {
        val x = listOf(0.0, 1.0, 1.5, 2.0, 3.3)
        val y = x.map{ linearTestFunction(it) }
        val output = cumtrapz(y, x).toList()
        val expected = listOf(2.0, 3.75, 6.0, 14.19)
        output.zip(expected).forEach { assert(it.first == it.second) }
    }
}