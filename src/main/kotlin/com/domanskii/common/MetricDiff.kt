package com.domanskii.common

import java.text.DecimalFormat

data class MetricDiff(
    val actual: Metric,
    val reference: Metric?,
) {
    val diff
        get() = if (reference != null) DecimalFormat("#.##").format(actual.value - reference.value).toDouble() else null
}