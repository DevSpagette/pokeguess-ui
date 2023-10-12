package com.project.pokeguess

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val handler = Handler(Looper.getMainLooper())
    private val imageSwapDelay = 3000L // Delay in milliseconds for image change
    private val homeImage = mutableListOf<String>()

    private lateinit var homePokemonImageView: ImageView
    private var rng = 0
    private var currentIndex = 0
    private var jwtToken: String? = null
    private var status = "..."


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        homePokemonImageView = findViewById(R.id.mainPageImage)

        homeImage.addAll(generatePokemonSprite())
        swapImagesPeriodically()

        // Retrieve the jwtToken from SharedPreferences
        jwtToken = getJwtToken()

        // Set a click listener for the "Go to Leaderboard" button
        val classicButton = findViewById<Button>(R.id.classic_mode_button)
        val challengeButton = findViewById<Button>(R.id.challenge_mode_button)
        val leaderboardButton = findViewById<Button>(R.id.leaderboard_button)
        val userButton = findViewById<ImageButton>(R.id.userButton)
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

        userButton.setOnClickListener {
            // Create an Intent to navigate to ChallengeActivity
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }

        challengeButton.setOnClickListener {
            if (jwtToken != null) {
                // Create an Intent to navigate to ChallengeActivity
                val intent = Intent(this, ChallengeActivity::class.java)
                startActivity(intent)
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please log in to use this feature", Snackbar.LENGTH_LONG)
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }
        }

        classicButton.setOnClickListener {
            if (jwtToken != null) {
                Snackbar.make(findViewById(android.R.id.content), "Classic mod not available switch to challenge", Snackbar.LENGTH_LONG)
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
                // Create an Intent to navigate to ChallengeActivity
                val intent = Intent(this, ChallengeActivity::class.java)
                startActivity(intent)
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please log in to use this feature", Snackbar.LENGTH_LONG)
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }
        }

        refreshButton.setOnClickListener {
            checkServerStatus()
        }

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
            serverStatusText.text = "API status$status"
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

    private fun getJwtToken(): String? {
        // Retrieve the jwtToken from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwtToken", null)
    }

    private fun generatePokemonSprite(): MutableList<String>{
        val homeImageView = findViewById<ImageView>(R.id.mainPageImage)
        val list = mutableListOf<String>()

        for (i in 1..10){
            rng = (1..1006).random()
            val imageUrl = "$apiUrl/sprite/$rng"

            list.add(imageUrl)
        }
        return list
    }

    private fun swapImagesPeriodically() {
        handler.post(object : Runnable {
            override fun run() {
                // Create a fade-out animation
                val fadeOutAnimation = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out_animation)
                homePokemonImageView.startAnimation(fadeOutAnimation)
                handler.postDelayed({
                    // Create a fade-in animation
                    val fadeInAnimation =
                        AnimationUtils.loadAnimation(applicationContext, R.anim.fade_animation)
                    homePokemonImageView.startAnimation(fadeInAnimation)

                    // Update the image source
                    runOnUiThread {
                        Glide.with(this@MainActivity).load(homeImage[currentIndex])
                            .into(homePokemonImageView)
                    }

                    // Increment the index, or reset if it exceeds the array length
                    currentIndex = (currentIndex + 1) % homeImage.size
                },fadeOutAnimation.duration)
                // Schedule the next image swap
                handler.postDelayed(this, imageSwapDelay)
            }
        })
    }

}