package com.yourco.spend.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.components.BigAmount
import com.yourco.spend.core.designsystem.components.CategoryBar
import com.yourco.spend.core.designsystem.components.CategoryChip
import com.yourco.spend.core.designsystem.components.DashedRule
import com.yourco.spend.core.designsystem.components.EntryHandle
import com.yourco.spend.core.designsystem.components.EntryRow
import com.yourco.spend.core.designsystem.components.LeaderRow
import com.yourco.spend.core.designsystem.components.SegmentedControl
import com.yourco.spend.core.designsystem.components.rememberPerforatedShape

/**
 * Debug-only gallery screen displaying all design tokens and primitives together with sample data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentGalleryScreen(
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    var selectedSegmentIndex by remember { mutableIntStateOf(1) } // Default: Month
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var handleClickCount by remember { mutableIntStateOf(0) }

    val segmentItems = listOf(
        stringResource(R.string.period_day),
        stringResource(R.string.period_month),
        stringResource(R.string.period_year),
    )

    val categories = listOf(
        stringResource(R.string.category_food),
        stringResource(R.string.category_groceries),
        stringResource(R.string.category_transport),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .verticalScroll(scrollState)
            .padding(vertical = AppTheme.spacing.xl),
    ) {
        // Gallery Title
        Text(
            text = stringResource(R.string.gallery_title),
            style = AppTheme.typography.headline.copy(color = AppTheme.colors.textPrimary),
            modifier = Modifier.padding(horizontal = AppTheme.spacing.screenHorizontal),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Tokens (Colors)
        SectionHeader(title = stringResource(R.string.gallery_section_tokens))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            ColorSwatch(color = AppTheme.colors.background, label = "bg")
            ColorSwatch(color = AppTheme.colors.surface, label = "surf")
            ColorSwatch(color = AppTheme.colors.paper, label = "paper")
            ColorSwatch(color = AppTheme.colors.textPrimary, label = "text1")
            ColorSwatch(color = AppTheme.colors.textSecondary, label = "text2")
            ColorSwatch(color = AppTheme.colors.hairline, label = "hair")
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Big Amount
        SectionHeader(title = stringResource(R.string.gallery_section_big_amount))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.screenHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            BigAmount(amount = "74.50", currencySymbol = "$")
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Segmented Control
        SectionHeader(title = stringResource(R.string.gallery_section_segmented_control))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.screenHorizontal),
        ) {
            SegmentedControl(
                items = segmentItems,
                selectedIndex = selectedSegmentIndex,
                onSelectedIndexChange = { selectedSegmentIndex = it },
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Category Chips
        SectionHeader(title = stringResource(R.string.gallery_section_category_chip))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            categories.forEachIndexed { index, name ->
                CategoryChip(
                    categoryName = name,
                    selected = index == selectedCategoryIndex,
                    onClick = { selectedCategoryIndex = index },
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Entry Handle
        SectionHeader(title = stringResource(R.string.gallery_section_entry_handle))
        EntryHandle(onClick = { handleClickCount++ })

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Entry Rows
        SectionHeader(title = stringResource(R.string.gallery_section_entry_row))
        EntryRow(
            note = stringResource(R.string.sample_note_coffee),
            category = stringResource(R.string.category_food),
            amount = "$4.50",
            onClick = {},
        )
        EntryRow(
            note = stringResource(R.string.sample_note_lunch),
            category = stringResource(R.string.category_food),
            amount = "$18.00",
            onClick = {},
        )
        EntryRow(
            note = stringResource(R.string.sample_note_groceries),
            category = stringResource(R.string.category_groceries),
            amount = "$52.00",
            onClick = {},
            showDivider = false,
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Category Bars
        SectionHeader(title = stringResource(R.string.gallery_section_category_bar))
        CategoryBar(
            name = stringResource(R.string.category_food),
            amount = "$22.50",
            percentage = 30,
            fraction = 0.30f,
        )
        CategoryBar(
            name = stringResource(R.string.category_groceries),
            amount = "$52.00",
            percentage = 70,
            fraction = 0.70f,
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

        // Section: Dashed Rule & Perforated Receipt Card
        SectionHeader(title = stringResource(R.string.gallery_section_perforated_shape))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spacing.screenHorizontal),
            contentAlignment = Alignment.Center,
        ) {
            val perforatedShape = rememberPerforatedShape(toothWidth = 6.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(perforatedShape)
                    .background(AppTheme.colors.paper)
                    .padding(vertical = AppTheme.spacing.l, horizontal = AppTheme.spacing.l),
            ) {
                Text(
                    text = stringResource(R.string.receipt_sample_title),
                    style = AppTheme.typography.receiptTitle.copy(color = AppTheme.colors.textPrimary),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.m))

                LeaderRow(
                    label = stringResource(R.string.sample_note_coffee),
                    amount = "4.50",
                )
                LeaderRow(
                    label = stringResource(R.string.sample_note_lunch),
                    amount = "18.00",
                )
                LeaderRow(
                    label = stringResource(R.string.sample_note_groceries),
                    amount = "52.00",
                )

                Spacer(modifier = Modifier.height(AppTheme.spacing.m))
                DashedRule(modifier = Modifier.padding(vertical = AppTheme.spacing.s))
                Spacer(modifier = Modifier.height(AppTheme.spacing.s))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.receipt_sample_total),
                        style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
                    )
                    Text(
                        text = "74.50",
                        style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTheme.typography.title.copy(color = AppTheme.colors.textSecondary),
        modifier = Modifier.padding(
            horizontal = AppTheme.spacing.screenHorizontal,
            vertical = AppTheme.spacing.s,
        ),
    )
}

@Composable
private fun ColorSwatch(color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = AppTheme.spacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppTheme.shapes.small)
                .background(color)
                .border(0.5.dp, AppTheme.colors.hairline, AppTheme.shapes.small),
        )
        Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
        Text(
            text = label,
            style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun ComponentGalleryPreviewLight() {
    AppTheme(darkTheme = false) {
        ComponentGalleryScreen()
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun ComponentGalleryPreviewDark() {
    AppTheme(darkTheme = true) {
        ComponentGalleryScreen()
    }
}

@Preview(name = "2.0 Font Scale", fontScale = 2.0f, showBackground = true)
@Composable
private fun ComponentGalleryPreviewFontScale2() {
    AppTheme(darkTheme = false) {
        ComponentGalleryScreen()
    }
}
