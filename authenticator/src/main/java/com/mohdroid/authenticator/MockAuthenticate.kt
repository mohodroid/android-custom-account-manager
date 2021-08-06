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

object MockAuthenticate {
    /**
     * Safe to call from main thread, this api is thread safe
     */
    var mobileNumber: String? = null
    var numberOfRegistered: Int = 0
    fun register(registerRequest: RegisterRequest): Future<RegisterResponse> {
        Log.d("oAuth", "register($registerRequest)")
        mobileNumber = registerRequest.mobileNumber
        numberOfRegistered++
        val executor = Executors.newCachedThreadPool()
        return executor.submit(Callable {
            Thread.sleep(1000)
            val accessToken = "access token - $mobileNumber - $numberOfRegistered"
            val refreshToken = "refresh token - $mobileNumber - $numberOfRegistered"
            val registerResponse = RegisterResponse(accessToken, refreshToken)
            Log.d("oAuth", "MockAuthenticator > register > RegisterResponse: $registerResponse ")
            registerResponse
        })
    }

    /**
     * Safe to call from main thread, this api is thread safe
     *
     */
    fun refresh(refreshToken: String): Future<RegisterResponse> {
        Log.d("oAuth", "refresh($refreshToken)")
        val executor = Executors.newCachedThreadPool()
        return executor.submit(Callable {
            Thread.sleep(1000)
            val registerResponse = if (Random().nextBoolean()) {
                RegisterResponse("", "")
            } else {
                val accessToken = "access token - $mobileNumber - $numberOfRegistered"
                val newRefreshToken = "refresh token - $mobileNumber - $numberOfRegistered"
                RegisterResponse(accessToken, newRefreshToken)
            }
            Log.d("oAuth", "MockAuthenticator > register > RegisterResponse: $registerResponse ")
            registerResponse
        })
    }
}


