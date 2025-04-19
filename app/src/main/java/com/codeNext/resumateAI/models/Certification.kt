package com.codeNext.resumateAI.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "certifications")
data class Certification(
    @PrimaryKey(autoGenerate = true) val certificationId: Int? = 0,
    val name: String,
    val year: String,
)
