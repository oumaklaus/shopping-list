package com.example.shoppinglist.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.auth.CredentialError
import com.example.shoppinglist.auth.CredentialResult
import com.example.shoppinglist.auth.CredentialValidator
import com.example.shoppinglist.data.AuthRepository
import com.example.shoppinglist.data.RemoteAuthRepository
import com.example.shoppinglist.data.SessionStorage
import kotlinx.coroutines.launch

data class AuthUiState(
    val isRegistering: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: CredentialError? = null,
    val passwordError: CredentialError? = null,
    val message: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val session: SessionStorage
) : ViewModel() {
    private val _uiState = MutableLiveData(AuthUiState())
    val uiState: LiveData<AuthUiState> = _uiState

    private val _authenticated = MutableLiveData<Boolean?>()
    val authenticated: LiveData<Boolean?> = _authenticated

    fun toggleMode() {
        val current = _uiState.value ?: AuthUiState()
        _uiState.value = AuthUiState(isRegistering = !current.isRegistering)
    }

    fun submit(rawEmail: String, rawPassword: String) {
        if (_uiState.value?.isSubmitting == true) return

        val validation = CredentialValidator.validate(rawEmail, rawPassword)
        if (validation is CredentialResult.Invalid) {
            val current = _uiState.value ?: AuthUiState()
            _uiState.value = current.copy(
                emailError = validation.emailError,
                passwordError = validation.passwordError,
                message = null
            )
            return
        }

        validation as CredentialResult.Valid
        val current = _uiState.value ?: AuthUiState()
        _uiState.value = current.copy(
            isSubmitting = true,
            emailError = null,
            passwordError = null,
            message = null
        )

        viewModelScope.launch {
            val result = if (current.isRegistering) {
                repository.register(validation.email, validation.password)
            } else {
                repository.login(validation.email, validation.password)
            }

            result.onSuccess { auth ->
                session.save(auth)
                _uiState.value = current.copy(isSubmitting = false)
                _authenticated.value = true
            }.onFailure { error ->
                _uiState.value = current.copy(
                    isSubmitting = false,
                    message = error.message ?: "Could not connect to the server"
                )
            }
        }
    }

    fun authenticationHandled() {
        _authenticated.value = null
    }

    class Factory(
        private val session: SessionStorage,
        private val repository: AuthRepository = RemoteAuthRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(repository, session) as T
    }
}
