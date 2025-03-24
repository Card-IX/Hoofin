package com.example.gettahoofin

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.reflect.Type
import kotlin.coroutines.resume

/**
 * Utility class for asynchronously loading and parsing JSON data
 */
object JsonLoader {
    private val gson = Gson()

    /**
     * Load and parse a JSON file asynchronously with explicit type
     *
     * @param context The application context
     * @param fileName The name of the asset file to load
     * @param type The Type to parse the JSON as
     * @return The parsed object or null if loading failed
     */
    suspend fun <T> loadFromAsset(context: Context, fileName: String, type: Type): T? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = readAssetFile(context, fileName)
                if (jsonString != null) {
                    gson.fromJson<T>(jsonString, type)
                } else {
                    null
                }
            } catch (e: Exception) {
                AppLogger.e("JsonLoader", "Error loading JSON from asset: $fileName", e)
                null
            }
        }
    }

    /**
     * Convenience method for loading a Program
     */
    suspend fun loadProgram(context: Context, fileName: String): Program? {
        return loadFromAsset(context, fileName, Program::class.java)
    }

    /**
     * Asynchronously read a file from assets
     */
    private suspend fun readAssetFile(context: Context, fileName: String): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputStream = context.assets.open(fileName)
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                val jsonString = String(buffer, Charsets.UTF_8)
                continuation.resume(jsonString)
            } catch (e: IOException) {
                AppLogger.e("JsonLoader", "Error reading asset file: $fileName", e)
                continuation.resume(null)
            }
        }
    }
}