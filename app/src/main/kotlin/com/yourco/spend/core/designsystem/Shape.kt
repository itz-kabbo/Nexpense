package com.yourco.spend.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// VERIFY: Corner radii proposed in section 3.3 and UI.md redesign specification
@Immutable
data class AppShapes(
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val chip: Shape = CircleShape,
    val pill: Shape = RoundedCornerShape(999.dp),
    val small: Shape = RoundedCornerShape(8.dp),
    val dock: Shape = RoundedCornerShape(20.dp),
)
