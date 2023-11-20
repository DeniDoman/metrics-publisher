package com.domanskii.serialization

import kotlinx.serialization.Serializable


// Requests
@Serializable
data class PostMetricsRequest(
    val commitSha: String, val name: String, val value: Double, val units: String, val threshold: Double = 0.0, val isIncreaseBad: Boolean = true
)
