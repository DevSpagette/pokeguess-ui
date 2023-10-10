package com.project.pokeguess

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.*
import java.io.IOException

import java.io.FileInputStream
import java.util.Properties


class ChallengeActivity : AppCompatActivity() {

    private var shouldGenerateNewSprite = true
    private var rng = 0
    private var name = "missingno"
    private var score = 0

    // Add member variables for the buttons
    private lateinit var confirmButton: Button
    private lateinit var idkButton: Button
    private lateinit var scoreTextView: TextView

    private lateinit var imageBackground: ImageView
    private lateinit var greenFlashAnimation: AnimationDrawable
    private lateinit var redFlashAnimation: AnimationDrawable

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

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

        val inputPokemon = findViewById<EditText>(R.id.pokemonEditText)

        rng = (1..1006).random()

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
                sendPostRequest(json)
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

        // Load Pokémon sprite from the API
        if (shouldGenerateNewSprite)
            generatePokemonSprite()
    }

    // loads obfuscated sprite, starts new round
    private fun generatePokemonSprite() {
        val pokemonImageView = findViewById<ImageView>(R.id.pokemonImageView)
        val inputPokemon = findViewById<EditText>(R.id.pokemonEditText)

        rng = (1..1006).random()
        val imageUrl = "$apiUrl/obf_sprite/$rng"
        runOnUiThread {
            inputPokemon.setText("")
            Glide.with(this@ChallengeActivity).load(imageUrl).into(pokemonImageView)
        }
        shouldGenerateNewSprite = false
    }

    // loads visible sprite
    private fun loadPokemonSprite() {
        val pokemonImageView = findViewById<ImageView>(R.id.pokemonImageView)
        val spriteUrl = "$apiUrl/sprite/$rng"
        runOnUiThread {
            Glide.with(this@ChallengeActivity).load(spriteUrl).into(pokemonImageView)
        }
        // wait 2 seconds for next round
        Thread.sleep(2000)
        generatePokemonSprite()
    }

    // confirm guess
    private fun sendPostRequest(json: JSONObject) {
        val client = OkHttpClient()
        val url = "$apiUrl/play"
        val body =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
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
                }
            }
        })
    }
}