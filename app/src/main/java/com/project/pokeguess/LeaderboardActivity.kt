package com.project.pokeguess

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val leaderboardTable = findViewById<TableLayout>(R.id.leaderboard_table)
        val leaderboardEntries = getLeaderboardEntries()

        // Calculate 25% of the screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val rowWidth = (screenWidth * 0.25).toInt()

        // Create an array of column titles
        val columnTitles = arrayOf("Rank", "Name", "Score")
        // Create a TableRow for column titles
        val columnTitleRow = TableRow(this)

        for (titleText in columnTitles) {
            val title = TextView(this)
            title.text = titleText
            title.setTextColor(Color.BLACK)
            title.background = ContextCompat.getDrawable(this, R.drawable.table_row_background)
            title.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            val params = TableRow.LayoutParams(rowWidth, TableRow.LayoutParams.WRAP_CONTENT)
            title.layoutParams = params
            title.setPadding(16, 16, 16, 16)
            title.setTypeface(null, Typeface.BOLD)

            columnTitleRow.addView(title)
        }

        // Add the columnTitleRow to the TableLayout
        leaderboardTable.addView(columnTitleRow)

        for (entry in leaderboardEntries) {
            val row = TableRow(this)

            val cellTexts = listOf(entry.rank.toString(), entry.name, entry.score.toString())

            for (text in cellTexts) {
                val cell = TextView(this)
                cell.text = text
                cell.setTextColor(Color.BLACK)
                cell.background = ContextCompat.getDrawable(this, R.drawable.table_row_background)
                cell.layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                val params = TableRow.LayoutParams(rowWidth, TableRow.LayoutParams.WRAP_CONTENT)
                cell.layoutParams = params
                cell.setPadding(16, 16, 16, 16)
                row.addView(cell)
            }

            // Add the TableRow to the TableLayout
            leaderboardTable.addView(row)
        }

        // Main activity button
        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        // Set a click listener for the ImageButton
        backButton.setOnClickListener(View.OnClickListener {
            // Create an Intent to navigate back to the MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        })

    }

    // Replace this with your actual data retrieval logic
    private fun getLeaderboardEntries(): List<LeaderboardEntry> {
        // Example: Return a list of leaderboard entries
        val entries = mutableListOf<LeaderboardEntry>()
        entries.add(LeaderboardEntry("Jean", 1000, 1))
        entries.add(LeaderboardEntry("Marco", 900, 2))
        entries.add(LeaderboardEntry("Polo", 800, 3))
        // Add more entries as needed
        return entries
    }
}