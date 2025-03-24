package com.example.gettahoofin

import android.content.Context
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream

/**
 * Data classes for pace definitions
 */
data class PaceDefinition(
    val type: String,
    val description: String,
    val perceivedEffort: String,
    val physicalSigns: String
)

data class PaceDefinitions(
    val paceDefinitions: List<PaceDefinition>
)

/**
 * Utilities for loading pace definitions
 */
object PaceDefinitionsLoader {
    /**
     * Load pace definitions from JSON asset file
     */
    fun loadPaceDefinitions(context: Context): List<PaceDefinition> {
        val jsonString = getJsonDataFromAsset(context, "pace_definitions.json")
        return if (jsonString != null) {
            val gson = Gson()
            gson.fromJson(jsonString, PaceDefinitions::class.java).paceDefinitions
        } else {
            emptyList()
        }
    }

    /**
     * Helper function to load JSON data from assets
     */
    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        val jsonString: String
        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            jsonString = String(buffer, Charsets.UTF_8)
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }
}