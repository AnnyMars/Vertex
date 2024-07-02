package com.example.vertex.data.models

import java.io.Serializable

data class Form(
    val text: List<TextField>?,
    val buttons: List<Button>?
): Serializable