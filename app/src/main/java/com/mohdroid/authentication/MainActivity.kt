package com.mohdroid.authentication

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerCallback
import android.accounts.AccountManagerFuture
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mohdroid.authentication.AccountAuthenticator.Companion.ACCOUNT_NAME
import com.mohdroid.authentication.AccountAuthenticator.Companion.ACCOUNT_TYPE
import com.mohdroid.authentication.AccountAuthenticator.Companion.USER_ID
import java.util.concurrent.Executors


class MainActivity : Activity() {

    private lateinit var lvAccount: ListView
    private lateinit var am: AccountManager
    private lateinit var accounts: Array<Account>

    private lateinit var requestedAccount: Account


    companion object {
        const val PERMISSION_REQUEST_USER_ACCESS_TO_THE_ACCOUNT = 1
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 1
        const val AUTH_TOKEN_TYPE: String = "oauth2:https://www.googleapis.com/auth/tasks.readonly"
        const val HUMAN_READABLE_AUTH_TOKEN_TYPE: String = "View your tasks"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lvAccount = findViewById(R.id.listViewAccounts)
        lvAccount.setOnItemClickListener { parent, view, position, id ->
            requestedAccount = accounts[position]
            getAuth(requestedAccount)
        }

        am = AccountManager.get(this) // "this" references the current Context
        accessToAccounts()
    }

    private fun accessToAccounts() {
//        if (SDK_INT >= O) {
//            val intent = AccountManager.newChooseAccountIntent(
//                null,
//                null,
//                arrayOf("com.google"),
//                null,
//                null,
//                null,
//                null
//            )
//            startActivityForResult(intent, 0)
//        } else if (SDK_INT >= M) {
//            readContactsPermission()
//        }
        if (SDK_INT >= M) {
            readContactsPermission()
        }
    }

    @RequiresApi(M)
    private fun readContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
            } else
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    PERMISSIONS_REQUEST_READ_CONTACTS
                )

        }
    }


    private inner class OnTokenAcquired : AccountManagerCallback<Bundle> {

        override fun run(future: AccountManagerFuture<Bundle>) {
            // Get the result of the operation from the AccountManagerFuture.
            try {
                val bundle: Bundle = future.result
                val launch: Intent? = bundle.get(AccountManager.KEY_INTENT) as? Intent
                //Perhaps the user's account has expired and they need to log in again, or perhaps their stored credentials are incorrect. Maybe the account requires two-factor authentication or it needs to activate the camera to do a retina scan.
                launch?.let {
                    Log.d("AAAA", it.data.toString())
                    Log.d("AAAA", it.action.toString())
                    Executors.newSingleThreadExecutor().submit {
                        startActivityForResult(it, PERMISSION_REQUEST_USER_ACCESS_TO_THE_ACCOUNT)
                    }

                    return
                }
                // The token is a named value in the bundle. The name of the value
                // is stored in the constant AccountManager.KEY_AUTHTOKEN.
                val token: String? = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                Log.d("AAAA", "token: $token")
            } catch (e: Exception) {
                Log.d("AAAA", "The user has denied you access to the API, you should handle that")
                //after future.result this exception thrown if the user denied
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            val accountName: String? = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            Log.d("AAAA", "Account Name=$accountName")
            val accountType: String? = data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
            Log.d("AAAA", "Account type=$accountType")

            val accountManager = AccountManager.get(this)
            val accounts = accountManager.accounts
            for (a in accounts) {
                Log.d("AAAA", "type--- " + a.type + " ---- name---- " + a.name)
            }
        }
        if (requestCode == PERMISSION_REQUEST_USER_ACCESS_TO_THE_ACCOUNT && resultCode == RESULT_OK) {
            getAuth(requestedAccount)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] === PackageManager.PERMISSION_GRANTED)
                Log.d("AAAA", "Permission granted")
            else
                Log.d("AAAA", "Permission denied")
            return
        }
    }

    private class OnError : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            Log.d("AAAA", msg.what.toString())
            return true
        }
    }

    private fun getAuth(account: Account) {
        am.getAuthToken(
            account, //account retrieved using getAccountsByType()
            HUMAN_READABLE_AUTH_TOKEN_TYPE, // string that defines the specific type of access your app is asking for auth scope for read-write access to Google Tasks is Manage your tasks.
            Bundle(), // Authenticator-specific options
            true,// Your activity
            OnTokenAcquired(),// Callback called when a token is successfully acquired
            null // Callback called if an error occurs
        )

    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.google -> {
                accounts = am.getAccountsByType("com.google")
                showAccountList(accounts)
            }
            R.id.twitter -> {
                accounts = am.getAccountsByType("com.twitter.android.auth.login")
                showAccountList(accounts)
            }
            //com.osp.app.signin -> samsung account
            R.id.all -> {
                accounts = am.accounts
                showAccountList(accounts)
            }
            R.id.custom -> {
                accounts = am.getAccountsByType(ACCOUNT_TYPE)
                requestedAccount = accounts[0]
                val token_one: String? = am.peekAuthToken(requestedAccount, AccountAuthenticator.AUTH_TOKEN_TYPE_ONE)
                token_one?.let {
                    Log.d("AAAA", it)
                }
                val peekAuthToken: String? = am.peekAuthToken(requestedAccount, AccountAuthenticator.AUTH_TOKEN_TYPE_TWO)
                peekAuthToken?.let {
                    Log.d("AAAA", it)
                }

            }
            R.id.createCustomAccount -> {
                val account = addOrFindAccount("admin_refresh_token")
                am.setUserData(account, USER_ID, "user123")
                am.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE_ONE, "admin_access_token_ONE")
                am.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE_TWO, "admin_access_token_TWO")
            }
        }
    }

    private fun addOrFindAccount(pass: String): Account {
        val accounts = am.getAccountsByType(ACCOUNT_TYPE)
        val account =
            if (accounts.isNotEmpty()) accounts[0] else Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            am.addAccountExplicitly(account, pass, null)
            Log.d("AAAA", "account added!")
        } else {
            am.setPassword(accounts[0], pass)
            Log.d("AAAA", "account pass changed!")
        }
        return account
    }

    private fun showAccountList(accounts: Array<Account>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts)
        lvAccount.adapter = adapter

    }
}