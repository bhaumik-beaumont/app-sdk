package researchstack.presentation.util

import researchstack.util.logDataSync

private const val TAG = "UnitConverter"

fun Float.lbsToKg(isMetric: Boolean): Float {
    val result = if (isMetric) {
        this
    } else this * LBS_TO_KG_RATIO
    logDataSync("lbsToKg input=$this isMetric=$isMetric -> $result", tag = TAG)
    return result
}

fun Float.kgToLbs(isMetric: Boolean): Float {
    val result = if (isMetric) {
        this
    } else this / LBS_TO_KG_RATIO
    logDataSync("kgToLbs input=$this isMetric=$isMetric -> $result", tag = TAG)
    return result
}

fun Float.toDecimalFormat(digit: Int): Float {
    val format = "%.${digit}f".format(this)
    return format.toFloat().also {
        logDataSync("toDecimalFormat value=$this digits=$digit -> $it", tag = TAG)
    }
}


fun Float.ftToCm(isMetric: Boolean): Float {
    val result = if (isMetric) {
        this
    } else this * FT_TO_CM_RATIO
    logDataSync("ftToCm input=$this isMetric=$isMetric -> $result", tag = TAG)
    return result
}

fun Float.cmToFt(isMetric: Boolean): Float {
    val result = if (isMetric) {
        this
    } else this / FT_TO_CM_RATIO
    logDataSync("cmToFt input=$this isMetric=$isMetric -> $result", tag = TAG)
    return result
}

private const val LBS_TO_KG_RATIO = 0.45359236f
private const val FT_TO_CM_RATIO = 30.48f
