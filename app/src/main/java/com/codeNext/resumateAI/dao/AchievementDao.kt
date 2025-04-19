package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codeNext.resumateAI.models.Achievement

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: Achievement)

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<Achievement>

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()


}
