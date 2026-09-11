package com.paradox.app.data.mapper

import com.paradox.app.data.local.entity.PaymentMethodEntity
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.PaymentMethodType

fun PaymentMethodEntity.toDomain(): PaymentMethod {
    val typeEnum = try {
        PaymentMethodType.valueOf(type)
    } catch (_: Exception) {
        PaymentMethodType.CUSTOM
    }
    return PaymentMethod(
        id = id,
        profileId = profileId,
        type = typeEnum,
        label = label,
        isCustom = isCustom
    )
}

fun PaymentMethod.toEntity(): PaymentMethodEntity {
    return PaymentMethodEntity(
        id = id,
        profileId = profileId,
        type = type.name,
        label = label,
        isCustom = isCustom
    )
}
