package com.yourco.spend.core.designsystem.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourco.spend.core.designsystem.AppTheme

/**
 * Segmented control selector, e.g. for Day / Month / Year.
 */
@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.pill)
            .background(AppTheme.colors.surface)
            .selectableGroup()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.background else AppTheme.colors.surface,
                animationSpec = tween(durationMillis = AppTheme.motion.FADE_DURATION_MS),
                label = "SegmentBackground",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                animationSpec = tween(durationMillis = AppTheme.motion.FADE_DURATION_MS),
                label = "SegmentTextColor",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(AppTheme.shapes.pill)
                    .background(backgroundColor)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = {
                            if (!isSelected) {
                                // Haptic feedback on segment change
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                onSelectedIndexChange(index)
                            }
                        },
                    )
                    .padding(vertical = AppTheme.spacing.s, horizontal = AppTheme.spacing.m),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.label.copy(
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
