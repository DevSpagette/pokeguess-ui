package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
    private lateinit var heartTextView: TextView
    private lateinit var imageViewHeart1: ImageView
    private lateinit var imageViewHeart2: ImageView
    private lateinit var imageViewHeart3: ImageView
    private lateinit var imageViewHeart4: ImageView
    private lateinit var imageViewHeart5: ImageView

    private lateinit var imageBackground: ImageView
    private lateinit var greenFlashAnimation: AnimationDrawable
    private lateinit var redFlashAnimation: AnimationDrawable

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val userUrl = "https://pokeguess-api.onrender.com/user"
    private var jwtToken: String? = null
    private var userId: String? = null
    private var bestScore: Long = 0

    // Initialize MediaPlayer objects
    private var mediaPlayerGood: MediaPlayer? = null
    private var mediaPlayerWrong: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classic)

            imageViewHeart1 = findViewById(R.id.heart1)
            imageViewHeart2 = findViewById(R.id.heart2)
            imageViewHeart3 = findViewById(R.id.heart3)
            imageViewHeart4 = findViewById(R.id.heart4)
            imageViewHeart5 = findViewById(R.id.heart5)

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

        confirmButton = findViewById(R.id.confirmButton)
        idkButton = findViewById(R.id.idkButton)
        scoreTextView = findViewById(R.id.scoreText)
        heartTextView = findViewById(R.id.heartsText)

        heartTextView.text = "x$hearts"

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
                if (rng != 0 && hearts >= 0) {
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
            if (rng != 0 && hearts >= 0)
                playRound(json)
        }

        idkButton.setOnClickListener {

            if (hearts < 0) {
                // If no hearts left, show a message or perform any other action
                runOnUiThread {
                    Toast.makeText(
                        this@ClassicActivity,
                        "Game Over! No hearts remaining.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@setOnClickListener
            }

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
                        hearts--
                        if (score < 0) {
                            runOnUiThread {
                                scoreTextView.text = "Score: 0"
                                heartTextView.text = "x$hearts"
                            }
                            score = 0
                        } else {
                            runOnUiThread {
                                scoreTextView.text = "Score: $score"
                                heartTextView.text = "x$hearts"
                            }
                        }
                        displayHearts()
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

        displayHearts()

        // Load Pokemon sprite from the API
        if (shouldGenerateNewSprite && hearts >= 0)
            generatePokemonSprite()
    }

    private fun displayHearts() {
        val heartImageViews = listOf(
            imageViewHeart1,
            imageViewHeart2,
            imageViewHeart3,
            imageViewHeart4,
            imageViewHeart5
        )

        for (i in heartImageViews.indices) {
            heartImageViews[i].visibility = if (i < hearts) View.VISIBLE else View.INVISIBLE
        }
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

        if (hearts >= 0) {
            // wait 2 seconds for next round
            Thread.sleep(2000)
            generatePokemonSprite()
        } else {
            endGame()
        }
    }

    private fun endGame() {
        runOnUiThread {
            confirmButton.isEnabled = false
            Toast.makeText(
                this@ClassicActivity,
                "Game Over! No hearts remaining.",
                Toast.LENGTH_SHORT
            ).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate back to the main menu
            val intent = Intent(this@ClassicActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }

    // confirm guess (play a round)
    private fun playRound(json: JSONObject) {

        if (hearts < 0) {
            endGame()
        }

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
                            heartTextView.text = "x$hearts"
                            imageBackground.setBackgroundResource(R.drawable.bordered_imageview_green)
                            greenFlashAnimation.start()
                            playGoodSound()
                        }
                        loadPokemonSprite()
                        updateAchievements()
                    } else {
                        // handle wrong
                        shouldGenerateNewSprite = false
                        score -= 1
                        hearts--
                        if (score < 0)
                            score = 0
                        runOnUiThread {
                            scoreTextView.text = "Score: $score"
                            heartTextView.text = "x$hearts"
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
                        heartTextView.text = "x$hearts"
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

                    if (hearts < 0) {
                        endGame()
                    }
                }
            }
        })
        displayHearts()

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

    interface AchievementCallback {
        fun onSuccess(achievements: List<AchievementEntry>)
        fun onFailure()
    }
    // get userAchievements
    private fun getAchievements(callback: AchievementCallback) {
        val client = OkHttpClient()
        val url = "$userUrl/profile/achiv"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Could not get achievements, try again later.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    callback.onFailure()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val json = JSONObject(responseBody)

                        // Assuming 'achievements' is an array in the JSON response
                        val achievementsArray = json.getJSONArray("achievements")
                        val userAchievements = mutableListOf<AchievementEntry>()

                        for (i in 0 until achievementsArray.length()) {
                            val achievementObject = achievementsArray.getJSONObject(i)
                            val _id = achievementObject.getString("_id")
                            val name = achievementObject.getString("name")
                            val description = achievementObject.getString("description")
                            val goal = achievementObject.getInt("goal")
                            val progress = achievementObject.getInt("progress")
                            val unlocked = achievementObject.getBoolean("unlocked")

                            // Create an AchievementEntry object and add it to the list
                            val achievementEntry = AchievementEntry(_id, name, description, progress, goal, unlocked)
                            userAchievements.add(achievementEntry)
                        }

                        callback.onSuccess(userAchievements)
                    } else {
                        runOnUiThread {
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                "Could not get achievements, try again later.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        callback.onFailure()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    response.body?.close()
                }
            }
        })
    }

    // Update userAchievements
    private fun updateAchievements() {
        // Call getAchievements to retrieve the latest achievements
        getAchievements(object : AchievementCallback {
            override fun onSuccess(achievements: List<AchievementEntry>) {
                // Find achievements where progress is less than the goal
                val eligibleAchievements = achievements.filter { it.progress < it.goal }

                if (eligibleAchievements.isNotEmpty()) {

                    // Update the progress based on goodGuesses for all eligible achievements
                    val updatedAchievements = eligibleAchievements.map { achievement ->
                        val updatedProgress = achievement.progress + 1
                        val newProgress = minOf(updatedProgress, achievement.goal)

                        AchievementEntry(
                            achievement._id,
                            achievement.name,
                            achievement.description,
                            newProgress,
                            achievement.goal,
                            newProgress >= achievement.goal
                        )
                    }

                    // Create a JSON object with the updated achievements list
                    val json = JSONObject()
                    val achievementsArray = JSONArray()
                    for (achievement in updatedAchievements) {
                        val achievementObject = JSONObject()
                        achievementObject.put("_id", achievement._id)
                        achievementObject.put("name", achievement.name)
                        achievementObject.put("description", achievement.description)
                        achievementObject.put("goal", achievement.goal)
                        achievementObject.put("progress", achievement.progress)
                        achievementObject.put("unlocked", achievement.unlocked)
                        achievementsArray.put(achievementObject)
                    }
                    json.put("achievements", achievementsArray)

                    // Now, update the userAchievements with the latest achievements
                    performUpdate(json)
                }
            }

            override fun onFailure() {
                // Handle failure to get achievements
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Could not update achievements, try again later.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun performUpdate(json: JSONObject) {
        val client = OkHttpClient()
        val url = "$userUrl/achievements"
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Could not update achievements, try again later.",
                    Snackbar.LENGTH_LONG
                )
                    .show()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Achievements updated.",
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