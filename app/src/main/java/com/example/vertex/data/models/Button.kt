package com.example.vertex.data.models

import java.io.Serializable

data class Button(
    val type: String,
    val caption: String,
    val formAction: String
): Serializable