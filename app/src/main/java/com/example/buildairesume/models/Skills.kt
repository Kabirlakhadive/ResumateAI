package com.example.buildairesume.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class Skills(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val skillName: String
)
