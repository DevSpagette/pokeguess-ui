package com.project.pokeguess

import android.content.BroadcastReceiver
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
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
import kotlin.random.Random

object GLOBAL {
    const val MAX = 1017
    val GEN1 = 1..151
    val GEN2 = 152..251
    val GEN3 = 252..386
    val GEN4 = 387..493
    val GEN5 = 494..649
    val GEN6 = 650..721
    val GEN7 = 722..809
    val GEN8 = 810..905
    val GEN9 = 906..1017

    // Function to generate a random number exclusively within the specified ranges
    fun generateRandomNumber(vararg ranges: IntRange): Int {
        while (true) {
            // Generate a random index between 0 (inclusive) and the size of the 'ranges' array (exclusive)
            val randomIndex = Random.nextInt(ranges.size)
            // Get the selected range based on the random index
            val selectedRange = ranges[randomIndex]
            // Generate a random number within the specified range (inclusive) using 'Random.nextInt'
            val result = Random.nextInt(selectedRange.first, selectedRange.last + 1)
            // Check if the generated number is not equal to 0
            if (result != 0) {
                return result
            }
        }
        // usage = val rng = GLOBAL.generateRandomNumber(GLOBAL.GEN1, GLOBAL.GEN3, GLOBAL.GEN5)
    }

}

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
    companion object {
        const val ACTION_CLOSE_APP = "com.project.pokeguess.ACTION_CLOSE_APP"
    }

    private val closeAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOSE_APP) {
                finishAffinity() // Finish all activities in the app
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register the broadcast receiver
        val intentFilter = IntentFilter(ACTION_CLOSE_APP)
        registerReceiver(closeAppReceiver, intentFilter)

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
            println("RNG = " + GLOBAL.generateRandomNumber(GLOBAL.GEN1, GLOBAL.GEN9))
            checkServerStatus()
        }

        quitButton.setOnClickListener {
            val intent = Intent(MainActivity.ACTION_CLOSE_APP)
            sendBroadcast(intent)
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
            rng = (1..GLOBAL.MAX).random()
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

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver when the activity is destroyed
        unregisterReceiver(closeAppReceiver)
    }

}