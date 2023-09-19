package com.kobera.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.toPx(): Float {
    return toPx(LocalDensity.current)
}

fun Dp.toPx(density: Density): Float {
    val dp = this
    return with(density) {
        dp.toPx()
    }
}

operator fun Dp.times(other: Double): Dp {
    return this.times(other.toFloat())
}