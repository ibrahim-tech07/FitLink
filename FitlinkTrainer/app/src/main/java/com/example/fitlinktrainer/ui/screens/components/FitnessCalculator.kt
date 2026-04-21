package com.example.fitlinktrainer.ui.screens.components

import kotlin.math.roundToInt

object FitnessCalculator {

    fun calculateCalories(
        met: Double,
        weightKg: Double,
        durationSeconds: Int
    ): Int {

        val hours = durationSeconds / 3600.0

        val calories = met * weightKg * hours

        return calories.roundToInt().coerceAtLeast(1)
    }
}