package com.mohdroid.authentication

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mohdroid.authentication.databinding.ActivityMainBinding
import com.mohdroid.authenticator.AccountAuthenticator.Companion.ACCOUNT_TYPE
import com.mohdroid.authenticator.AccountAuthenticator.Companion.AUTHTOKEN_TYPE
import com.mohdroid.authenticator.AuthenticatorActivity
import java.util.concurrent.Executors


class MainActivity : Activity() {

    private lateinit var am: AccountManager
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val TAG: String = "oAuth"
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        am = AccountManager.get(this) // "this" references the current Context
        binding.addAccount.setOnClickListener {
            addNewAccount(ACCOUNT_TYPE, AUTHTOKEN_TYPE)
        }
        binding.getAuthTokenByFeature.setOnClickListener {
            getTokenForAccountCreateIfNeeded(ACCOUNT_TYPE, AUTHTOKEN_TYPE)
        }
        binding.invalidateAuthToken.setOnClickListener {
            showAccountPicker(ACCOUNT_TYPE, true)
        }
        binding.getAuthToken.setOnClickListener {
            showAccountPicker(ACCOUNT_TYPE, false)
        }
        binding.getOthersAccounts.setOnClickListener {
            if (Build.VERSION.SDK_INT >= M) {
                if (readContactsPermission() != 0) return@setOnClickListener
            }
            showAccountPicker(null, false)
        }
    }

    /**
     * Show all the accounts registered on the account manager by this package name.
     * consider if type of thr account requested created by this package name, no permission required,
     * otherwise should access read-contact-permission to see accounts created by other apps!
     * the result will show in the list view below to buttons in the page.
     * @param accountType type of the account registered in account manager
     */
    private fun showAccountPicker(accountType: String?, invalidateAccount: Boolean) {
        val availableAccounts = am.getAccountsByType(accountType)
        if (availableAccounts.isEmpty()) {
            showMessage("No available accounts")
            return
        }
        val names = arrayListOf<String>()
        availableAccounts.forEach {
            names.add(it.name)
        }
        Log.d(TAG, "showAccountPicker > accountNames: $names")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Pick account")
        builder.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, names)
        ) { dialog, which ->
            if (invalidateAccount)
                invalidateAuthToken(availableAccounts[which], AUTHTOKEN_TYPE)
            else
                getExistingAccountAuthToken(availableAccounts[which], AUTHTOKEN_TYPE)
        }
        builder.show()
    }

    /**
     * Add new account to the account manager
     * @param accountType
     * @param authTokenType
     */
    private fun addNewAccount(accountType: String, authTokenType: String) {
        val accountOptions = Bundle()
        accountOptions.putString(AuthenticatorActivity.PARAM_BUTTON_NAME, "login")
        am.addAccount(accountType, authTokenType, null, accountOptions, this, { future ->
            try {
                val result = future?.result
                showMessage("Account was created")
                Log.d(TAG, "AddNewAccount Bundle is $result")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }
        }, null)
    }

    /**
     * Invalidate the auth token for the account
     * @param account
     * @param authTokenType
     *
     */
    private fun invalidateAuthToken(account: Account, authTokenType: String) {
        val future: AccountManagerFuture<Bundle> =
            am.getAuthToken(account, authTokenType, null, this, null, null)
        Executors.newCachedThreadPool().execute {
            try {
                val result = future.result
                val authToken = result.getString(AccountManager.KEY_AUTHTOKEN)
                am.invalidateAuthToken(account.type, authToken)
                showMessage("${account.name} invalidated")
                Log.d(TAG, "invalidateAuthToken success")
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                showMessage(e.message)
                Log.d(TAG, "invalidateAuthToken failed")
            }
        }
    }

    /**
     * Get the auth token for an existing account on the AccountManager
     * @param account
     */
    private fun getExistingAccountAuthToken(account: Account, authTokenType: String) {
        val authToken: AccountManagerFuture<Bundle> = am.getAuthToken(
            account, //account retrieved using getAccountsByType()
            authTokenType, // string that defines the specific type of access your app is asking for auth scope for read-write access to Google Tasks is Manage your tasks.
            null, // Authenticator-specific options
            this,// Your activity
            null,// Callback called when a token is successfully acquired
            null // Callback called if an error occurs
        )
        var token: String?
        val newCachedThreadPool = Executors.newCachedThreadPool()
        newCachedThreadPool.execute {
            try {
                val result = authToken.result
                val launch: Intent? = result.get(AccountManager.KEY_INTENT) as? Intent
                launch?.let {
                    startActivityForResult(it, 0)
                    return@execute
                }
                token = result.getString(AccountManager.KEY_AUTHTOKEN)
                showMessage(if (token != null) "SUCCESS!\ntoken: $token" else "FAIL")
                Log.d(TAG, "GetToken Bundle is $result")

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }
        }
    }

    /**
     * Get an auth token for the account.
     * If not exist - add it and then return its auth token.
     * If one exist - return its auth token.
     * If more than one exists - show a picker and return the select account's auth token.
     * @param accountType
     * @param authTokenType
     */
    private fun getTokenForAccountCreateIfNeeded(accountType: String, authTokenType: String) {
        val future = am.getAuthTokenByFeatures(
            accountType,
            authTokenType,
            null,
            this,
            null,
            null,
            null,
            null
        )
        Executors.newCachedThreadPool().execute {
            try {
                val result = future.result
                val authToken = result.getString(AccountManager.KEY_AUTHTOKEN)
                showMessage(if (authToken != null) "SUCCESS!\ntoken: $authToken" else "FAIL")
                Log.d(
                    TAG,
                    "getTokenForAccountCreateIfNeeded > GetTokenForAccount Bundle is  $result"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }

        }

    }

    /**
     * Utility method for show a message with toast
     * @param message  message content to show
     */
    private fun showMessage(message: String?) {
        if (message == null) return
        if (TextUtils.isEmpty(message)) return
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(M)
    private fun readContactsPermission(): Int {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) return 0

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS
            )
        ) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Read accounts permission")
            builder.setPositiveButton(android.R.string.ok, null)
            builder.setMessage("Please enable access to accounts.")
            builder.setOnDismissListener {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    PERMISSIONS_REQUEST_READ_CONTACTS
                )
            }
            builder.show()
            return 2
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
            return 1
        }
    }

    /**
     * After request to access read-contact-permission if user grant access showAccounts otherwise don't do!
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult > Permission granted")
                showAccountPicker(null, false)
            }
            else Log.d(TAG, "onRequestPermissionsResult > Permission denied")
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            showAccountPicker(ACCOUNT_TYPE, false)
        }
    }
}