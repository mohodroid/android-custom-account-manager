package com.mohdroid.authentication

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT
import android.accounts.AccountManager.KEY_BOOLEAN_RESULT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log


class AuthenticatorService : Service() {


    private val binder = LocalBinder()

    lateinit var auth: AccountAuthenticator
    override fun onCreate() {
        super.onCreate()
        Log.d("AAAA", "onCreate service")
        auth = AccountAuthenticator(this)
    }


    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action.equals(ACTION_AUTHENTICATOR_INTENT)) auth.iBinder else null
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): AuthenticatorService = this@AuthenticatorService
    }


}

class AccountAuthenticator(private val context: Context?) : AbstractAccountAuthenticator(context) {

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? {
        return null
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? {
        return null
    }

    /*
     * The user has requested to add a new account to the system. We return an intent that will launch our login screen
    * if the user has not logged in yet, otherwise our activity will just pass the user's credentials on to the account
    * manager.
    */

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        TODO("Not yet implemented")
    }

    // See /Applications/android-sdk-macosx/samples/android-18/legacy/SampleSyncAdapter/src/com/example/android/samplesync/authenticator/Authenticator.java
    // Also take a look here https://github.com/github/android/blob/d6ba3f9fe2d88967f56e9939d8df7547127416df/app/src/main/java/com/github/mobile/accounts/AccountAuthenticator.java
    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        val bundle = Bundle()
        // If the caller requested an authToken type we don't support, then return an error
        if (authTokenType != AUTH_TOKEN_TYPE_TWO || authTokenType != AUTH_TOKEN_TYPE_ONE) {
            Log.d("AAAA", "invalid authTokenType $authTokenType")
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            return bundle
        }
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken
        val accountManager = AccountManager.get(context)
        // Password is storing the refresh token
        val password = accountManager.getPassword(account)
        if (password != null) {
            Log.d("AAAA", "Trying to refresh access token")
            try {
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account?.name)
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE)
                bundle.putString(AccountManager.KEY_AUTHTOKEN, "admin_access_token")
                accountManager.setPassword(account, "admin_refresh_token")
                return bundle
            } catch (e: Exception) {
                Log.d("AAAA", "Failed refreshing token.")
            }
        }

        // Otherwise... start the login intent
//
//        // Otherwise... start the login intent
//        Timber.i("Starting login activity")
//        val intent = Intent(context, AuthenticatorActivity::class.java)
//        intent.putExtra(LoginFragment.PARAM_USERNAME, account!!.name)
//        intent.putExtra(LoginFragment.PARAM_AUTHTOKEN_TYPE, authTokenType)
//        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
//        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String?): String? {
        return if (ACCOUNT_TYPE == authTokenType) authTokenType else null
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        return null
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        val result = Bundle()
        result.putBoolean(KEY_BOOLEAN_RESULT, false)
        return result
    }

    companion object {
        // Account type id
        const val ACCOUNT_TYPE = "com.mohdroid.authentication"

        // Account name
        const val ACCOUNT_NAME = "mohdroid"

        // Auth token type
        const val AUTH_TOKEN_TYPE_ONE = "$ACCOUNT_TYPE.ONE"

        // Auth token type
        const val AUTH_TOKEN_TYPE_TWO = "$ACCOUNT_TYPE.TWO"
        const val USER_ID = "user_id"
    }

}