package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Query
import com.codeNext.resumateAI.models.Education

@Dao
interface EducationDao : BaseDao<Education> {
    @Query("SELECT * FROM education")
    suspend fun getAllEducation(): List<Education>

    @Query("DELETE FROM education")
    suspend fun deleteAllEducation()
}
