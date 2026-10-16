package com.yourco.spend.feature.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.BigAmount
import com.yourco.spend.core.designsystem.components.CategoryBar
import com.yourco.spend.core.designsystem.components.SegmentedControl
import com.yourco.spend.feature.insight.charts.MonthSparkline
import com.yourco.spend.feature.insight.charts.YearBarChart

/**
 * Insight screen: Day / Month / Year summaries with one number and its categories.
 */
@Composable
fun InsightScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InsightScreenContent(
        state = state,
        modifier = modifier,
        onModeSelected = viewModel::setMode,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
    )
}

@Composable
fun InsightScreenContent(
    state: InsightUiState,
    modifier: Modifier = Modifier,
    onModeSelected: (InsightMode) -> Unit = {},
    onPreviousPeriod: () -> Unit = {},
    onNextPeriod: () -> Unit = {},
) {
    val modes = listOf(
        InsightMode.DAY to stringResource(R.string.insight_day),
        InsightMode.MONTH to stringResource(R.string.insight_month),
        InsightMode.YEAR to stringResource(R.string.insight_year),
    )
    val modeTitles = modes.map { it.second }
    val selectedIndex = modes.indexOfFirst { it.first == state.mode }.coerceAtLeast(0)

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = AppTheme.spacing.xxl + 32.dp),
    ) {
        // Segmented Control (Day / Month / Year)
        SegmentedControl(
            items = modeTitles,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onModeSelected(modes[index].first)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.spacing.screenHorizontal,
                    vertical = AppTheme.spacing.m,
                ),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.l))

        // Big Total
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BigAmount(
                amount = state.amountDigits,
                currencySymbol = state.currencySymbol,
            )
        }

        // VERIFY: Insight stepper layout beneath BigAmount total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousPeriod,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.cd_previous_period),
                    tint = AppTheme.colors.textPrimary,
                )
            }

            Text(
                text = state.periodLabel,
                style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.s),
            )

            IconButton(
                onClick = onNextPeriod,
                enabled = state.canGoForward,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.cd_next_period),
                    tint = if (state.canGoForward) {
                        AppTheme.colors.textPrimary
                    } else {
                        AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.l))

        // Chart section: 12-bar chart in Year mode, sparkline in Month mode
        when (state.mode) {
            InsightMode.YEAR -> {
                YearBarChart(bars = state.yearBars)
                Spacer(modifier = Modifier.height(AppTheme.spacing.l))
            }
            InsightMode.MONTH -> {
                MonthSparkline(points = state.monthPoints)
                Spacer(modifier = Modifier.height(AppTheme.spacing.l))
            }
            InsightMode.DAY -> {
                // Day mode: no chart by default
            }
        }

        // Category breakdown or empty state
        if (state.isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.insight_empty),
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.categories.forEach { category ->
                    val displayName = if (category.nameRes != null) {
                        stringResource(category.nameRes)
                    } else {
                        category.name
                    }
                    CategoryBar(
                        name = displayName,
                        amount = category.formattedAmount,
                        percentage = category.percentage,
                        fraction = category.fraction,
                    )
                }
            }
        }
    }
}
