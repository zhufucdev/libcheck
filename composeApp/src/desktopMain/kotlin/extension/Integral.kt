package extension

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

fun integral(a: Double, b: Double, step: Double = 0.001, f: (x: Double) -> Double): Double {
    val stepsFloat = (b - a) / step
    val stepsInt = stepsFloat.toInt()

    var y = 0.0
    repeat(stepsInt) {
        val x = a + step * it
        y += f(x) * step
    }
    return y
}

val gaussianDistributionRangeNormalized: (Double) -> Double = { x ->
    9.0 / sqrt(2 * PI) * E.pow(-9 * (x - 0.5).pow(2))
}