package com.example.shadow.core.contracts

data class ClientErrorReport(
    val hwid: String,
    val jobId: String?,
    val errorType: String,
    val message: String,
    val fatal: Boolean,
    val timestamp: String,
)
