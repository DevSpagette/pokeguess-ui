package com.project.pokeguess

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Integer.min

class LeaderboardActivity : AppCompatActivity() {

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val maxEntries = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val leaderboardTable = findViewById<TableLayout>(R.id.leaderboard_table)

        // Create an array of column titles
        val columnTitles = arrayOf("Rank", "Name", "Score")
        val columnTitleRow = TableRow(this)

        // array headers
        for (titleText in columnTitles) {
            val title = TextView(this)
            title.text = titleText
            title.setTextColor(Color.BLACK)
            title.background = ContextCompat.getDrawable(this, R.drawable.table_row_background)
            title.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            val params = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
            title.layoutParams = params
            title.setPadding(16, 16, 16, 16)
            title.setTypeface(null, Typeface.BOLD)

            columnTitleRow.addView(title)
        }

        // Add the columnTitleRow to the TableLayout
        leaderboardTable.addView(columnTitleRow)

        // Use a callback to fetch leaderboard entries
        fetchLeaderboardEntries { entries ->
            runOnUiThread {
                // Add leaderboard entries to the TableLayout
                for (entry in entries) {
                    val row = TableRow(this@LeaderboardActivity)

                    val cellTexts =
                        listOf(entry.rank.toString(), entry.name, entry.score.toString())

                    for (text in cellTexts) {
                        val cell = TextView(this@LeaderboardActivity)
                        cell.text = text
                        cell.setTextColor(Color.BLACK)
                        cell.background = ContextCompat.getDrawable(
                            this@LeaderboardActivity,
                            R.drawable.table_row_background
                        )
                        cell.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        val params = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                        cell.layoutParams = params
                        cell.setPadding(16, 16, 16, 16)
                        row.addView(cell)
                    }

                    // Add the TableRow to the TableLayout
                    leaderboardTable.addView(row)
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

    private fun fetchLeaderboardEntries(callback: (List<LeaderboardEntry>) -> Unit) {
        val client = OkHttpClient()
        val url = "$apiUrl/leaderboard"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val entries = mutableListOf<LeaderboardEntry>()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle network request failure here
                e.printStackTrace()
                callback(emptyList()) // Return an empty list in case of failure
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody: ResponseBody? = response.body
                val json = responseBody?.string()

                if (json != null) {
                    val jsonObject = JSONObject(json)

                    if (jsonObject.has("leaderboard")) {
                        val jsonArray = jsonObject.getJSONArray("leaderboard")

                        for (i in 0 until min(maxEntries, jsonArray.length())) {
                            val entryObject = jsonArray.getJSONObject(i)
                            val username = entryObject.getJSONObject("user").getString("username")
                            val score = entryObject.getLong("score")

                            entries.add(LeaderboardEntry(username, score.toString(), i + 1))
                        }

                        for (i in entries.size until maxEntries) {
                            entries.add(LeaderboardEntry("---", "---", i + 1))
                        }
                    }
                }

                callback(entries)
            }
        })
    }

}