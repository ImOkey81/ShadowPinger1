package com.example.shadow.core.network

import kotlinx.coroutines.delay

interface BackendClient {
    suspend fun register(login: String, password: String): BackendResult<Unit>
    suspend fun authorize(login: String, password: String): BackendResult<String>
}

sealed class BackendResult<out T> {
    data class Success<T>(val data: T) : BackendResult<T>()
    data class Failure(val message: String) : BackendResult<Nothing>()
}

class FakeBackendClient : BackendClient {
    override suspend fun register(login: String, password: String): BackendResult<Unit> {
        delay(300)
        return if (login.isBlank() || password.isBlank()) {
            BackendResult.Failure("Missing credentials")
        } else {
            BackendResult.Success(Unit)
        }
    }

    override suspend fun authorize(login: String, password: String): BackendResult<String> {
        delay(300)
        return if (login.isBlank() || password.isBlank()) {
            BackendResult.Failure("Missing credentials")
        } else {
            BackendResult.Success("token_${login.hashCode()}")
        }
    }
}
