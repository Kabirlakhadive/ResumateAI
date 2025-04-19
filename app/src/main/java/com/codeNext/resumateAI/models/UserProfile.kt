package com.codeNext.resumateAI.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Int? = 0,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val address: String,
    val jobProfile: String?,
    val gender: String,
    val linkedIn: String,
    val github: String,
    val website: String,
    var objective: String?,
)
