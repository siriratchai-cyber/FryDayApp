package com.example.frydayapp

import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val res_id: String,
    val username: String? = null,
    val email: String? = null,
    val tel: String? = null
)