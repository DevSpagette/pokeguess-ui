package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AuthActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    private val apiUrl = "https://pokeguess-api.onrender.com/user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        logoutButton = findViewById(R.id.logout_button)
        progressBar = findViewById(R.id.progressBar)

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
        val bestMasterTextView = findViewById<TextView>(R.id.bestMasterTextView)
        val bestScoreTextView = findViewById<TextView>(R.id.bestScoreTextView)

        // Check if jwtToken is present in SharedPreferences
        val token = getJwtToken()

        if (token.isNullOrEmpty()) {
            // jwtToken is not present, show the login button
            usernameInput.visibility = View.VISIBLE
            passwordInput.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            usernameTextView.visibility = View.GONE
            bestMasterTextView.visibility = View.GONE
            bestScoreTextView.visibility = View.GONE
        } else {
            profile()
            // jwtToken is present, hide the login button and show the logout button
            usernameInput.visibility = View.GONE
            passwordInput.visibility = View.GONE
            loginButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
            usernameTextView.visibility = View.VISIBLE
            bestMasterTextView.visibility = View.VISIBLE
            bestScoreTextView.visibility = View.VISIBLE
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

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

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }

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
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error while logging in.",
                    Snackbar.LENGTH_LONG
                )
                    .show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val token = json.optString("token")
                    val userId = json.optString("userId")
                    val username = json.optString("username")
                    val bestMaster = json.optLong("bestMaster")
                    val bestScore = json.optLong("bestScore")

                    if (!token.isNullOrEmpty()) {
                        // Save the token to SharedPreferences
                        val sharedPreferences =
                            getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("jwtToken", token)
                        editor.putString("username", username)
                        editor.putString("userId", userId)
                        editor.putLong("bestMaster", bestMaster)
                        editor.putLong("bestScore", bestScore)
                        editor.apply()

                        // Redirect to MainActivity
                        val intent = Intent(this@AuthActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Close the AuthActivity
                    } else {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Error while logging in.",
                            Snackbar.LENGTH_LONG
                        )
                            .show()
                    }
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error while logging in.",
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                }
            }
        })
    }

    private fun profile() {
        val client = OkHttpClient()
        val url = "$apiUrl/profile" // Adjust the URL based on your API endpoints

        val jwtToken = getJwtToken()

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error while fetching user profile.",
                    Snackbar.LENGTH_LONG
                )
                    .show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val bestScore = json.optLong("bestScore")
                    val bestMaster = json.optLong("bestMaster")

                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.remove("bestScore")
                    editor.remove("bestMaster")
                    editor.putLong("bestScore", bestScore)
                    editor.putLong("bestMaster", bestMaster)
                    editor.apply()

                    val usernameTextView = findViewById<TextView>(R.id.usernameTextView)
                    val bestMasterTextView = findViewById<TextView>(R.id.bestMasterTextView)
                    val bestScoreTextView = findViewById<TextView>(R.id.bestScoreTextView)

                    val username = getUsername()

                    runOnUiThread {
                        if (username != null) {
                            usernameTextView.text = "Logged in as: $username"
                            bestMasterTextView.text = "Best score (Classic) : $bestMaster"
                            bestScoreTextView.text = "Best score (Challenge) : $bestScore"
                        }
                    }

                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content), "Error while logging in.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun logout() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
        }
        // Remove the token from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("jwtToken")
        editor.remove("username")
        editor.remove("userId")
        editor.remove("bestScore")
        editor.remove("bestMaster")
        editor.apply()

        Snackbar.make(
            findViewById(android.R.id.content),
            "Logged out successfully.",
            Snackbar.LENGTH_LONG
        ).show()

        // Redirect to MainActivity
        val intent = Intent(this@AuthActivity, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the AuthActivity
    }

    private fun getJwtToken(): String? {
        // Retrieve the jwtToken from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwtToken", null)
    }

    private fun getBestMaster(): Long {
        // Retrieve the best score (classic) from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("bestMaster", 0)
    }

    private fun getBestScore(): Long {
        // Retrieve the best score (challenge) from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("bestScore", 0)
    }

    private fun getUsername(): String? {
        // Retrieve the username from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }

}
