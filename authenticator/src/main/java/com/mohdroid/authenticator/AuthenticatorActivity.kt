package com.mohdroid.authenticator

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.mohdroid.authenticator.AccountAuthenticator.Companion.AUTHTOKEN_TYPE
import com.mohdroid.authenticator.databinding.ActivityAuthenticatorBinding
import java.util.*


class AuthenticatorActivity : AccountAuthenticatorActivity() {

    companion object {
        const val PARAM_BUNDLE_OPTIONS: String = "BUNDLE_OPTIONS"
        const val PARAM_BUTTON_NAME: String = "BUTTON_NAME"

        /** The tag used to log to adb console. */
        const val TAG: String = "oAuth"
        const val PARAM_ACCOUNT_TYPE: String = "ACCOUNT_TYPE"
        const val PARAM_AUTHTOKEN_TYPE: String = "AUTHTOKEN_TYPE"
        const val PARAM_IS_ADDING_NEW_ACCOUNT: String = "IS_ADDING_ACCOUNT"
        const val PARAM_ACCOUNT_NAME: String = "ACCOUNT_NAME"
        const val PARAM_PASS = "pass"
    }

    private lateinit var accountManager: AccountManager
    private var authTokenType: String? = null

    private lateinit var binding: ActivityAuthenticatorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        accountManager = AccountManager.get(baseContext)
        authTokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE)
        val bundle: Bundle? = intent.getBundleExtra(PARAM_BUNDLE_OPTIONS)
        bundle?.let {
            val buttonName = it.getString(PARAM_BUTTON_NAME)
            buttonName?.let { name ->
                binding.btnSubmit.text = name
            }
        }
        binding.btnSubmit.setOnClickListener {
            val mobileNumber = binding.etMobileNumber.text.toString()
            Task(mobileNumber).execute()
        }

    }

    /**
     * Todo("Refactor to another soloution for prevent memory-leak")
     */
    inner class Task(private val mobileNumber: String) : AsyncTask<Void?, Void?, Intent?>() {
        override fun doInBackground(vararg params: Void?): Intent {
            val future = register(RegisterRequest(mobileNumber))
            val authToken = future.get().accessToken
            val refreshToken = future.get().refreshToken
            val data = Bundle()
            val res = Intent()
            data.putString(AccountManager.KEY_ACCOUNT_NAME, mobileNumber)
            data.putString(
                AccountManager.KEY_ACCOUNT_TYPE,
                intent.getStringExtra(PARAM_ACCOUNT_TYPE)
            )
            data.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            data.putString(PARAM_PASS, refreshToken)
            res.putExtras(data)
            return res
        }

        override fun onPostExecute(intent: Intent?) {
            finishLogin(intent)
        }

    }

    @SuppressLint("MissingPermission")
    private fun finishLogin(intent: Intent?) {
        Log.d(TAG, "finishLogin()")
        val accountName = intent!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        val refreshToken = intent.getStringExtra(PARAM_PASS)
        //username, your_account_type
        val account = Account(accountName, accountType)
        if (getIntent().getBooleanExtra(PARAM_IS_ADDING_NEW_ACCOUNT, false)) {
            //You add a new account to the device - that’s a tricky part. When creating an account, the auth-token is NOT saved immediately to the AccountManager, it needs to be saved explicitly. That’s why I’m setting the auth-token explicitly after adding the new account to the AccountManager. Failing to do so, makes the AccountManager do another trip to the server, when the getAuthToken method is called, and authenticating the user again.
            Log.d(TAG, "finishLogin() > addAccountExplicitly()")
            val authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)
            val authTokenType: String = AUTHTOKEN_TYPE
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            // userData =  custom data, such as API key to your service

            accountManager.addAccountExplicitly(account, refreshToken, null)
            accountManager.setAuthToken(account, authTokenType, authToken)
        } else {
            Log.d(TAG, "finishLogin() > setPassword()")
            //Existing account with an invalidated auth-token - in this case, we already have a record on the AccountManager. The new auth-token will replace the old one without any action by you, but if the user had changed his password for that, you need to update the AccountManager with the new password too. This can be seen in the code above.
            accountManager.setPassword(account, refreshToken)
        }
        //returns the information back to the Authenticator.
        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)
        finish()
    }
}