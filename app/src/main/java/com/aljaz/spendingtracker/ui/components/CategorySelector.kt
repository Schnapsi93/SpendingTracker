package com.aljaz.spendingtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aljaz.spendingtracker.domain.model.Category

/**
 * Category chips that wrap onto as many lines as needed.
 *
 * @param categories the categories to offer (already filtered by type)
 * @param selectedId id of the selected category, or null for none
 * @param onCategoryClick called with the id of the tapped category
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedId: Long?,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onCategoryClick(category.id) },
                label = { Text(category.name) },
                leadingIcon = { ColorDot(category.colorArgb) }
            )
        }
    }
}

/** Small filled circle in a category's color. */
@Composable
fun ColorDot(colorArgb: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color = Color(colorArgb), shape = CircleShape)
    )
}