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
import androidx.core.content.ContextCompat
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
    var GEN1Checked = true
    var GEN2Checked = true
    var GEN3Checked = true
    var GEN4Checked = true
    var GEN5Checked = true
    var GEN6Checked = true
    var GEN7Checked = true
    var GEN8Checked = true
    var GEN9Checked = true

    const val MAX = 1017
    private val GEN1 = 1..151
    private val GEN2 = 152..251
    private val GEN3 = 252..386
    private val GEN4 = 387..493
    private val GEN5 = 494..649
    private val GEN6 = 650..721
    private val GEN7 = 722..809
    private val GEN8 = 810..905
    private val GEN9 = 906..1017

    val checkedGens = arrayOf(
        GEN1Checked, GEN2Checked, GEN3Checked, GEN4Checked, GEN5Checked,
        GEN6Checked, GEN7Checked, GEN8Checked, GEN9Checked
    )

    // Function to generate a random number exclusively within the specified ranges
    fun generateRandomNumber(): Int {
        val selectedRanges = mutableListOf<IntRange>()
        for (i in checkedGens.indices) {
            if (checkedGens[i]) {
                when (i) {
                    0 -> selectedRanges.add(GEN1)
                    1 -> selectedRanges.add(GEN2)
                    2 -> selectedRanges.add(GEN3)
                    3 -> selectedRanges.add(GEN4)
                    4 -> selectedRanges.add(GEN5)
                    5 -> selectedRanges.add(GEN6)
                    6 -> selectedRanges.add(GEN7)
                    7 -> selectedRanges.add(GEN8)
                    8 -> selectedRanges.add(GEN9)
                }
            }
        }

        if (selectedRanges.isEmpty()) {
            throw IllegalArgumentException("At least one generation must be selected.")
        }

        while (true) {
            val randomIndex = Random.nextInt(selectedRanges.size)
            val selectedRange = selectedRanges[randomIndex]
            val result = Random.nextInt(selectedRange.first, selectedRange.last + 1)
            if (result != 0) {
                return result
            }
        }
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
        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_volc)

        // Register the broadcast receiver
        val intentFilter = IntentFilter(ACTION_CLOSE_APP)
        registerReceiver(closeAppReceiver, intentFilter)

        homePokemonImageView = findViewById(R.id.mainPageImage)

        homeImage.addAll(generatePokemonSprite())
        swapImagesPeriodically()

        // Retrieve the jwtToken from SharedPreferences
        jwtToken = getJwtToken()

        // load settings
        loadSettings()
        for (i in 0 until GLOBAL.checkedGens.size) {
            println("Generation ${i+1}: ${GLOBAL.checkedGens[i]}")
        }

        // Set a click listener for the "Go to Leaderboard" button
        val userButton = findViewById<ImageButton>(R.id.userButton)
        val classicButton = findViewById<Button>(R.id.classic_mode_button)
        val challengeButton = findViewById<Button>(R.id.challenge_mode_button)
        val leaderboardButton = findViewById<Button>(R.id.leaderboard_button)
        val settingsButton = findViewById<Button>(R.id.settings_button)
        val quitButton = findViewById<Button>(R.id.quit_button)

        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)

        val serverStatusText = findViewById<TextView>(R.id.serverStatusText)
        val serverStatusView = findViewById<View>(R.id.statusDot)
        serverStatusText.text = "API status: $status"
        serverStatusView.setBackgroundResource(R.drawable.yellow_dot)

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
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Please log in to use this feature",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }
        }

        classicButton.setOnClickListener {
            if (jwtToken != null) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Classic mod not available switch to challenge",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
                // Create an Intent to navigate to ChallengeActivity
                val intent = Intent(this, ChallengeActivity::class.java)
                startActivity(intent)
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Please log in to use this feature",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Log In") {
                        val intent = Intent(this, AuthActivity::class.java)
                        startActivity(intent)
                    }
                    .show()
            }
        }

        leaderboardButton.setOnClickListener(View.OnClickListener {
            // Create an Intent to navigate to LeaderboardActivity
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        })

        settingsButton.setOnClickListener {
            // Create an Intent to navigate to LeaderboardActivity
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        refreshButton.setOnClickListener {
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

    private fun generatePokemonSprite(): MutableList<String> {
        val homeImageView = findViewById<ImageView>(R.id.mainPageImage)
        val list = mutableListOf<String>()

        for (i in 1..10) {
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
                val fadeOutAnimation =
                    AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out_animation)
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
                }, fadeOutAnimation.duration)
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

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Initialize preferences if they don't exist
        if (!sharedPreferences.contains("gen1")) {
            // If the preferences don't exist, initialize them to true
            for (i in 1..9) {
                val generationKey = "gen$i"
                sharedPreferences.edit().putBoolean(generationKey, true).apply()
            }
        }

        // Load settings into global variables
        for (i in 1..9) {
            val generationKey = "gen$i"
            val isChecked = sharedPreferences.getBoolean(generationKey, true)
            // Update the corresponding global variable (if you have them defined)
            when (i) {
                1 -> GLOBAL.GEN1Checked = isChecked
                2 -> GLOBAL.GEN2Checked = isChecked
                3 -> GLOBAL.GEN3Checked = isChecked
                4 -> GLOBAL.GEN4Checked = isChecked
                5 -> GLOBAL.GEN5Checked = isChecked
                6 -> GLOBAL.GEN6Checked = isChecked
                7 -> GLOBAL.GEN7Checked = isChecked
                8 -> GLOBAL.GEN8Checked = isChecked
                9 -> GLOBAL.GEN9Checked = isChecked
            }
            // update the checkedGens!
            GLOBAL.checkedGens[i - 1] = isChecked
        }
    }

}