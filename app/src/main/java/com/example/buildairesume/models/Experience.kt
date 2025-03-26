package com.example.buildairesume.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiences")
data class Experience(
    @PrimaryKey(autoGenerate = true) val experienceId: Int? = 0,
    val companyName: String,
    val startDate: String,
    var endDate: String?,
    val position: String,
    val description: String?,
    var output: String?

) {
    fun isEndDateEmpty(): Boolean = endDate.isNullOrEmpty()
}