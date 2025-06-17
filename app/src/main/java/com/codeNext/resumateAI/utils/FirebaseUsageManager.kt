package com.codeNext.resumateAI.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.annotation.SuppressLint

object FirebaseUsageManager {

    private const val TAG = "FirebaseUsageManager"
    private const val COLLECTION_DEVICES = "device_usage"
    private const val FIELD_GENERATIONS_COUNT = "generationsToday"
    private const val FIELD_LAST_GENERATION_DATE = "lastGenerationDate"
    private const val MAX_GENERATIONS_PER_DAY = 3

    // Gets a reference to the Firestore database
    private val db = FirebaseFirestore.getInstance()

    /**
     * Provides a unique and persistent ID for the device/app installation.
     * This ID survives app reinstalls.
     */
    @SuppressLint("HardwareIds")

    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * Returns the current date as a string in "yyyy-MM-dd" format.
     */
    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Checks if the user can generate content and, if so, records the generation.
     * This is an atomic operation to prevent race conditions.
     * @return Boolean: true if generation is allowed and was recorded, false otherwise.
     */
    suspend fun canGenerateAndIncrement(context: Context): Boolean {
        val deviceId = getDeviceId(context)
        val todayStr = getCurrentDateString()
        val deviceDocRef = db.collection(COLLECTION_DEVICES).document(deviceId)

        return try {
            // Firestore transaction to ensure atomic read-modify-write
            db.runTransaction { transaction ->
                val snapshot = transaction.get(deviceDocRef)

                if (!snapshot.exists()) {
                    // First time user: create the document
                    Log.i(TAG, "First time generation for device $deviceId. Creating record.")
                    val newDeviceData = hashMapOf(
                        FIELD_GENERATIONS_COUNT to 1,
                        FIELD_LAST_GENERATION_DATE to todayStr
                    )
                    transaction.set(deviceDocRef, newDeviceData)
                    return@runTransaction true // Allow generation
                }

                val lastGenerationDate = snapshot.getString(FIELD_LAST_GENERATION_DATE)
                val generationsToday = snapshot.getLong(FIELD_GENERATIONS_COUNT) ?: 0

                if (lastGenerationDate != todayStr) {
                    // It's a new day: reset the count
                    Log.i(TAG, "New day for device $deviceId. Resetting count.")
                    transaction.update(deviceDocRef, mapOf(
                        FIELD_GENERATIONS_COUNT to 1,
                        FIELD_LAST_GENERATION_DATE to todayStr
                    ))
                    return@runTransaction true // Allow generation
                }

                if (generationsToday < MAX_GENERATIONS_PER_DAY) {
                    // Same day, but still has generations left
                    Log.i(TAG, "Incrementing generation count for device $deviceId.")
                    transaction.update(deviceDocRef, FIELD_GENERATIONS_COUNT, FieldValue.increment(1))
                    return@runTransaction true // Allow generation
                }

                // Daily limit reached
                Log.w(TAG, "Daily generation limit reached for device $deviceId.")
                return@runTransaction false // Block generation

            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase transaction", e)
            // Fail-safe: if Firebase fails, don't allow generation to prevent abuse.
            // You could change this to `true` to have a better user experience on network errors,
            // but it's less secure.
            return false
        }
    }

    /**
     * Fetches the remaining generations without incrementing the count.
     * Useful for displaying the remaining count in the UI.
     */
    suspend fun getRemainingGenerations(context: Context): Int {
        val deviceId = getDeviceId(context)
        val todayStr = getCurrentDateString()
        val deviceDocRef = db.collection(COLLECTION_DEVICES).document(deviceId)

        return try {
            val snapshot = deviceDocRef.get().await()
            if (!snapshot.exists()) {
                return MAX_GENERATIONS_PER_DAY
            }

            val lastGenerationDate = snapshot.getString(FIELD_LAST_GENERATION_DATE)
            if (lastGenerationDate != todayStr) {
                return MAX_GENERATIONS_PER_DAY
            }

            val generationsToday = snapshot.getLong(FIELD_GENERATIONS_COUNT) ?: 0
            (MAX_GENERATIONS_PER_DAY - generationsToday).toInt().coerceAtLeast(0)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remaining generations", e)
            0 // Return 0 on error
        }
    }
}