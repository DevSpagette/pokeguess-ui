package com.project.pokeguess

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.core.os.postDelayed
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val handler = Handler(Looper.getMainLooper())
    private val imageSwapDelay = 3000L // Delay in milliseconds for image change
    private val homeImage = mutableListOf<String>()

    private lateinit var homePokemonImageView: ImageView
    private var rng = 0
    private var currentIndex = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        homePokemonImageView = findViewById(R.id.HomePokemonImageView)


        homeImage.addAll(generatePokemonSprite())
        swapImagesPeriodically()

        // Set a click listener for the "Go to Leaderboard" button
        val leaderboardButton = findViewById<Button>(R.id.leaderboard_button)
        val challengeButton = findViewById<Button>(R.id.challenge_mode_button)
        val classicButton = findViewById<Button>(R.id.classic_mode_button)

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

        // Find the Quit button by its ID
        val quitButton = findViewById<Button>(R.id.quit_button)
        quitButton.setOnClickListener {
            finish()
        }


        // Start the image swapping process
    }
    private fun generatePokemonSprite(): MutableList<String>{
        val homeImageView = findViewById<ImageView>(R.id.HomePokemonImageView)
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