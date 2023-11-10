package com.project.pokeguess

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class DiffSelectorActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diff_selector)

        val easyButton = findViewById<Button>(R.id.easyButton)
        val normalButton = findViewById<Button>(R.id.normalButton)
        val masterButton = findViewById<Button>(R.id.masterButton)

        easyButton.setOnClickListener {
            startClassicActivity(0) // easy
        }

        normalButton.setOnClickListener {
            startClassicActivity(1) // normal
        }

        masterButton.setOnClickListener {
            startClassicActivity(2) // master
        }

        // Back to the main activity button
        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun startClassicActivity(difficulty: Int) {
        val intent = Intent(this, ClassicActivity::class.java)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
        finish()
    }
}