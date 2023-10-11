package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AuthActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button

    private val apiUrl = "https://pokeguess-api.onrender.com/user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        logoutButton = findViewById(R.id.logout_button)

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)

        // Check if jwtToken is present in SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwtToken", null)

        if (token.isNullOrEmpty()) {
            // jwtToken is not present, show the login button
            usernameInput.visibility = View.VISIBLE
            passwordInput.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            usernameTextView.visibility = View.GONE
        } else {
            // jwtToken is present, hide the login button and show the logout button
            usernameInput.visibility = View.GONE
            passwordInput.visibility = View.GONE
            loginButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
            usernameTextView.visibility = View.VISIBLE

            // You can also show the username of the current user (fetch it from your server)
            val username = sharedPreferences.getString("username", null)// Replace with your server call to get the username
            if (username != null) {
                // Set the username in a TextView or any other view
                usernameTextView.text = "Logged in as: $username"
            }
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            // Make a login request
            login(username, password)
        }

        logoutButton.setOnClickListener {
            logout()
        }

        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun login(username: String, password: String) {
        val client = OkHttpClient()
        val url = "$apiUrl/login" // Adjust the URL based on your API endpoints

        val requestBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Snackbar.make(findViewById(android.R.id.content), "Error while logging in.", Snackbar.LENGTH_LONG)
                    .show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val token = json.optString("token")
                    val userId = json.optString("userId")
                    val username = json.optString("username")

                    if (!token.isNullOrEmpty()) {
                        // Save the token to SharedPreferences
                        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("jwtToken", token)
                        editor.putString("username", username)
                        editor.putString("userId", userId)
                        editor.apply()

                        // Redirect to MainActivity
                        val intent = Intent(this@AuthActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Close the AuthActivity
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "Error while logging in.", Snackbar.LENGTH_LONG)
                            .show()
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Error while logging in.", Snackbar.LENGTH_LONG)
                        .show()
                }
            }
        })
    }

    private fun logout() {
        // Remove the token from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("jwtToken")
        editor.remove("username")
        editor.remove("userId")
        editor.apply()

        Snackbar.make(findViewById(android.R.id.content), "Logged out successfully.", Snackbar.LENGTH_LONG)
            .show()

        // Redirect to MainActivity
        val intent = Intent(this@AuthActivity, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the AuthActivity
    }

}
