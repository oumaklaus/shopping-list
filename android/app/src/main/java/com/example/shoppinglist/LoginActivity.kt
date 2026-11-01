package com.example.shoppinglist

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.shoppinglist.auth.CredentialError
import com.example.shoppinglist.data.SharedPreferencesSessionStorage
import com.example.shoppinglist.data.SessionStorage
import com.example.shoppinglist.databinding.ActivityLoginBinding
import com.example.shoppinglist.ui.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionStorage
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory(session) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SharedPreferencesSessionStorage(this)
        if (!session.token.isNullOrBlank()) {
            openShoppingList()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSubmit.setOnClickListener { submit() }
        binding.buttonSwitchMode.setOnClickListener { viewModel.toggleMode() }
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        viewModel.uiState.observe(this) { state ->
            binding.title.text = getString(
                if (state.isRegistering) R.string.create_account else R.string.welcome_back
            )
            binding.subtitle.text = getString(
                if (state.isRegistering) R.string.register_subtitle else R.string.login_subtitle
            )
            binding.buttonSubmit.text = getString(
                if (state.isRegistering) R.string.create_account else R.string.log_in
            )
            binding.buttonSwitchMode.text = getString(
                if (state.isRegistering) R.string.have_account else R.string.need_account
            )
            binding.layoutEmail.error = state.emailError?.toMessage()
            binding.layoutPassword.error = state.passwordError?.toMessage()
            binding.progressAuth.isVisible = state.isSubmitting
            binding.buttonSubmit.isEnabled = !state.isSubmitting
            binding.buttonSwitchMode.isEnabled = !state.isSubmitting

            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.authenticated.observe(this) { authenticated ->
            if (authenticated == true) {
                viewModel.authenticationHandled()
                openShoppingList()
            }
        }

        if (intent.getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            Snackbar.make(binding.root, R.string.session_expired, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun submit() {
        viewModel.submit(
            binding.inputEmail.text?.toString().orEmpty(),
            binding.inputPassword.text?.toString().orEmpty()
        )
    }

    private fun CredentialError.toMessage(): String = getString(
        when (this) {
            CredentialError.INVALID_EMAIL -> R.string.error_email_invalid
            CredentialError.INVALID_PASSWORD -> R.string.error_password_invalid
        }
    )

    private fun openShoppingList() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_SESSION_EXPIRED = "session_expired"
    }
}
