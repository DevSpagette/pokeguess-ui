package com.project.pokeguess

import android.content.BroadcastReceiver
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import org.json.JSONObject

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

    private var selectedNumbers: MutableList<Int> = mutableListOf()
    private val classicModeNumbers = (1..MAX).toMutableList()

    val checkedGens = arrayOf(
        GEN1Checked, GEN2Checked, GEN3Checked, GEN4Checked, GEN5Checked,
        GEN6Checked, GEN7Checked, GEN8Checked, GEN9Checked
    )

    // Function to generate a random number exclusively within the specified ranges
    private fun shuffleSelectedNumbers() {
        selectedNumbers.clear()

        for (i in checkedGens.indices) {
            if (checkedGens[i]) {
                when (i) {
                    0 -> selectedNumbers.addAll(GEN1.toList())
                    1 -> selectedNumbers.addAll(GEN2.toList())
                    2 -> selectedNumbers.addAll(GEN3.toList())
                    3 -> selectedNumbers.addAll(GEN4.toList())
                    4 -> selectedNumbers.addAll(GEN5.toList())
                    5 -> selectedNumbers.addAll(GEN6.toList())
                    6 -> selectedNumbers.addAll(GEN7.toList())
                    7 -> selectedNumbers.addAll(GEN8.toList())
                    8 -> selectedNumbers.addAll(GEN9.toList())
                }
            }
        }

        if (selectedNumbers.isEmpty()) {
            throw IllegalArgumentException("At least one generation must be selected.")
        }

        // Shuffle the list of selected numbers
        selectedNumbers.shuffle()
    }

    // Function to generate a unique random number for challenge mode
    fun generateRandomNumber(): Int {
        if (selectedNumbers.isEmpty()) {
            shuffleSelectedNumbers()
        }
        return selectedNumbers.removeAt(0)
    }

    // Function to generate a unique random number within the range of 1 to MAX for classic mode
    fun generateUniqueRandomNumber(): Int {
        if (classicModeNumbers.isEmpty()) {
            throw IllegalArgumentException("All numbers in classic mode have been used.")
        }

        classicModeNumbers.shuffle()
        return classicModeNumbers.removeAt(0)
    }
}

class MainActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val handler = Handler(Looper.getMainLooper())
    private var imageSwapDelay = 3000L
    private val homeImage = mutableListOf<String>()
    private val homeImageName = mutableListOf<String>()

    private lateinit var homePokemonImageView: ImageView
    private lateinit var homePokemonNameView: TextView

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
                finishAffinity()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register the broadcast receiver
        val intentFilter = IntentFilter(ACTION_CLOSE_APP)
        registerReceiver(closeAppReceiver, intentFilter)

        generatePokemonSprite()

        homePokemonImageView = findViewById(R.id.mainPageImage)
        homePokemonNameView = findViewById(R.id.mainPageImageName)

        swapImagesPeriodically()

        // Retrieve the jwtToken from SharedPreferences
        jwtToken = getJwtToken()

        // load settings
        loadSettings()

        // Set a click listener for the "Go to Leaderboard" button
        val userButton = findViewById<ImageButton>(R.id.user_button)
        val classicButton = findViewById<Button>(R.id.classic_mode_button)
        val challengeButton = findViewById<Button>(R.id.challenge_mode_button)
        val leaderboardButton = findViewById<Button>(R.id.leaderboard_button)
        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
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
                // Create an Intent to navigate to diff selector then to ChallengeActivity
                val intent = Intent(this, DiffSelectorActivity::class.java)
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

        leaderboardButton.setOnClickListener {
            // Create an Intent to navigate to LeaderboardActivity
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }

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
            serverStatusText.text = "Checking API status$status"
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
                        status = "asleep"
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

    private fun generatePokemonSprite() {

        val client = OkHttpClient()

        for (i in 1..10) {

            rng = (1..GLOBAL.MAX).random()
            val imageUrl = "$apiUrl/sprite/$rng"
            val imageInfo = "$apiUrl/info/$rng"


            var nameUrl = imageInfo
            val request = Request.Builder()
                .url(nameUrl)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                override fun onResponse(call: Call, response: Response) {

                    try {
                        if (response.code == 200) {
                            val responseBody = response.body?.string()
                            val jsonObject = JSONObject(responseBody)

                            homeImageName.add(
                                jsonObject.getString("name")
                                    .replaceFirstChar { it.uppercaseChar() })
                            homeImage.add(imageUrl)

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    private fun swapImagesPeriodically() {
        handler.post(object : Runnable {
            override fun run() {
                if (homeImage.isNotEmpty() && homeImageName.isNotEmpty()) {
                    // Create a fade-out animation
                    val fadeOutAnimation =
                        AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out_animation)
                    homePokemonImageView.startAnimation(fadeOutAnimation)
                    homePokemonNameView.startAnimation(fadeOutAnimation)

                    handler.postDelayed({
                        // Create a fade-in animation
                        val fadeInAnimation =
                            AnimationUtils.loadAnimation(applicationContext, R.anim.fade_animation)
                        homePokemonImageView.startAnimation(fadeInAnimation)
                        homePokemonNameView.startAnimation(fadeInAnimation)

                        // Update the image source
                        runOnUiThread {
                            Glide.with(this@MainActivity).load(homeImage[currentIndex])
                                .into(homePokemonImageView)
                            homePokemonNameView.text = homeImageName[currentIndex]
                        }

                        // Increment the index, or reset if it exceeds the array length
                        currentIndex = (currentIndex + 1) % homeImage.size
                    }, fadeOutAnimation.duration)
                }
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