package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Array of checkbox IDs
        val checkBoxIds = intArrayOf(
            R.id.generation1_checkbox,
            R.id.generation2_checkbox,
            R.id.generation3_checkbox,
            R.id.generation4_checkbox,
            R.id.generation5_checkbox,
            R.id.generation6_checkbox,
            R.id.generation7_checkbox,
            R.id.generation8_checkbox,
            R.id.generation9_checkbox,
        )

        val checkBoxes = mutableListOf<CheckBox>()

        for (checkBoxId in checkBoxIds) {
            val checkBox = findViewById<CheckBox>(checkBoxId)
            val generation = checkBox.text.toString()

            // Get the stored value or use the default
            val isChecked = sharedPreferences.getBoolean(generation, true)
            checkBox.isChecked = isChecked
            checkBoxes.add(checkBox)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val checkedCheckboxes = checkBoxes.filter { it.isChecked }
                val generation = checkBox.text.toString()

                if (isChecked) {
                    // Update the SharedPreferences with the new state
                    sharedPreferences.edit().putBoolean(generation, true).apply()
                } else {
                    // Ensure at least one generation is selected
                    if (checkedCheckboxes.isEmpty()) {
                        // Prevent unchecking if only one is checked
                        checkBox.isChecked = true
                    } else {
                        // Update the SharedPreferences with the new state
                        sharedPreferences.edit().putBoolean(generation, false).apply()
                    }
                }
            }
        }

        // Back to the main activity button
        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}