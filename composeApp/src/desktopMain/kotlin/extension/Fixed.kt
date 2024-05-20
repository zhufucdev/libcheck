package extension

import kotlin.math.roundToInt

fun Float.toFixed(precision: Int): Float {
    return (this * precision).roundToInt() / precision.toFloat()
}
