package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class AchievementActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievement)

        val token = getJwtToken()

        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profile()
    }

    private fun profile() {
        val client = OkHttpClient()
        val url = "$apiUrl/profile/achiv"

        val jwtToken = getJwtToken()

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error while fetching user profile.",
                    Snackbar.LENGTH_LONG
                ).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)

                    val achievementsArray = json.getJSONArray("achievements")
                    val userAchievements = mutableListOf<AchievementEntry>()

                    for (i in 0 until achievementsArray.length()) {
                        val achievementObject = achievementsArray.getJSONObject(i)
                        val name = achievementObject.getString("name")
                        val description = achievementObject.getString("description")
                        val goal = achievementObject.getInt("goal")
                        val progress = achievementObject.getInt("progress")
                        val unlocked = achievementObject.getBoolean("unlocked")

                        // Create an AchievementEntry object and add it to the list
                        val achievementEntry = AchievementEntry(name, description,progress, goal, unlocked)
                        userAchievements.add(achievementEntry)
                    }

                    runOnUiThread {
                        updateAchievementsUI(userAchievements)
                    }

                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content), "Error while fetching user profile.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun updateAchievementsUI(userAchievements: List<AchievementEntry>) {
        // Set up RecyclerView
        val achievementsRecyclerView = findViewById<RecyclerView>(R.id.achievementsRecyclerView)
        val achievementsAdapter = AchievementsAdapter(userAchievements)
        achievementsRecyclerView.adapter = achievementsAdapter
        achievementsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun getJwtToken(): String? {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("jwtToken", null)
    }

    private inner class AchievementsAdapter(private val achievements: List<AchievementEntry>) :
        RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

        inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.achievement_name)
            val descriptionTextView: TextView = itemView.findViewById(R.id.achievement_description)
            val progressBar: ProgressBar = itemView.findViewById(R.id.achievement_progress_bar)
            val goalTextView: TextView = itemView.findViewById(R.id.goal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false)
            return AchievementViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
            val achievement = achievements[position]
            holder.nameTextView.text = achievement.name
            holder.descriptionTextView.text = achievement.description
            Log.d("AchievementAdapter", "Progress: ${achievement.progress}, Goal: ${achievement.goal}")
            val ratio = if (achievement.goal > 0) achievement.progress.toFloat() / achievement.goal else 0f
            holder.progressBar.progress = (ratio * 100).toInt()

            holder.goalTextView.text = "${achievement.progress} / ${achievement.goal}"
        }

        override fun getItemCount(): Int {
            return achievements.size
        }
    }

}