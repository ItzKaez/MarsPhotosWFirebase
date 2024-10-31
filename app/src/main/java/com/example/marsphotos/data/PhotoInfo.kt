package com.example.marsphotos.data

data class PhotoInfo(
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = ""
)