package com.example.buildairesume

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5, // Update when making schema changes
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

        // --- V V V --- ADD THE MIGRATION OBJECT HERE --- V V V ---
        val MIGRATION_1_2 = object : Migration(1, 2) { // Match the version numbers (from 1, to 2)
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the 'output' column to projects table if it doesn't exist
                // Using "IF NOT EXISTS" is safer in migrations
                db.execSQL("ALTER TABLE projects ADD COLUMN output TEXT")
                // Add the 'output' column to experiences table if it doesn't exist
                db.execSQL("ALTER TABLE experiences ADD COLUMN output TEXT")
                // If you added 'output' to other tables, add ALTER TABLE statements here too
            }
        }
        // --- ^ ^ ^ --- END OF MIGRATION OBJECT --- ^ ^ ^ ---


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
