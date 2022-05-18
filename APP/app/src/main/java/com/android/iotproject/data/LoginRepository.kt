package com.android.iotproject.data

import android.content.Context
import android.util.Log
import com.android.iotproject.R
import com.android.iotproject.data.model.LoggedInUser
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

interface VolleyResponse {
    fun processFinish(output: String?)
}

class LoginRepository(val context: Context) {

//    var user: LoggedInUser? = null

    companion object {
        var user: LoggedInUser? = null
    }

    val isLoggedIn: Boolean
        get() = user != null

    fun logout() {
        user = null
    }

    fun getUser(): LoggedInUser {
        return user!!
    }

    fun setUser(u: LoggedInUser) {
        user = u
    }

    fun login(
        username: String,
        password: String,
        responseOK: VolleyResponse,
        responseERROR: VolleyResponse
    ) {
        // handle login
        val url = context.getString(R.string.login_url)

        val response = Response.Listener<String> {
            Log.i("Data Source", it.toString())
            val r = JSONObject(it)
            user = LoggedInUser(r.getString("access_token"), "")
            responseOK.processFinish(it)
        }
        val error = Response.ErrorListener {
            Log.i("Data Source", it.toString())
            responseERROR.processFinish(it.toString())
        }
        val req = object : StringRequest(Method.POST, url, response, error) {
            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["username"] = username
                params["password"] = password
                return params
            }
        }
        ResquestInstance.getInstance(context).addToRequestQueue(req)
    }

}