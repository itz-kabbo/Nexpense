package com.yourco.spend.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.yourco.spend.core.designsystem.components.SegmentedControl
import com.yourco.spend.feature.insight.InsightScreen
import com.yourco.spend.feature.receipt.ReceiptScreen
import com.yourco.spend.feature.record.RecordScreen
import com.yourco.spend.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class RightPaneTab { RECEIPT, INSIGHT }

// VERIFY: Horizontal pager with 3 pages on compact screens; two-pane layout on medium and expanded widths (>= 600dp) per GUIDE.md section 3.4
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(onNavigateBack = { showSettings = false })
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            if (maxWidth >= 600.dp) {
                TwoPaneRoot(
                    onNavigateToSettings = {
                        if (onNavigateToSettings != null) {
                            onNavigateToSettings()
                        } else {
                            showSettings = true
                        }
                    },
                )
            } else {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
                val recordSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.PartiallyExpanded,
                    skipHiddenState = true,
                )
                val coroutineScope = rememberCoroutineScope()
                val isRecordSheetExpanded = pagerState.currentPage == 0 &&
                    (recordSheetState.targetValue == SheetValue.Expanded || recordSheetState.currentValue == SheetValue.Expanded)

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            0 -> RecordScreen(
                                sheetState = recordSheetState,
                                onNavigateToSettings = {
                                    if (onNavigateToSettings != null) {
                                        onNavigateToSettings()
                                    } else {
                                        showSettings = true
                                    }
                                },
                            )
                            1 -> ReceiptScreen()
                            2 -> InsightScreen()
                        }
                    }

                    if (!isRecordSheetExpanded) {
                        ThreeDotIndicator(
                            pagerState = pagerState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp),
                            onDotClick = { targetPage ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TwoPaneRoot(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    recordPane: @Composable () -> Unit = {
        RecordScreen(
            onNavigateToSettings = onNavigateToSettings,
            forceCenteredEntryDialog = true,
        )
    },
    receiptPane: @Composable () -> Unit = { ReceiptScreen() },
    insightPane: @Composable () -> Unit = { InsightScreen() },
) {
    var rightPaneTab by rememberSaveable { mutableStateOf(RightPaneTab.RECEIPT) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        // Left pane: Record screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            recordPane()
        }

        // Hairline vertical divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight()
                .background(AppTheme.colors.hairline),
        )

        // Right pane: Receipt / Insight with segmented tab switcher
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = AppTheme.spacing.s),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentedControl(
                    items = listOf(
                        stringResource(R.string.nav_receipt),
                        stringResource(R.string.nav_insight),
                    ),
                    selectedIndex = if (rightPaneTab == RightPaneTab.RECEIPT) 0 else 1,
                    onSelectedIndexChange = { index ->
                        rightPaneTab = if (index == 0) RightPaneTab.RECEIPT else RightPaneTab.INSIGHT
                    },
                    modifier = Modifier.widthIn(max = 280.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (rightPaneTab) {
                    RightPaneTab.RECEIPT -> receiptPane()
                    RightPaneTab.INSIGHT -> insightPane()
                }
            }
        }
    }
}

@Composable
internal fun ThreeDotIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onDotClick: (Int) -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pagerState.pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val color = if (isSelected) {
                AppTheme.colors.textPrimary
            } else {
                AppTheme.colors.hairline
            }
            val pageDescription = stringResource(R.string.cd_page_n, index + 1)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClickLabel = pageDescription,
                    ) { onDotClick(index) }
                    .semantics {
                        contentDescription = pageDescription
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}
