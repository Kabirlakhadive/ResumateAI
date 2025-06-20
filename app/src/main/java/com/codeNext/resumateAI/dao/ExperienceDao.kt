package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.codeNext.resumateAI.models.Experience

@Dao
interface ExperienceDao : BaseDao<Experience> {
    @Query("SELECT * FROM experiences")
    suspend fun getAllExperiences(): List<Experience>

    @Query("DELETE FROM experiences")
    suspend fun deleteAllExperiences()

    @Update
    fun updateExperience(experience: Experience)

    @Upsert
    suspend fun upsertExperience(experience: Experience)

    @Upsert
    suspend fun upsertExperiences(experiences: List<Experience>)

    @Update
    suspend fun updateExperiences(experiences: List<Experience>)
}
