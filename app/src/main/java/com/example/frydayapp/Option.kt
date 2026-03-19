package com.example.frydayapp

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

@KSerializable
data class Option(
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val isSelected: Boolean = false
) : Serializable