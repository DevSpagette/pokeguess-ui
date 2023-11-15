package com.project.pokeguess

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.*
import java.io.IOException
import java.util.Random

open class ClassicActivity : AppCompatActivity() {

    private var shouldGenerateNewSprite = true
    private var rng = 0
    private var name = "missing-no"
    private var score: Long = 0
    private var doubleBackToExitPressedOnce = false

    // Difficulty 0 = easy, 1 = normal, 2 = master
    private var difficulty: Int = 0

    // Lives & jokers
    private var hearts: Int = when (difficulty) {
        0 -> 5 // Easy
        1 -> 3 // Normal
        2 -> 1 // Master
        else -> throw IllegalArgumentException("Invalid difficulty level")
    }

    // Add member variables for the buttons
    private lateinit var confirmButton: Button
    private lateinit var idkButton: Button
    private lateinit var scoreTextView: TextView

    private lateinit var imageBackground: ImageView
    private lateinit var greenFlashAnimation: AnimationDrawable
    private lateinit var redFlashAnimation: AnimationDrawable

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private var jwtToken: String? = null
    private var userId: String? = null
    private var bestScore: Long = 0

    // Initialize MediaPlayer objects
    private var mediaPlayerGood: MediaPlayer? = null
    private var mediaPlayerWrong: MediaPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        // select difficulty from diff selector activity
        val intent = intent
        if (intent.hasExtra("DIFFICULTY")) {
            difficulty = intent.getIntExtra(
                "DIFFICULTY",
                0
            ) // 0 is the default difficulty if nothing is passed
            hearts = when (difficulty) {
                0 -> 5 // Easy
                1 -> 3 // Normal
                2 -> 1 // Master
                else -> throw IllegalArgumentException("Invalid difficulty level")
            }
        }

        jwtToken = getJwtToken()
        userId = getUserId()
        bestScore = getBestScore()

        imageBackground = findViewById(R.id.pokemonImageView)
        redFlashAnimation =
            ContextCompat.getDrawable(this, R.drawable.red_flash) as AnimationDrawable
        greenFlashAnimation =
            ContextCompat.getDrawable(this, R.drawable.green_flash) as AnimationDrawable

        // Set the initial background (silver)
        imageBackground.setBackgroundResource(R.color.silver)

        confirmButton = findViewById<Button>(R.id.confirmButton)
        idkButton = findViewById<Button>(R.id.idkButton)
        scoreTextView = findViewById(R.id.scoreText)

        // Load sound effects
        mediaPlayerGood = MediaPlayer.create(this, R.raw.good_guess)
        mediaPlayerWrong = MediaPlayer.create(this, R.raw.wrong_guess)

        rng = GLOBAL.generateUniqueRandomNumber()

        val inputPokemon = findViewById<EditText>(R.id.pokemonEditText)
        inputPokemon.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Disable both buttons
                confirmButton.isEnabled = false
                idkButton.isEnabled = false

                val name = inputPokemon.text.toString()

                // Create a JSON object with name and id
                val json = JSONObject()
                json.put("name", name)
                json.put("id", rng)

                // Send the API POST request
                if (rng != 0) {
                    playRound(json)
                }

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        confirmButton.setOnClickListener {

            // Disable both buttons
            confirmButton.isEnabled = false
            idkButton.isEnabled = false

            val name = inputPokemon.text.toString()

            // Create a JSON object with name and id
            val json = JSONObject()
            json.put("name", name)
            json.put("id", rng)

            // Send the API POST request
            if (rng != 0)
                playRound(json)
        }

