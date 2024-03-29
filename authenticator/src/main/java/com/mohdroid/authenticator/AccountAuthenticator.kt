package com.mohdroid.authenticator

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log

class AccountAuthenticator(
    // Authentication Service context
    private val context: Context
) : AbstractAccountAuthenticator(context) {


    /**
     * The user wants log-in and add a new account to the system. We return an
     * intent that will launch our login screen if the user has not logged in
     * yet, otherwise our activity will just pass the user's credentials on to
     * the account manager.
     */
    @SuppressLint("MissingPermission")
    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle {
        Log.d(TAG, "addAccount()");
        val am = AccountManager.get(context)
        val accountsByType = am.getAccountsByType(ACCOUNT_TYPE)
        if (accountsByType.isNotEmpty()) {
            //if account is already exist , consider it as an error.
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_CODE, "403")
            result.putString(
                AccountManager.KEY_ERROR_MESSAGE,
                context.getString(R.string.error_account_exists)
            )
            return result
        }


        return showAuthActivity(true, accountType, authTokenType, response, options)
    }

    @SuppressLint("MissingPermission")
    private fun accountExist(account: Account): Boolean {
        val am = AccountManager.get(context)
        val accounts = am.getAccountsByType(ACCOUNT_TYPE)
        for (ac in accounts) {
            if (ac == account) {
                return true
            }
        }
        return false
    }
    /**
     * THIS method calls when user wants to get token from accountManager
     */
    @SuppressLint("MissingPermission")
    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account,
        authTokenType: String?,
        options: Bundle?
    ): Bundle {
        Log.d(TAG, "getAuthToken( options$options )");
        // When the specified account doesn't exist, consider it as an error.
        if (!accountExist(account)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_CODE, "403");
            result.putString(AccountManager.KEY_ERROR_MESSAGE,
                context.getString(R.string.error_account_not_exists));
        }

        // If the caller requested an authToken type we don't support, then return an error
        if (authTokenType != AUTHTOKEN_TYPE) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            Log.d(TAG, "getAuthToken() > returned: $result");
            return result
        }
        val callerPackageName = options?.getString("androidPackageName")
        Log.d(TAG, "getAuthToken() > callerPackageName(): $callerPackageName");
        if (callerPackageName == null) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "empty bundle option")
            Log.d(TAG, "getAuthToken() > returned: $result");
            return result
        }
        val packageManager = context.packageManager
        val debugSign =
            packageManager.checkSignatures("com.mohdroid.authentication.debug", callerPackageName)
        val releaseSign =
            packageManager.checkSignatures("com.mohdroid.authentication.release", callerPackageName)
        Log.d(TAG, "getAuthToken() > checkSignatures(): $debugSign , $releaseSign");
        if (debugSign < PackageManager.SIGNATURE_MATCH && releaseSign < PackageManager.SIGNATURE_MATCH) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "access denied")
            Log.d(TAG, "getAuthToken() > returned: $result");
            return result
        }
        val accountManager = AccountManager.get(context)
        val password = accountManager.getPassword(account)
        if (password != null) {
            // this is mock for getAccessToken from server
            val future = MockAuthenticate.refresh(password)
            while (!future.isDone) {
                Log.d(TAG, "getAuthToken() > trying to refresh current access token ...")
                Thread.sleep(300)
            }
            val accessToken = future.get().accessToken
            val refreshToken = future.get().refreshToken
            //means successfully refresh the access-token
            //we should update access and refresh token
            if (!TextUtils.isEmpty(accessToken)) {
                val result = Bundle()
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                result.putString(AccountManager.KEY_AUTHTOKEN, accessToken)
                accountManager.clearPassword(account)
                accountManager.setPassword(account, refreshToken)
                return result
            }
        }
        Log.d(TAG, "getAuthToken > displaying AuthenticatorActivity")
        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        return showAuthActivity(false, account.type, authTokenType, response, options)

    }

    private fun showAuthActivity(
        isNewAccount: Boolean,
        accountType: String?,
        authTokenType: String?,
        response: AccountAuthenticatorResponse?,
        options: Bundle?
    ): Bundle {
        val intent = Intent(context, AuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AuthenticatorActivity.PARAM_ACCOUNT_TYPE, accountType)
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType)
        intent.putExtra(AuthenticatorActivity.PARAM_IS_ADDING_NEW_ACCOUNT, isNewAccount)
        intent.putExtra(AuthenticatorActivity.PARAM_BUNDLE_OPTIONS, options)
        val result = Bundle()
        result.putParcelable(AccountManager.KEY_INTENT, intent)
        return result
    }

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? {
        Log.d(TAG, "editProperties()")
        throw UnsupportedOperationException()
    }

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? {
        Log.d(TAG, "confirmCredentials()");
        return null;
    }

    override fun getAuthTokenLabel(authTokenType: String?): String? {
        Log.d(TAG, "getAuthTokenLabel()");
        /* Detail Message to show in request user access page for account token*/
        if (authTokenType == AUTHTOKEN_TYPE)
            return AUTHTOKEN_LABEL
        return null
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        Log.d(TAG, "updateCredentials()");
        return null;
    }

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        Log.d(TAG, "hasFeatures()");
        val result = Bundle()
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        return result
    }

    companion object {
        /**
         * Account type string.
         */
        const val ACCOUNT_TYPE = BuildConfig.ACCOUNT_TYPE

        /**
         * Authtoken type string. represent the specific type of access your app is asking for.
         * For instance the auth scope for the read-only access to Google Tasks is View Your tasks
         * for our example is access-token
         */
        const val AUTHTOKEN_TYPE = ACCOUNT_TYPE

        /**
         *  message show when ask user for grant access to account token
         */
        const val AUTHTOKEN_LABEL = "full access to account"

        /** The tag used to log to adb console. **/
        const val TAG: String = "oAuth"
    }

}