package extension

import model.Configurations
import model.Reader

fun Reader.getClearCredit(configurations: Configurations) =
    creditability + (configurations.tiers[tier]?.baseCredit ?: 0f)

fun calculateCredit(reader: Reader, configurations: Configurations, step: Float, positive: Boolean): Double {
    val credit = reader.getClearCredit(configurations) * 1.0
    return if (!positive) {
        reader.creditability - integral(
            a = credit - step,
            b = credit,
            f = gaussianDistributionRangeNormalized
        )
    } else {
        reader.creditability + integral(
            a = credit,
            b = credit + step,
            f = gaussianDistributionRangeNormalized
        )
    }
}