        idkButton.setOnClickListener {

            // Disable both buttons
            confirmButton.isEnabled = false
            idkButton.isEnabled = false

            inputPokemon.setText("")

            val client = OkHttpClient()
            val url = "$apiUrl/info/$rng"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.code == 200) {
                            val responseBody = response.body?.string()
                            try {
                                val jsonObject = JSONObject(responseBody)
                                name = jsonObject.getString("name")
                                runOnUiThread {
                                    inputPokemon.setText(name)
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                        shouldGenerateNewSprite = true
                        score -= 10
                        if (score < 0) {
                            runOnUiThread {
                                scoreTextView.text = "Score: 0"
                            }
                            score = 0
                        } else {
                            runOnUiThread {
                                scoreTextView.text = "Score: $score"
                            }
                        }
                        loadPokemonSprite()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        runOnUiThread {
                            // Enable both buttons
                            confirmButton.isEnabled = true
                            idkButton.isEnabled = true
                        }
                        response.body?.close()
                    }
                }
            })
        }

        // Load Pokemon sprite from the API
        if (shouldGenerateNewSprite)
            generatePokemonSprite()
    }

    // loads obfuscated sprite, starts new round
    private fun generatePokemonSprite() {
        val pokemonImageView = findViewById<ImageView>(R.id.pokemonImageView)
        val inputPokemon = findViewById<EditText>(R.id.pokemonEditText)

        rng = GLOBAL.generateRandomNumber()
        val imageUrl = "$apiUrl/obf_sprite/$rng"
        runOnUiThread {
            inputPokemon.setText("")
            Glide.with(this@ClassicActivity).load(imageUrl).into(pokemonImageView)
        }
        shouldGenerateNewSprite = false
    }

    // loads visible sprite
    private fun loadPokemonSprite() {
        val pokemonImageView = findViewById<ImageView>(R.id.pokemonImageView)
        val spriteUrl = "$apiUrl/sprite/$rng"
        runOnUiThread {
            Glide.with(this@ClassicActivity).load(spriteUrl).into(pokemonImageView)
        }
        // wait 2 seconds for next round
        Thread.sleep(2000)
        generatePokemonSprite()
    }

    // confirm guess (play a round)
    private fun playRound(json: JSONObject) {

        val client = OkHttpClient()
        val url = "$apiUrl/play"
        val body =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Thread.sleep(1000)
                runOnUiThread {
                    confirmButton.isEnabled = true
                    idkButton.isEnabled = true
                    imageBackground.setBackgroundResource(R.color.silver)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.code == 200) {
                        shouldGenerateNewSprite = true
                        score += 10
                        runOnUiThread {
                            scoreTextView.text = "Score: $score"
                            imageBackground.setBackgroundResource(R.drawable.bordered_imageview_green)
                            greenFlashAnimation.start()
                            playGoodSound()
                        }
                        loadPokemonSprite()
                    } else {
                        // handle wrong
                        shouldGenerateNewSprite = false
                        score -= 1
                        if (score < 0)
                            score = 0
                        runOnUiThread {
                            scoreTextView.text = "Score: $score"
                            imageBackground.setBackgroundResource(R.drawable.bordered_imageview_red)
                            redFlashAnimation.start()
                            playWrongSound()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    response.body?.close()
                    Thread.sleep(1000)
                    runOnUiThread {
                        scoreTextView.text = "Score: $score"
                        confirmButton.isEnabled = true
                        idkButton.isEnabled = true
                        imageBackground.setBackgroundResource(R.color.silver)
                    }
                    // Create a JSON object with name and id if current score is better
                    // only if difficulty is on master
                    if (score > bestScore && difficulty == 2) {
                        val sharedPreferences =
                            getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.remove("bestScore")
                        editor.putLong("bestScore", bestScore)
                        editor.apply()

                        val json = JSONObject()
                        json.put("userId", userId)
                        json.put("score", score)
                        updateLeaderboard(json)
                    }
                }
            }
        })
    }

    // update the leaderboard entry
    private fun updateLeaderboard(json: JSONObject) {
        val client = OkHttpClient()
        val url = "$apiUrl/leaderboard/classic"
        val body =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Could not update leaderboard, try again later.",
                    Snackbar.LENGTH_LONG
                )
                    .show()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Leaderboard updated.",
                        Snackbar.LENGTH_LONG
                    )
                        .show()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    response.body?.close()
                }
            }
        })
    }

    private fun playGoodSound() {
        if (!GLOBAL.MUTESOUNDS)
            mediaPlayerGood?.start()
    }

    private fun playWrongSound() {
        if (!GLOBAL.MUTESOUNDS)
            mediaPlayerWrong?.start()
    }

    override fun onDestroy() {
        mediaPlayerGood?.release()
        mediaPlayerWrong?.release()
        super.onDestroy()
    }

    private fun getJwtToken(): String? {
        // Retrieve the jwtToken from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwtToken", null)
    }

    private fun getUserId(): String? {
        // Retrieve the jwtToken from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userId", null)
    }

    private fun getBestScore(): Long {
        // Retrieve the jwtToken from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("bestScore", 0)
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

}