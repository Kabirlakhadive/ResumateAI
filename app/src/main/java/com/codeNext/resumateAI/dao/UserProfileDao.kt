package com.codeNext.resumateAI.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codeNext.resumateAI.models.UserProfile

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUser(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userProfile: UserProfile)
}
