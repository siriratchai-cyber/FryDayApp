package com.example.frydayapp

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val cus_id: String,
    val username: String?,
    val email: String?,
    val tel: String?
)