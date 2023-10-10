package com.project.pokeguess

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

}