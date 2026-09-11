package com.paradox.app.data.mapper

import com.paradox.app.data.local.entity.CategoryEntity
import com.paradox.app.domain.model.Category

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        profileId = profileId,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isCustom = isCustom,
        isDefault = isDefault
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        profileId = profileId,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isCustom = isCustom,
        isDefault = isDefault
    )
}
