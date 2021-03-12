package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private const val SIGN_IN_REQUEST_CODE = 1
        private val TAG = AuthenticationActivity::class.java.simpleName
    }

    private val viewModel: AuthenticationViewModel by viewModels {
        AuthenticationViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityAuthenticationBinding>(
            this,
            R.layout.activity_authentication
        )
        viewModel.authenticationState.observe(this, ::onAuthStateChanged)
        binding.loginButton.setOnClickListener { launchSignInFlow() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
            if (requestCode == SIGN_IN_REQUEST_CODE)
                navigateToMainScreen()
    }

    private fun navigateToMainScreen() {
        val intent = RemindersActivity.newIntent(this)
        startActivity(intent)
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            SIGN_IN_REQUEST_CODE
        )
    }

    private fun onAuthStateChanged(state: AuthenticationViewModel.AuthenticationState) {
        when (state) {
            AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> navigateToMainScreen()
            AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> logUnauthenticatedUser()
        }
    }

    private fun logUnauthenticatedUser() {
        Log.i(TAG, "Unauthenticated user")
    }

}
