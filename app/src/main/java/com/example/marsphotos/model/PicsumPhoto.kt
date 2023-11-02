package com.example.marsphotos.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PicsumPhoto(
    val id: String = "",
    val author: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val url: String = "",
    val download_url: String = ""
)