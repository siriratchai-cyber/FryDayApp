package com.example.frydayapp

data class CartItem(
    val menuId: String,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val options: List<Option> = emptyList(),
    val specialInstructions: String = "",
    val imageUrl: String? = null  // เพิ่ม imageUrl
) {
    val totalPrice: Double
        get() = (price + options.sumOf { it.price }) * quantity
}