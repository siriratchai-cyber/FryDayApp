package com.example.frydayapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MenuItemModel(
    val menu_id: String,
    var name: String? = null,
    var price: Double = 0.0,
    @SerialName("image_url")
    val image_url: String? = null,
    val category: String? = null,
    val cate_id: String? = null,
    @SerialName("Details")
    var details: String? = null,
    val options: List<Option>? = null,
    @SerialName("is_popular")
    val is_popular: Boolean = false
    // ✅ ไม่มี menuAddons แล้ว
)