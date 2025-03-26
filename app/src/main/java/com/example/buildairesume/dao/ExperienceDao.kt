package com.example.buildairesume.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.buildairesume.models.Experience

@Dao
interface ExperienceDao : BaseDao<Experience> {
    @Query("SELECT * FROM experiences")
    suspend fun getAllExperiences(): List<Experience>

    @Query("DELETE FROM experiences")
    suspend fun deleteAllExperiences()

    @Update
    fun updateExperience(experience: Experience)

    @Update
    suspend fun updateExperiences(experiences: List<Experience>)
}
