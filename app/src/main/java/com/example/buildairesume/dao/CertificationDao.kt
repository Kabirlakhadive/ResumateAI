package com.example.buildairesume.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.buildairesume.models.Certification


@Dao
interface CertificationDao : BaseDao<Certification> {
    @Query("SELECT * FROM certifications")
    suspend fun getAllCertifications(): List<Certification>

    @Query("DELETE FROM certifications")
    suspend fun deleteAllCertifications()
}
