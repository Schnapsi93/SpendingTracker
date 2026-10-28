package com.aljaz.spendingtracker.domain.charts

import com.aljaz.spendingtracker.domain.model.CategoryTotal

/**
 * One piece of the category chart: a slice of the pie and its line in the list.
 *
 * @property categoryId the category, or null for "Uncategorized" or "Other"
 * @property share fraction of the total, from 0.0 to 1.0
 * @property isOther true for the merged "Other" entry
 */
data class ChartSlice(
    val categoryId: Long?,
    val amountMinor: Long,
    val share: Double,
    val isOther: Boolean = false
)

/**
 * Everything the category chart needs for one period.
 *
 * The pie and the list next to it show exactly the same entries, so every
 * line in the list can be found in the pie and the list adds up to the total.
 *
 * @property totalMinor all expenses in the period
 * @property slices largest first; small categories merged into one "Other" entry (last)
 */
data class CategoryBreakdown(
    val totalMinor: Long,
    val slices: List<ChartSlice>
) {
    companion object {
        val EMPTY = CategoryBreakdown(0, emptyList())
    }
}

/** Turns per-category expense totals into pie slices (also used as the list). */
object CategoryBreakdownCalculator {

    /** Categories below this share are merged into "Other". */
    const val MIN_SLICE_SHARE = 0.05

    fun calculate(
        totals: List<CategoryTotal>,
        minSliceShare: Double = MIN_SLICE_SHARE
    ): CategoryBreakdown {
        val sorted = totals.filter { it.totalMinor > 0 }.sortedByDescending { it.totalMinor }
        val total = sorted.sumOf { it.totalMinor }
        if (total == 0L) return CategoryBreakdown.EMPTY

        val items = sorted.map {
            ChartSlice(it.categoryId, it.totalMinor, it.totalMinor.toDouble() / total)
        }

        // Merge small categories, but only if there are at least two of them.
        // An "Other" with a single category would just hide its name.
        val (big, small) = items.partition { it.share >= minSliceShare }
        val slices = if (small.size >= 2) big + merged(small) else items

        return CategoryBreakdown(total, slices)
    }

    /** Sums several entries into one "Other" entry. */
    private fun merged(parts: List<ChartSlice>) = ChartSlice(
        categoryId = null,
        amountMinor = parts.sumOf { it.amountMinor },
        share = parts.sumOf { it.share },
        isOther = true
    )
}