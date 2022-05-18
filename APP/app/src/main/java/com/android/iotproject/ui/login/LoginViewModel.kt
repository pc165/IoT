package com.android.iotproject.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.iotproject.R
import com.android.iotproject.data.LoginRepository
import com.android.iotproject.data.VolleyResponse
import com.android.iotproject.data.model.LoggedInUser
import org.json.JSONObject

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult
    val buttonState: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    fun login(username: String, password: String) {
        buttonState.value = false
        try {
            loginRepository.login(username, password,
                object : VolleyResponse {
                    override fun processFinish(output: String?) {
                        val answer = JSONObject(output!!)
                        loginRepository.setUser(
                            LoggedInUser(answer.getString("access_token"), "")
                        )
                        _loginResult.value =
                            LoginResult(success = LoggedInUserView(displayName = ""))
                        buttonState.value = true

                    }
                }, object : VolleyResponse {
                    override fun processFinish(output: String?) {
                        _loginResult.value = LoginResult(error = R.string.login_failed)
                        buttonState.value = true
                    }
                }
            )
        } catch (e: Exception) {
            buttonState.value = true
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 2
    }

}