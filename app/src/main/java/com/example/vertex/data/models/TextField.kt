package com.example.vertex.data.models

import java.io.Serializable

data class TextField(
    val type: String,
    val caption: String,
    val attribute: String,
    val required: Boolean,
    val suggestions: List<String>?
): Serializable
