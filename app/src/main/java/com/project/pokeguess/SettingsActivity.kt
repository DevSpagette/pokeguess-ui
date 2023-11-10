package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class SettingsActivity : AppCompatActivity() {

    private lateinit var saveButton: Button
    private var changesMade: Boolean = false

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

        saveButton = findViewById(R.id.saveButton)
        saveButton.isEnabled = false
        runOnUiThread {
            saveButton.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_volc))
        }

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
                    sharedPreferences.edit().putBoolean(generation, true).apply()
                } else {
                    // Ensure at least one generation is selected
                    if (checkedCheckboxes.isEmpty()) {
                        // Prevent unchecking if only one is checked
                        checkBox.isChecked = true
                    } else {
                        sharedPreferences.edit().putBoolean(generation, false).apply()
                    }
                }

                // Enable the save button when a change occurs
                runOnUiThread {
                    saveButton.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_volc))
                }
                changesMade = true
                saveButton.isEnabled = true
            }
        }

        // Back to the main activity button
        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            onBackPressed()
        }

        saveButton.setOnClickListener {
            val ctx = applicationContext
            val pm = ctx.packageManager
            val intent = pm.getLaunchIntentForPackage(ctx.packageName)
            val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
            ctx.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }

    }
}