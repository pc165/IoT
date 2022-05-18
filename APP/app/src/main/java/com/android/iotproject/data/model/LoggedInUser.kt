package com.android.iotproject.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val access_token: String,
    val displayName: String
)