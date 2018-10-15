package org.sagebase.crf.step.active

import java.util.*

data class HeartBeatSample @JvmOverloads constructor(
        val uptime: Double,
        val timestamp: Double,
        val red: Float,
        val green: Float,
        val blue: Float,
        val redLevel: Double,
        val isCoveringLens: Boolean,
        val timestampDate: Date? = null)