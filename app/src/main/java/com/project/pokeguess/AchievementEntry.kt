package com.project.pokeguess

data class AchievementEntry(
    val _id: String,
    val name: String,
    val description: String,
    val progress: Int,
    val goal: Int,
    val unlocked: Boolean
)
