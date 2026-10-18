package com.godson.vpn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Système de rayons du design system Godson.
val RadiusSmall = 10.dp
val RadiusMedium = 16.dp
val RadiusLarge = 20.dp
val RadiusPill = 999.dp

val GodsonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
    large = RoundedCornerShape(RadiusLarge),
    extraLarge = RoundedCornerShape(28.dp)
)
