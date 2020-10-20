package ai.fuzzylabs.wearablemyfoot.math

fun cumtrapz(y: Iterable<Double>, x: Iterable<Double>? = null): Sequence<Double> {
    var segments = y.zipWithNext().map { (it.first + it.second) / 2 }
    if (x != null) {
        val xDiff = x.zipWithNext().map { it.second - it.first }
        segments = segments.zip(xDiff).map { it.first * it.second }
    }

    return sequence {
        var sum = 0.0
        for (segment in segments) {
            sum += segment
            yield(sum)
        }
    }
}