package com.example.buildairesume.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.buildairesume.models.Project

@Dao
interface ProjectDao : BaseDao<Project> {
    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<Project>

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Update
    fun updateProject(project: Project)

    @Update
    suspend fun updateProjects(projects: List<Project>)
}
