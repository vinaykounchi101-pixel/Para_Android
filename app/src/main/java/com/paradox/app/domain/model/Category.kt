package com.paradox.app.domain.model

data class Category(
    val id: String,
    val profileId: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false
) {
    companion object {
        fun createStarterCategories(profileId: String): List<Category> {
            return listOf(
                Category(
                    id = "${profileId}_cat_food",
                    profileId = profileId,
                    name = "Food & Dining",
                    iconName = "restaurant",
                    colorHex = "#7E8B67", // Sage Olive
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_transport",
                    profileId = profileId,
                    name = "Transportation",
                    iconName = "directions_car",
                    colorHex = "#5B7C99", // Dusty Slate Blue
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_shopping",
                    profileId = profileId,
                    name = "Shopping",
                    iconName = "shopping_bag",
                    colorHex = "#82708F", // Pastel Mauve
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_bills",
                    profileId = profileId,
                    name = "Bills & Utilities",
                    iconName = "receipt_long",
                    colorHex = "#B8864E", // Muted Amber
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_health",
                    profileId = profileId,
                    name = "Healthcare",
                    iconName = "medical_services",
                    colorHex = "#BA6D68", // Dusty Rose
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_entertainment",
                    profileId = profileId,
                    name = "Entertainment",
                    iconName = "movie",
                    colorHex = "#628E75", // Soft Mint
                    isDefault = true
                ),
                Category(
                    id = "${profileId}_cat_other",
                    profileId = profileId,
                    name = "Other",
                    iconName = "category",
                    colorHex = "#9AA3B5", // Slate Grey
                    isDefault = true
                )
            )
        }
    }
}
