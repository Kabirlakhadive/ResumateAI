package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Query
import com.codeNext.resumateAI.models.Certification


@Dao
interface CertificationDao : BaseDao<Certification> {
    @Query("SELECT * FROM certifications")
    suspend fun getAllCertifications(): List<Certification>

    @Query("DELETE FROM certifications")
    suspend fun deleteAllCertifications()
}
