package com.codeNext.resumateAI // Or your app's package name

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

object DailyUsageManager {

    private const val PREFS_NAME = "GeminiDailyUsagePrefs"
    private const val KEY_LAST_GENERATION_DATE = "lastGenerationDate"
    private const val KEY_TODAY_GENERATION_COUNT = "todayGenerationCount"
    private const val MAX_GENERATIONS_PER_DAY = 3

    // Helper to get a consistent date string format (YYYY-MM-DD)
    private fun getFormattedDate(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-$month-$day"
    }

    /**
     * Checks if a new generation is allowed today.
     * If it's a new day, it resets the count.
     * If allowed, it increments the count for today.
     *
     * @param context Context to access SharedPreferences.
     * @return True if generation is allowed and count has been incremented, false otherwise.
     */
    fun canGenerateAndIncrement(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val todayCalendar = Calendar.getInstance()
        val todayDateStr = getFormattedDate(todayCalendar)

        val lastGenerationDateStr = prefs.getString(KEY_LAST_GENERATION_DATE, null)
        var generationCount = prefs.getInt(KEY_TODAY_GENERATION_COUNT, 0)

        if (lastGenerationDateStr != todayDateStr) {
            // It's a new day!
            generationCount = 0 // Reset count for the new day
            editor.putString(KEY_LAST_GENERATION_DATE, todayDateStr) // Update the last generation date
            Log.d("DailyUsageManager", "New day detected. Resetting generation count.")
        }

        return if (generationCount < MAX_GENERATIONS_PER_DAY) {
            generationCount++
            editor.putInt(KEY_TODAY_GENERATION_COUNT, generationCount)
            editor.apply() // Save changes
            Log.d("DailyUsageManager", "Generation allowed. Count for today: $generationCount")
            true // Allowed to generate
        } else {
            // Limit reached for today, ensure date is saved if it was a new day check
            editor.apply()
            Log.d("DailyUsageManager", "Daily generation limit ($MAX_GENERATIONS_PER_DAY) reached.")
            false // Limit reached
        }
    }

    /**
     * Gets the number of remaining generations for today.
     * Also resets the count if it's a new day.
     *
     * @param context Context to access SharedPreferences.
     * @return Number of remaining generations.
     */
    fun getRemainingGenerations(context: Context): Int {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val todayCalendar = Calendar.getInstance()
        val todayDateStr = getFormattedDate(todayCalendar)

        val lastGenerationDateStr = prefs.getString(KEY_LAST_GENERATION_DATE, null)
        var generationCount = prefs.getInt(KEY_TODAY_GENERATION_COUNT, 0)

        if (lastGenerationDateStr != todayDateStr) {
            // It's a new day, reset count in prefs for consistency
            generationCount = 0
            editor.putInt(KEY_TODAY_GENERATION_COUNT, 0)
            editor.putString(KEY_LAST_GENERATION_DATE, todayDateStr)
            editor.apply()
        }
        return (MAX_GENERATIONS_PER_DAY - generationCount).coerceAtLeast(0)
    }
}