package com.paradox.app.domain.usecase.category

import com.paradox.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FuzzyCategoryMatcherTest {

    private lateinit var matcher: FuzzyCategoryMatcher
    private val categories = listOf(
        Category(
            id = "cat_food",
            profileId = "p1",
            name = "Food & Dining",
            iconName = "restaurant",
            colorHex = "#7E8B67",
            isDefault = true
        ),
        Category(
            id = "cat_transport",
            profileId = "p1",
            name = "Transportation",
            iconName = "directions_car",
            colorHex = "#5B7C99",
            isDefault = false
        ),
        Category(
            id = "cat_groceries",
            profileId = "p1",
            name = "Groceries",
            iconName = "shopping_cart",
            colorHex = "#628E75",
            isDefault = false
        )
    )

    @Before
    fun setUp() {
        matcher = FuzzyCategoryMatcher()
    }

    @Test
    fun `exact match resolves to correct category`() {
        val result = matcher.matchCategory("Transportation", categories)
        assertTrue(result.isExactMatch)
        assertEquals("cat_transport", result.matchedCategory?.id)
        assertEquals(null, result.suggestedNewCategoryName)
    }

    @Test
    fun `case-insensitive substring match resolves to correct category`() {
        val result = matcher.matchCategory("food", categories)
        assertEquals("cat_food", result.matchedCategory?.id)
        assertEquals(null, result.suggestedNewCategoryName)
    }

    @Test
    fun `alias dictionary lookup resolves to parent category`() {
        val uberResult = matcher.matchCategory("Uber", categories)
        assertEquals("cat_transport", uberResult.matchedCategory?.id)

        val swiggyResult = matcher.matchCategory("Swiggy", categories)
        assertEquals("cat_food", swiggyResult.matchedCategory?.id)

        val zeptoResult = matcher.matchCategory("Zepto", categories)
        assertEquals("cat_groceries", zeptoResult.matchedCategory?.id)
    }

    @Test
    fun `unknown category suggests dynamic creation with default fallback`() {
        val result = matcher.matchCategory("crypto investment", categories)
        assertFalse(result.isExactMatch)
        assertEquals("cat_food", result.matchedCategory?.id) // default fallback
        assertEquals("Crypto Investment", result.suggestedNewCategoryName)
    }
}
