package com.mohdroid.authenticator

import android.util.Log
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

data class RegisterRequest(
    val mobileNumber: String
)

data class RegisterResponse(
    val accessToken: String,
    val refreshToken: String
)

object InMemoryCache {
    var mobileNumber: String = ""
}

/**
 * Safe to call from main thread, this api is thread safe
 */
fun register(registerRequest: RegisterRequest): Future<RegisterResponse> {
    Log.d("oAuth", "register($registerRequest)")
    val executor = Executors.newCachedThreadPool()
    return executor.submit(Callable {
        Thread.sleep(1000)
        val accessToken = "access token " + registerRequest.mobileNumber +
                Random().nextInt(100)
        val refreshToken = "refresh token " + registerRequest.mobileNumber +
                Random().nextInt(100)
        InMemoryCache.mobileNumber = registerRequest.mobileNumber
        RegisterResponse(accessToken, refreshToken)
    })
}

/**
 * Safe to call from main thread, this api is thread safe
 */
fun refresh(refreshToken: String): Future<RegisterResponse> {
    Log.d("oAuth", "refresh($refreshToken)")
    val executor = Executors.newCachedThreadPool()
    return executor.submit(Callable {
        Thread.sleep(1000)
        val accessToken = "access token " + InMemoryCache.mobileNumber +
                Random().nextInt(100)
        val newRefreshToken = "refresh token " + InMemoryCache.mobileNumber +
                Random().nextInt(100) + "lastRefresh $refreshToken"
        Log.d("oAuth", "MockAuthenticator > refreshToken: $newRefreshToken")
        RegisterResponse(accessToken, newRefreshToken)
    })
}

