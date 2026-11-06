package com.zensa.api.dto

data class SsoRequest(
    val idToken: String,
    val provider: String = "google"
)
