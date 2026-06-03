package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.cache.SearchCacheKey
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.localization.Strings

class HomeBrowseRulesUseCase {
    fun computeLanguageCodes(
        languages: Set<Strings.Language>,
        mapLanguageToCode: (Strings.Language) -> String
    ): List<String> {
        val codes = languages.map(mapLanguageToCode).toMutableList()
        if (!codes.contains("unknown")) codes.add("unknown")
        return codes
    }

    fun buildBrowseCacheSignature(
        searchQuery: String,
        selectedCategory: String?,
        languageCodes: List<String>,
        sortBy: String?,
        loveRange: ClosedFloatingPointRange<Float>,
        faithRange: ClosedFloatingPointRange<Float>,
        hopeRange: ClosedFloatingPointRange<Float>
    ): String {
        return buildString {
            append("q=").append(searchQuery.trim())
            append("|cat=").append(selectedCategory ?: "")
            append("|love=").append(loveRange.start.toInt()).append("-").append(loveRange.endInclusive.toInt())
            append("|faith=").append(faithRange.start.toInt()).append("-").append(faithRange.endInclusive.toInt())
            append("|hope=").append(hopeRange.start.toInt()).append("-").append(hopeRange.endInclusive.toInt())
            append("|lang=").append(languageCodes.sorted().joinToString(","))
            append("|sort=").append(sortBy ?: "")
        }
    }

    fun isDefaultBrowseState(
        searchQuery: String,
        selectedCategory: String?,
        browseRatingFilters: BrowseRatingFilters,
        minRating: Float,
        maxRating: Float,
        loveRange: ClosedFloatingPointRange<Float>,
        faithRange: ClosedFloatingPointRange<Float>,
        hopeRange: ClosedFloatingPointRange<Float>
    ): Boolean {
        return searchQuery.isBlank() &&
            selectedCategory == null &&
            browseRatingFilters == BrowseRatingFilters() &&
            loveRange.start.toInt() == minRating.toInt() &&
            loveRange.endInclusive.toInt() == maxRating.toInt() &&
            faithRange.start.toInt() == minRating.toInt() &&
            faithRange.endInclusive.toInt() == maxRating.toInt() &&
            hopeRange.start.toInt() == minRating.toInt() &&
            hopeRange.endInclusive.toInt() == maxRating.toInt()
    }

    fun buildRequestKey(key: SearchCacheKey): String {
        return buildString {
            append("browse|q=").append(key.searchQuery)
            append("|cat=").append(key.category ?: "")
            append("|love=").append(key.minLove).append("-").append(key.maxLove)
            append("|faith=").append(key.minFaith).append("-").append(key.maxFaith)
            append("|hope=").append(key.minHope).append("-").append(key.maxHope)
            append("|lang=").append(key.languages.joinToString(","))
            append("|sort=").append(key.sortBy)
            append("|page=").append(key.page)
            append("|per=").append(key.perPage)
        }
    }
}
