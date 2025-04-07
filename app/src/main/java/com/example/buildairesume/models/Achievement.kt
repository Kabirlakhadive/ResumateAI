package com.example.buildairesume.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val achievementId: Int? = 0, // Nullable for new entries
    val title: String,
    val date: String
)
