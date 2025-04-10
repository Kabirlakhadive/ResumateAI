package com.example.buildairesume.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val projectId: Int? = 0,
    val title: String,
    val description: String,
    val techStack: String,
    var output: String?

)
