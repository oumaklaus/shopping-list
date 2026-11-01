package com.example.shoppinglist.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object AuthSession {
    @Volatile
    var token: String? = null
        private set

    private val _expired = MutableLiveData(false)
    val expired: LiveData<Boolean> = _expired

    @Volatile
    private var clearPersistentSession: (() -> Unit)? = null

    @Synchronized
    fun configure(initialToken: String?, clearPersistentSession: () -> Unit) {
        token = initialToken
        this.clearPersistentSession = clearPersistentSession
        if (initialToken != null) _expired.postValue(false)
    }

    @Synchronized
    fun activate(newToken: String) {
        token = newToken
        _expired.postValue(false)
    }

    @Synchronized
    fun expire(requestToken: String) {
        if (token == null || token != requestToken) return
        clearPersistentSession?.invoke()
        token = null
        _expired.postValue(true)
    }

    @Synchronized
    fun clear() {
        token = null
        _expired.postValue(false)
    }

    fun expirationHandled() {
        _expired.postValue(false)
    }
}

interface SessionStorage {
    val token: String?
    val email: String?
    fun save(auth: AuthResponse)
    fun clear()
}

class SharedPreferencesSessionStorage(context: Context) : SessionStorage {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    init {
        AuthSession.configure(preferences.getString(KEY_TOKEN, null)) {
            preferences.edit().clear().apply()
        }
    }

    override val token: String?
        get() = preferences.getString(KEY_TOKEN, null)

    override val email: String?
        get() = preferences.getString(KEY_EMAIL, null)

    override fun save(auth: AuthResponse) {
        preferences.edit()
            .putString(KEY_TOKEN, auth.token)
            .putString(KEY_EMAIL, auth.user.email)
            .apply()
        AuthSession.activate(auth.token)
    }

    override fun clear() {
        preferences.edit().clear().apply()
        AuthSession.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "shopping_list_session"
        const val KEY_TOKEN = "auth_token"
        const val KEY_EMAIL = "user_email"
    }
}
