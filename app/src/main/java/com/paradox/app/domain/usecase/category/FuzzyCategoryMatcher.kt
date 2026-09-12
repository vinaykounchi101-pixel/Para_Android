package com.paradox.app.domain.usecase.category

import com.paradox.app.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

data class CategoryMatchResult(
    val matchedCategory: Category?,
    val isExactMatch: Boolean,
    val suggestedNewCategoryName: String?
)

@Singleton
class FuzzyCategoryMatcher @Inject constructor() {

    private val categoryAliases = mapOf(
        "food" to listOf("dining", "restaurant", "cafe", "swiggy", "zomato", "eat", "lunch", "dinner", "breakfast", "meal"),
        "transportation" to listOf("travel", "uber", "ola", "rapido", "cab", "taxi", "metro", "bus", "train", "fuel", "petrol", "flight"),
        "groceries" to listOf("supermarket", "blinkit", "zepto", "instamart", "vegetables", "fruits", "mart", "provisions"),
        "utilities" to listOf("bills", "electricity", "water", "wifi", "internet", "recharge", "gas", "mobile"),
        "entertainment" to listOf("movies", "netflix", "spotify", "prime", "cinema", "gaming", "concert"),
        "shopping" to listOf("clothes", "amazon", "flipkart", "myntra", "fashion", "shoes", "electronics"),
        "healthcare" to listOf("medical", "medicine", "pharmacy", "doctor", "hospital", "apollo", "health")
    )

    fun matchCategory(
        suggestedName: String?,
        availableCategories: List<Category>
    ): CategoryMatchResult {
        if (suggestedName.isNullOrBlank() || availableCategories.isEmpty()) {
            val fallback = availableCategories.firstOrNull { it.isDefault } ?: availableCategories.firstOrNull()
            return CategoryMatchResult(fallback, false, null)
        }

        val target = suggestedName.trim().lowercase()

        // Tier 1: Exact Match (Case-Insensitive)
        val exactMatch = availableCategories.find { it.name.trim().equals(target, ignoreCase = true) }
        if (exactMatch != null) {
            return CategoryMatchResult(exactMatch, true, null)
        }

        // Tier 2: Substring Match
        val substringMatch = availableCategories.find {
            val catName = it.name.trim().lowercase()
            catName.contains(target) || target.contains(catName)
        }
        if (substringMatch != null) {
            return CategoryMatchResult(substringMatch, false, null)
        }

        // Tier 3: Keyword / Alias Dictionary Lookup
        for (category in availableCategories) {
            val catLower = category.name.trim().lowercase()
            for ((key, aliases) in categoryAliases) {
                if (catLower.contains(key) || key.contains(catLower)) {
                    if (aliases.any { target.contains(it) || it.contains(target) }) {
                        return CategoryMatchResult(category, false, null)
                    }
                }
            }
        }

        // Tier 4: No match found in user's categories -> Suggest Dynamic Category Creation
        val formattedSuggestion = suggestedName.trim().split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }

        val defaultFallback = availableCategories.firstOrNull { it.isDefault } ?: availableCategories.firstOrNull()
        return CategoryMatchResult(
            matchedCategory = defaultFallback,
            isExactMatch = false,
            suggestedNewCategoryName = formattedSuggestion
        )
    }
}
