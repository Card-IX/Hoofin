package com.example.gettahoofin

import java.io.Closeable

/**
 * Manages lifecycle of app resources to ensure proper cleanup
 */
class ResourceManager {
    // List of resources to be cleaned up
    private val resources = mutableListOf<Closeable>()

    /**
     * Register a resource for automatic cleanup
     */
    fun register(resource: Closeable) {
        synchronized(resources) {
            resources.add(resource)
        }
    }

    /**
     * Unregister a resource from automatic cleanup
     * This method might be useful in the future for resources with different lifecycles
     */
    @Suppress("unused")
    fun unregister(resource: Closeable) {
        synchronized(resources) {
            resources.remove(resource)
        }
    }

    /**
     * Clean up all registered resources
     */
    fun cleanup() {
        synchronized(resources) {
            resources.forEach {
                try {
                    it.close()
                    AppLogger.d("ResourceManager", "Successfully closed resource: $it")
                } catch (e: Exception) {
                    AppLogger.e("ResourceManager", "Error closing resource", e)
                }
            }
            resources.clear()
        }
    }
}