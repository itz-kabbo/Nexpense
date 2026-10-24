package com.nexusbench.nexpense.feature.receipt

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexusbench.nexpense.R
import com.nexusbench.nexpense.core.designsystem.AppMotion
import com.nexusbench.nexpense.core.designsystem.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val prevDayCd = stringResource(R.string.cd_previous_day)
    val nextDayCd = stringResource(R.string.cd_next_day)
    val shareCd = stringResource(R.string.cd_share_receipt)

    val handleShare: () -> Unit = onShare ?: {
        coroutineScope.launch {
            try {
                val imageBitmap = graphicsLayer.toImageBitmap()
                val bitmap = imageBitmap.asAndroidBitmap()
                val uri = withContext(Dispatchers.IO) {
                    ReceiptImageSaver.saveBitmap(context, bitmap, state.selectedDate)
                }
                val shareSubject = context.getString(R.string.app_name) + " - " + state.formattedDate
                val shareIntent = ReceiptImageSaver.createShareIntent(
                    uri = uri,
                    subject = shareSubject,
                )
                val chooser = Intent.createChooser(shareIntent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.w("ReceiptScreen", "Failed to share receipt", e)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .navigationBarsPadding(),
    ) {
        // Top bar with navigation (< Previous, Date, Next >) and Share button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = AppTheme.spacing.screenHorizontal,
                    vertical = AppTheme.spacing.s,
                ),
        ) {
            // Previous day button (36dp circle with 48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .clickable(
                        role = Role.Button,
                        onClick = onPreviousDay,
                        onClickLabel = prevDayCd,
                    )
                    .semantics { contentDescription = prevDayCd },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceVariant)
                        .border(width = 0.5.dp, color = AppTheme.colors.hairline, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Text(
                text = state.formattedDate,
                style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp),
            )

            // Forward day button and Share button grouped on the right
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                val nextEnabled = state.canGoForward
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (nextEnabled) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = onNextDay,
                                    onClickLabel = nextDayCd,
                                )
                            } else {
                                Modifier
                            }
                        )
                        .semantics { contentDescription = nextDayCd },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceVariant.copy(alpha = if (nextEnabled) 1f else 0.6f))
                            .border(
                                width = 0.5.dp,
                                color = AppTheme.colors.hairline.copy(alpha = if (nextEnabled) 1f else 0.6f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = if (nextEnabled) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                val shareEnabled = !state.isEmpty
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (shareEnabled) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = handleShare,
                                    onClickLabel = shareCd,
                                )
                            } else {
                                Modifier
                            }
                        )
                        .semantics { contentDescription = shareCd },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceVariant.copy(alpha = if (shareEnabled) 1f else 0.6f))
                            .border(
                                width = 0.5.dp,
                                color = AppTheme.colors.hairline.copy(alpha = if (shareEnabled) 1f else 0.6f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = if (shareEnabled) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Persistent Receipt Card with thermal print animation (AppMotion.receiptPrintSpec, 350ms)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppTheme.spacing.screenHorizontal,
                    top = AppTheme.spacing.m,
                    end = AppTheme.spacing.screenHorizontal,
                    bottom = AppTheme.spacing.xxl + 32.dp,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = AppMotion.receiptPrintSpec()) +
                        slideInVertically(
                            animationSpec = AppMotion.receiptPrintSpec(),
                            initialOffsetY = { fullHeight -> fullHeight / 8 },
                        )).togetherWith(fadeOut(animationSpec = AppMotion.fadeSpec()))
                },
                label = "ReceiptPrintAnimation",
            ) { animatedState ->
                ReceiptCard(
                    state = animatedState,
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
