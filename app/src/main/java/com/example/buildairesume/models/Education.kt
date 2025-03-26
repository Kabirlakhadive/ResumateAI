package com.example.buildairesume.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Education(
    @PrimaryKey(autoGenerate = true) val educationId: Int? = 0,
    val qualification: String,
    val school: String,
    val board: String,
    val degree: String?,
    val branch: String?,
    val specialization: String?,
    val gradingSystem: String,
    val score: String,
    val year: String
) {
    fun isSpecializationEmpty(): Boolean = specialization.isNullOrEmpty()
    fun isBranchEmpty(): Boolean = branch.isNullOrEmpty()
    fun isDegreeEmpty(): Boolean = degree.isNullOrEmpty()
}


