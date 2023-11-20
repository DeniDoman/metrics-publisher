package com.domanskii.common

data class Metric(
    val commitSha: String,
    val name: String,
    val value: Double,
    val units: String,
    val threshold: Double,
    val isReference: Boolean,
    val isIncreaseBad: Boolean
)