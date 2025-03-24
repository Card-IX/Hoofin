package com.example.gettahoofin

/**
 * Utility class for managing UI click events
 */
object ClickUtils {
    /**
     * Provides a debounced click handler to prevent rapid multiple clicks
     *
     * @param debounceTime The time in milliseconds to wait before allowing another click
     * @param onClick The action to perform on click
     * @return A function that can be used as an onClick handler
     */
    fun debounceClick(
        debounceTime: Long = 300L,
        onClick: () -> Unit
    ): () -> Unit {
        var lastClickTime = 0L

        return {
            val now = System.currentTimeMillis()
            if (now - lastClickTime > debounceTime) {
                lastClickTime = now
                onClick()
            }
        }
    }
}