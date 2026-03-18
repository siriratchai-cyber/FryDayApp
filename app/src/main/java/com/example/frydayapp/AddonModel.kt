package com.example.frydayapp

import kotlinx.serialization.Serializable

@Serializable
data class AddonModel(
    val addon_id: String,
    val name: String,
    val price: Double? = 0.0,
    val available: Boolean? = true
)