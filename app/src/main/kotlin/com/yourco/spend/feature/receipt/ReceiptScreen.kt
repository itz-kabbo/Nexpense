package com.yourco.spend.feature.receipt

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import kotlinx.coroutines.launch

@Composable
fun ReceiptScreen(
    modifier: Modifier = Modifier,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ReceiptScreenContent(
        state = state,
        modifier = modifier,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
    )
}

@Composable
fun ReceiptScreenContent(
    state: ReceiptUiState,
    modifier: Modifier = Modifier,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onShare: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val handleShare: () -> Unit = onShare ?: {
        coroutineScope.launch {
            try {
                val imageBitmap = graphicsLayer.toImageBitmap()
                val bitmap = imageBitmap.asAndroidBitmap()
                val uri = ReceiptImageSaver.saveBitmap(context, bitmap, state.selectedDate)
                val shareIntent = ReceiptImageSaver.createShareIntent(
                    uri = uri,
                    subject = "Mote - ${state.formattedDate}",
                )
                val chooser = Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
                // Ignore error if share activity cannot be started in test or headless environment
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        // Top bar with navigation (< Previous, Date, Next >) and Share button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = AppTheme.spacing.s,
                    vertical = AppTheme.spacing.s,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.cd_previous_day),
                    tint = AppTheme.colors.textPrimary,
                )
            }

            Text(
                text = state.formattedDate,
                style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.xs),
            )

            IconButton(
                onClick = onNextDay,
                enabled = state.canGoForward,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.cd_next_day),
                    tint = if (state.canGoForward) {
                        AppTheme.colors.textPrimary
                    } else {
                        AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    },
                )
            }

            IconButton(
                onClick = handleShare,
                enabled = !state.isEmpty,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.cd_share_receipt),
                    tint = if (!state.isEmpty) {
                        AppTheme.colors.textPrimary
                    } else {
                        AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    },
                )
            }
        }

        // Empty state or Scrollable Receipt Card
        if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.receipt_empty),
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AppTheme.spacing.screenHorizontal,
                        vertical = AppTheme.spacing.m,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                ReceiptCard(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        },
                )
            }
        }
    }
}
