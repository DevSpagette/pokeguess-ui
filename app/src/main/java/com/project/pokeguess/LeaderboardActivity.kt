package com.project.pokeguess

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val username = getUsername()
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = LeaderboardPagerAdapter(this, username)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Classic"
                1 -> "Challenge"
                else -> ""
            }
        }.attach()

        // Back to the main activity button
        val backButton = findViewById<ImageButton>(R.id.back_to_main_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private inner class LeaderboardPagerAdapter(activity: AppCompatActivity, private val username: String?) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return 2
        }
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClassicLeaderboardFragment.newInstance(username)
                1 -> ChallengeLeaderboardFragment.newInstance(username)
                else -> ChallengeLeaderboardFragment.newInstance(username)
            }
        }
    }

    private fun getUsername(): String? {
        // Retrieve the username from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("username", null)
    }
}