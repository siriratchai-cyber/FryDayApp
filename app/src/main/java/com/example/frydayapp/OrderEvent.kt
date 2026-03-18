package com.example.frydayapp

import java.util.Calendar

data class OrderEvent(
    val day: Calendar,
    val orderCount: Int,
    val hasOrders: Boolean
)