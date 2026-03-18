package com.example.frydayapp

import kotlinx.serialization.Serializable

@Serializable
data class PromotionModel(
    val pro_id: String,
    val pro_name: String?,
    val description: String? = null,
    val img_url: String?,
    val price: Double? = null
)