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
    private lateinit var muteSounds: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        saveButton = findViewById(R.id.saveButton)
        saveButton.isEnabled = false
        setSaveButtonDisabledState()

        muteSounds = findViewById(R.id.settings_mute_sounds)
        val isSoundMuted = sharedPreferences.getBoolean("muteSounds", false)
        muteSounds.isChecked = isSoundMuted

        muteSounds.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("muteSounds", isChecked).apply()

            // Update the global variable
            GLOBAL.MUTESOUNDS = isChecked
        }

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

            val isChecked = sharedPreferences.getBoolean(generation, true)
            checkBox.isChecked = isChecked
            checkBoxes.add(checkBox)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val checkedCheckboxes = checkBoxes.filter { it.isChecked }
                val generation = checkBox.text.toString()

                if (isChecked) {
                    sharedPreferences.edit().putBoolean(generation, true).apply()
                } else {
                    if (checkedCheckboxes.isEmpty()) {
                        checkBox.isChecked = true
                    } else {
                        sharedPreferences.edit().putBoolean(generation, false).apply()
                    }
                }

                setSaveButtonEnabledState()
            }
        }

        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            onBackPressed()
        }

        saveButton.setOnClickListener {
            restartApplication()
        }
    }

    private fun setSaveButtonEnabledState() {
        runOnUiThread {
            saveButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_volc)
        }
        saveButton.isEnabled = true
    }

    private fun setSaveButtonDisabledState() {
        runOnUiThread {
            saveButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray_volc)
        }
        saveButton.isEnabled = false
    }

    private fun restartApplication() {
        val ctx = applicationContext
        val pm = ctx.packageManager
        val intent = pm.getLaunchIntentForPackage(ctx.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        ctx.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}