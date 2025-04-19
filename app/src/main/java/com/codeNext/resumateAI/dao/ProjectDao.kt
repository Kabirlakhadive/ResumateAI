package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.codeNext.resumateAI.models.Project

@Dao
interface ProjectDao : BaseDao<Project> {
    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<Project>

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Update
    fun updateProject(project: Project)

    @Upsert // Replaces @Insert and @Update for this use case
    suspend fun upsertProject(project: Project) // Upsert single

    @Upsert
    suspend fun upsertProjects(projects: List<Project>) // Upsert list

    @Update
    suspend fun updateProjects(projects: List<Project>)
}
