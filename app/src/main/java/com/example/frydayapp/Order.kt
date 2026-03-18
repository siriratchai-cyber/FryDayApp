package com.example.frydayapp

data class Order(
    val orderId: String,
    val cus_id: String,
    val username: String,
    val phoneNumber: String = "",
    val items: List<CartItem>,
    val total: Double,
    val orderDate: String,
    val orderTime: Long = System.currentTimeMillis(),
    val pickupTime: String,
    val pickupTimestamp: Long = 0,  // ✅ เพิ่มฟิลด์นี้
    val status: String,
    val paymentMethod: String,
    val restaurantName: String,
    val restaurantAddress: String,
    val restaurantPhone: String,
    val paymentTime: Long = 0
)