package com.project.pokeguess

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private var status = "..."
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set a click listener for the "Go to Leaderboard" button
        val classicButton = findViewById<Button>(R.id.classic_mode_button)
        val challengeButton = findViewById<Button>(R.id.challenge_mode_button)
        val leaderboardButton = findViewById<Button>(R.id.leaderboard_button)
        val quitButton = findViewById<Button>(R.id.quit_button)

        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)
        val serverStatusText = findViewById<TextView>(R.id.serverStatusText)
        val serverStatusView = findViewById<View>(R.id.statusDot)
        serverStatusText.text = "API status: $status"
        serverStatusView.setBackgroundResource(R.drawable.yellow_dot)

        leaderboardButton.setOnClickListener(View.OnClickListener {
            // Create an Intent to navigate to LeaderboardActivity
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        })

        challengeButton.setOnClickListener(View.OnClickListener {
            // Create an Intent to navigate to ChallengeActivity
            val intent = Intent(this, ChallengeActivity::class.java)
            startActivity(intent)
        })

        refreshButton.setOnClickListener(View.OnClickListener {
            checkServerStatus()
        })

        quitButton.setOnClickListener {
            finish()
        }

        checkServerStatus()
    }

    private fun checkServerStatus() {
        val serverStatusText = findViewById<TextView>(R.id.serverStatusText)
        val serverStatusView = findViewById<View>(R.id.statusDot)

        runOnUiThread {
            status = "..."
            serverStatusText.text = "API status: $status"
            serverStatusView.setBackgroundResource(R.drawable.yellow_dot)
        }

        val client = OkHttpClient()
        val url = "$apiUrl/"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    runOnUiThread {
                        status = "off"
                        serverStatusText.text = "API status: $status"
                        serverStatusView.setBackgroundResource(R.drawable.red_dot)
                    }
                } else {
                    e.printStackTrace()
                    runOnUiThread {
                        status = "Asleep"
                        serverStatusText.text = "API status: $status"
                        serverStatusView.setBackgroundResource(R.drawable.yellow_dot)
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    status = "on"
                    serverStatusText.text = "API status: $status"
                    serverStatusView.setBackgroundResource(R.drawable.green_dot)
                }
            }
        })
    }

}