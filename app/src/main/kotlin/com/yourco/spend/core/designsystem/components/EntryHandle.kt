package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme

/**
 * 36x4dp pill with accessible touch target (min 48dp height/width area) and onClick.
 */
@Composable
fun EntryHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val defaultDescription = stringResource(R.string.cd_add_expense)
    val description = contentDescription ?: defaultDescription

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        // Visible 36x4dp pill
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(AppTheme.shapes.pill)
                .background(AppTheme.colors.hairline),
        )
    }
}
