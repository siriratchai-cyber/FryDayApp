package com.example.frydayapp

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseResponse(
    val message: String? = null,
    val error: String? = null
)