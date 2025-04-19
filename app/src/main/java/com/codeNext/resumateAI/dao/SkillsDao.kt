package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.codeNext.resumateAI.models.Skills

@Dao
interface SkillsDao {
    @Query("SELECT * FROM skills")
    suspend fun getAllSkills(): List<Skills>

    @Insert
    suspend fun insert(skill: Skills)

    @Delete
    suspend fun delete(skill: Skills)

    @Query("DELETE FROM skills")
    suspend fun deleteAll()
}
