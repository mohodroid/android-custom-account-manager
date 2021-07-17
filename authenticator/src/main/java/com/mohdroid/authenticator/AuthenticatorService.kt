package com.mohdroid.authenticator

import android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log


class AuthenticatorService : Service() {

    companion object {
        private const val TAG: String = "oAuth"
    }

    lateinit var auth: AccountAuthenticator
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate service")
        auth = AccountAuthenticator(this)
    }

    /*
    look on Transport, an inner class of AbstractAccountAuthenticator and read about AIDL for inter-process communication
     */
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind service: intent $intent")
        intent.action?.let {
            return if (it == ACTION_AUTHENTICATOR_INTENT) auth.iBinder else null
        }
        return null
    }

}

