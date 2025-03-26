package com.example.buildairesume

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.buildairesume.dao.AchievementDao
import com.example.buildairesume.dao.CertificationDao
import com.example.buildairesume.dao.EducationDao
import com.example.buildairesume.dao.ExperienceDao
import com.example.buildairesume.dao.ProjectDao
import com.example.buildairesume.dao.SkillsDao
import com.example.buildairesume.dao.UserProfileDao
import com.example.buildairesume.models.Achievement
import com.example.buildairesume.models.Certification
import com.example.buildairesume.models.Education
import com.example.buildairesume.models.Experience
import com.example.buildairesume.models.Project
import com.example.buildairesume.models.Skills
import com.example.buildairesume.models.UserProfile

@Database(
    entities = [UserProfile::class, Skills::class, Achievement::class, Project::class, Certification::class, Experience::class, Education::class],
    version = 4, // Update when making schema changes
    exportSchema = true // Consider exporting the schema for version tracking
)
abstract class UserData : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun projectDao(): ProjectDao
    abstract fun certificationDao(): CertificationDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun educationDao(): EducationDao
    abstract fun skillsDao(): SkillsDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile

        
        private var instance: UserData? = null

        fun getInstance(context: Context): UserData {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserData::class.java, "resume_database"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }  // REMOVED allowMainThreadQueries()
            }
        }

    }
}
