package com.project.pokeguess

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Integer.min
import androidx.fragment.app.Fragment

class ChallengeLeaderboardFragment : Fragment() {
    companion object {
        var username: String? = null
        fun newInstance(username: String?): ChallengeLeaderboardFragment {
            val fragment = ChallengeLeaderboardFragment()
            val args = Bundle()
            args.putString("username", username)
            fragment.arguments = args

            Companion.username = username

            return fragment
        }
    }

    private val apiUrl = "https://pokeguess-api.onrender.com/pokemon"
    private val maxEntries = 100
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_challenge, container, false)

        val leaderboardTable = view.findViewById<TableLayout>(R.id.leaderboard_challenge_table)
        progressBar = view.findViewById(R.id.progressBar)
        scrollView = view.findViewById(R.id.leaderboard_challenge_view)

        requireActivity().runOnUiThread {
            progressBar.visibility = View.VISIBLE
            scrollView.visibility = View.GONE
        }

        // Create an array of column titles
        val columnTitles = arrayOf("Rank", "Name", "Score")
        val columnTitleRow = TableRow(requireContext())

        // array headers
        for (titleText in columnTitles) {
            val title = TextView(requireContext(), null, 0, R.style.DefaultTextStyle)
            title.text = titleText
            title.setTextColor(Color.BLACK)
            title.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.table_row_background)
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
        leaderboardTable.addView(columnTitleRow)

        // Use a callback to fetch leaderboard entries
        fetchLeaderboardEntries { entries ->
            requireActivity().runOnUiThread {
                for (entry in entries) {
                    val row = TableRow(requireContext())

                    val cellTexts =
                        listOf(entry.rank.toString(), entry.name, entry.score)

                    for (text in cellTexts) {
                        val cell = TextView(requireContext(), null, 0, R.style.DefaultTextStyle)
                        cell.text = text
                        cell.textSize = 15F
                        cell.setTextColor(Color.BLACK)
                        cell.setTypeface(null, Typeface.NORMAL)
                        cell.setPadding(16, 16, 16, 16)

                        if (entry.name == username) {
                            cell.background = ContextCompat.getDrawable(requireContext(), R.drawable.table_row_special_background)
                        } else {
                            cell.background = ContextCompat.getDrawable(requireContext(), R.drawable.table_row_background)
                        }

                        cell.layoutParams = TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        val params = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
                        cell.layoutParams = params

                        row.addView(cell)
                    }
                    leaderboardTable.addView(row)
                }

                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
            }
        }

        return view
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
                callback(emptyList())
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
                            val username = entryObject.getJSONObject("userId").getString("username")
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
